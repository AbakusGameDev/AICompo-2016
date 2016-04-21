package com.aicompo.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Stack;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// This AI selects a random point on the map, moves towards it
// for 1 second before choosing a new point while shooting.
public class DemoAIPlayer implements Runnable {
	private static final String PLAYER_NAME = "BITSAUCE";
	private static String hostIp = "127.0.0.1";
	private static String hostPort = "45556";

	private enum Mode {
		AIM,
		MANOUVER,
		JENKINS
	}
	
	private enum Movement {
		FORWARDS,
		BACKWARDS,
		STOPPED
	}

	private Map map;
	private AStar astar;
	private int playerID;
	private HashMap<Integer, Player> playerMap;
	private HashMap<Integer, Bullet> bulletMap;
	private Vector2 target;
	private Stack<AStar.Node> path;
	private long prevPathTime;
	private long gameStartTime;
	private long randomPhraseTime;
	private Mode mode;
	private Movement movement;
	
	static private String[] randomPhrases = {
		"GET REKT",
		"1337",
		"HF GL",
		"EZ GAME",
		"@_@",
		"How Can AIs Think If Intelligence Isn't Real",
		"I'M HERE TO KICK ASS AND CHEW BUBBLEGUM",
		"AND I'M ALL OUT OF BUBBLEGUM",
		":)",
		"ALL YOUR TANK ARE BELONG TO US"
	};

	public DemoAIPlayer() {
		// Initialize variables
		playerMap = new HashMap<Integer, Player>();
		bulletMap = new HashMap<Integer, Bullet>();
		target = new Vector2();
		path = new Stack<AStar.Node>();
		prevPathTime = 0;
		mode = Mode.MANOUVER;
		movement = Movement.STOPPED;
		randomPhraseTime = -1;
	}

	@Override
	public void run() {
		try {
			// Connect to host
			Socket socket = new Socket(hostIp, Integer.parseInt(hostPort));
			if(socket.isConnected()) {
				// Write player name to the socket
				new DataOutputStream(socket.getOutputStream()).writeBytes(PLAYER_NAME + "\n");

				// Get our player ID
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				playerID = Integer.parseInt(bufferedReader.readLine());

				// Do stuff while we're connected
				while(socket.isConnected()) {
					String line = bufferedReader.readLine();
					if(line.isEmpty()) {
						// Input: Empty string
						// Here is a complete list of available moves:
						// TURN_LEFT - Turn your tank left
						// TURN_RIGHT - Turn your tank right
						// STOP_TURN - Stop your tank turning
						// MOVE_FORWARDS - Move your tank forwards
						// MOVE_BACKWARDS - Move your tank backwards
						// STOP_MOVE - Stop your tank moving
						// SHOOT - Shoot a bullet (has a 1 second cooldown)
						// NAME - Changes your name

						// YOUR AI CODE HERE

						DataOutputStream out = new DataOutputStream(socket.getOutputStream());

						// Get my player
						if(playerMap.containsKey(playerID)) {
							Player myPlayer = playerMap.get(playerID);

							char[][] debugMap = new char[18][18];
							for(int y = 0; y < 18; ++y) {
								for(int x = 0; x < 18; ++x) {
									debugMap[x][y] = map.isOccupied(x, y) ? '#' : ' ';
								}
							}

							if(gameStartTime < 0) {
								gameStartTime = System.nanoTime();
								randomPhrase(out);
							}
							
							if(randomPhraseTime > 0 && System.nanoTime() - randomPhraseTime >= 3000000000L) {
								out.writeBytes("NAME BITSAUCE\n");
								randomPhraseTime = -1;
							}
							
							if(System.nanoTime() - gameStartTime >= 170000000000L) {
								out.writeBytes("NAME LEEROY JENKINS\n");
								mode = Mode.JENKINS;
							}

							if(Math.abs(myPlayer.getPosition().x - target.x) <= 10.0f && Math.abs(myPlayer.getPosition().y - target.y) <= 10.0f) {
								path.pop();
							}

							Player closestPlayer = null;
							{
								float minDist = Float.MAX_VALUE;
								for(Player player : playerMap.values()) {
									if(player == myPlayer) continue;
									float dist = new Vector2(myPlayer.getPosition()).sub(player.getPosition()).len();
									if(dist < minDist) {
										minDist = dist;
										closestPlayer = player;
									}
								}
							}
							
							boolean inLOS = !new RayCast((int) (myPlayer.getPosition().x / Map.TILE_SIZEF), (int) (myPlayer.getPosition().y / Map.TILE_SIZEF),
									(int) (closestPlayer.getPosition().x / Map.TILE_SIZEF), (int) (closestPlayer.getPosition().y / Map.TILE_SIZEF),
									new RayCast.PlotTest() {
										@Override
										public boolean test(int x, int y) {
											return map.isOccupied(x, y);
										}
									}).hasHit();

							if(inLOS && mode != Mode.JENKINS) {
								mode = Mode.AIM;
								target = closestPlayer.getPosition();
							}
							else {
								if(path.isEmpty() || System.nanoTime() - prevPathTime > 1000000000) {
									if(closestPlayer != null) {
										path = astar.getPathToTarget(
												myPlayer.getPosition().x / Map.TILE_SIZEF, myPlayer.getPosition().y / Map.TILE_SIZEF,
												closestPlayer.getPosition().x / Map.TILE_SIZEF, closestPlayer.getPosition().y / Map.TILE_SIZEF
												);
										target.set(path.peek().x * Map.TILE_SIZEF, path.peek().y * Map.TILE_SIZEF);
										prevPathTime = System.nanoTime();
									}
								}
								else {
									target.set(path.peek().x * Map.TILE_SIZEF, path.peek().y * Map.TILE_SIZEF);
								}
								mode = Mode.MANOUVER;
							}

							if(mode != Mode.JENKINS) {
								// Bullet dodging
								Bullet bulletToDodge = null;
								{
									float movementDir = movement == Movement.STOPPED ? 0.0f : 1.0f;
									Vector2 playerPos = new Vector2(myPlayer.getPosition()).sub(new Vector2(MathUtils.cosDeg(myPlayer.getAngle()), MathUtils.sinDeg(myPlayer.getAngle())).scl(120 * movementDir));
									Vector2 playerDestPos = new Vector2(myPlayer.getPosition()).add(new Vector2(MathUtils.cosDeg(myPlayer.getAngle()), MathUtils.sinDeg(myPlayer.getAngle())).scl(120 * movementDir));
	
									ArrayList<RayCast.Point> points = new RayCast(
											(int) (playerPos.x / Map.TILE_SIZEF), (int) (playerPos.y / Map.TILE_SIZEF),
											(int) (playerDestPos.x / Map.TILE_SIZEF), (int) (playerDestPos.y / Map.TILE_SIZEF),
											new RayCast.PlotTest() {
												@Override
												public boolean test(int x, int y) {
													return x < 0 || x >= 18 || y < 0 || y >= 18 || map.isOccupied(x, y);
												}
											}).points;
	
									for(RayCast.Point p : points) {
										debugMap[p.x][p.y] = '-';
									}
	
									float minDist = Float.MAX_VALUE;
									for(Bullet bullet : bulletMap.values()) {
										if(bullet.getOwnerID() == myPlayer.getID()) {
											continue;
										}
	
										Vector2 bulletPos = new Vector2(bullet.getPosition());
										Vector2 bulletDestPos = new Vector2(bullet.getPosition()).add(new Vector2(MathUtils.cosDeg(bullet.getAngle()), MathUtils.sinDeg(bullet.getAngle())).scl(400));
	
										RayCast r = new RayCast(
												(int) (bulletPos.x / Map.TILE_SIZEF), (int) (bulletPos.y / Map.TILE_SIZEF),
												(int) (bulletDestPos.x / Map.TILE_SIZEF), (int) (bulletDestPos.y / Map.TILE_SIZEF),
												new RayCast.PlotTest() {
													@Override
													public boolean test(int x, int y) {
														for(RayCast.Point point : points) {
															if(point.x == x && point.y == y) {
																return true;
															}
														}
														return map.isOccupied(x, y);
													}
												});
	
										for(RayCast.Point p : r.points) {
											if(p.x >= 0 && p.x < 18 && p.y >= 0 && p.y < 18) {
												debugMap[p.x][p.y] = 'x';
											}
										}
	
										if(r.hasHit()) {
											float dist = new Vector2(myPlayer.getPosition()).sub(bullet.getPosition()).len();
											if(dist < minDist) {
												minDist = dist;
												bulletToDodge = bullet;
											}
										}
									}
	
									if(bulletToDodge != null) {
										Vector2 bulletVector = new Vector2(MathUtils.cosDeg(bulletToDodge.getAngle()), MathUtils.sinDeg(bulletToDodge.getAngle()));
										float angle = new Vector2(myPlayer.getPosition()).sub(bulletToDodge.getPosition()).angle(bulletVector);
										target = new Vector2(bulletVector).rotate(angle < 0.0 ? 45 : -45).nor().scl(120).add(myPlayer.getPosition());
										mode = Mode.MANOUVER;
									}
								}
							}

							Vector2 targetVector = new Vector2(target).sub(myPlayer.getPosition());
							Vector2 targetVectorNormalized = new Vector2(targetVector).nor();
							float cross = targetVectorNormalized.crs(new Vector2(MathUtils.cosDeg(myPlayer.getAngle()), MathUtils.sinDeg(myPlayer.getAngle())));
							float dot = targetVectorNormalized.dot(new Vector2(MathUtils.cosDeg(myPlayer.getAngle()), MathUtils.sinDeg(myPlayer.getAngle())));

							if(mode == Mode.MANOUVER) {
								if(dot > 0.1f) {
									if(cross > 0.1f) {
										out.writeBytes("TURN_LEFT\n");
									}
									else if(cross < -0.1f) {
										out.writeBytes("TURN_RIGHT\n");
									}
									else {
										out.writeBytes("STOP_TURN\n");
									}
								}
								else if(dot < -0.1f) {
									if(cross > 0.1f) {
										out.writeBytes("TURN_RIGHT\n");
									}
									else if(cross < -0.1f) {
										out.writeBytes("TURN_LEFT\n");
									}
									else {
										out.writeBytes("STOP_TURN\n");
									}
								}
								else {
									out.writeBytes("STOP_TURN\n");
								}

								if(targetVector.len() <= 5.0f) {
									out.writeBytes("STOP_MOVE\n");
								}
								else if(dot < -0.3f) {
									out.writeBytes("MOVE_BACKWARDS\n");
									movement = Movement.BACKWARDS;
								}
								else if(dot > 0.3f) {
									out.writeBytes("MOVE_FORWARDS\n");
									movement = Movement.FORWARDS;
								}
								else {
									out.writeBytes("STOP_MOVE\n");
									movement = Movement.STOPPED;
								}
							}
							else if(mode == Mode.AIM) {
								if(cross > 0.1f) {
									out.writeBytes("TURN_LEFT\n");
								}
								else if(cross < -0.1f) {
									out.writeBytes("TURN_RIGHT\n");
								}
								else {
									out.writeBytes("STOP_TURN\n");
								}

								out.writeBytes("SHOOT\n");
								out.writeBytes("STOP_MOVE\n");
								movement = Movement.STOPPED;
							}
							else if(mode == Mode.JENKINS) {
								if(cross > 0.1f) {
									out.writeBytes("TURN_LEFT\n");
								}
								else if(cross < -0.1f) {
									out.writeBytes("TURN_RIGHT\n");
								}
								else {
									out.writeBytes("STOP_TURN\n");
								}
								
								out.writeBytes("SHOOT\n");
								out.writeBytes("MOVE_FORWARDS\n");
								movement = Movement.FORWARDS;
							}

							// DEBUG CODE
							/*for(Bullet bullet : bulletMap.values()) {
								debugMap[(int) (bullet.getPosition().x/Map.TILE_SIZEF)][(int) (bullet.getPosition().y/Map.TILE_SIZEF)] = '*';
							}

							debugMap[(int) (myPlayer.getPosition().x/Map.TILE_SIZEF)][(int) (myPlayer.getPosition().y/Map.TILE_SIZEF)] = 'P';
							//debugMap[(int) (target.x/Map.TILE_SIZEF)][(int) (target.y/Map.TILE_SIZEF)] = 'T';

							String debugStr = "";
							for(int y = 0; y < 18; ++y) {
								for(int x = 0; x < 18; ++x) {
									debugStr += debugMap[x][y];
								}
								debugStr += "\n";
							}
							System.out.println(debugStr);*/
						}
						else {
							out.writeBytes("NAME RIPSAUCE\n");
						}

						// Ends your turn. DON'T FORGET IT!
						out.writeBytes("\n");

						// This gives a steady frame rate
						try {
							Thread.sleep(16);
						} catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
					else if(line.equals("PLAYERS_BEGIN")) {
						// Input: PLAYERS_BEGIN
						// This is where we parse the players.
						// The data is send in the given format:
						// id;name;x;y;angle
						// until PLAYERS_END is received.

						// Clear players
						playerMap.clear();

						// Parse players
						String playerDataString;
						while(!(playerDataString = bufferedReader.readLine()).equals("PLAYERS_END")) {
							// Split player data
							String[] playerData = playerDataString.split(";");

							// Add player to collection
							playerMap.put(Integer.parseInt(playerData[0]), new Player(Integer.parseInt(playerData[0]), playerData[1], new Vector2(Float.parseFloat(playerData[2]), Float.parseFloat(playerData[3])), Float.parseFloat(playerData[4])));
						}
					}
					else if(line.equals("BULLETS_BEGIN")) {
						// Input: BULLETS_BEGIN
						// This is where we parse the bullets.
						// The data is send in the given format:
						// id;ownerid;x;y;angle
						// until BULLETS_END is received.

						// Clear bullet data
						bulletMap.clear();

						// Parse bullets
						String bulletDataString;
						while(!(bulletDataString = bufferedReader.readLine()).equals("BULLETS_END")) {
							// Split bullet data
							String[] bulletData = bulletDataString.split(";");

							// Add bullet to bullet list
							bulletMap.put(Integer.parseInt(bulletData[0]), new Bullet(Integer.parseInt(bulletData[0]), Integer.parseInt(bulletData[1]), new Vector2(Float.parseFloat(bulletData[2]), Float.parseFloat(bulletData[3])), Float.parseFloat(bulletData[4])));
						}
					}
					else if(line.equals("MAP_BEGIN")) {
						// Input: MAP_BEGIN
						// This is where we parse the map.
						// First the size of the map is send in the given format:
						// width;height
						// Then the tiles of the map are sent in the given format:
						// x;y;tile
						// until MAP_END is received.

						// Initialize map
						String[] mapSizeData = bufferedReader.readLine().split(";");
						int[][] tiles = new int[Integer.parseInt(mapSizeData[0])][Integer.parseInt(mapSizeData[1])];

						// Parse map
						String mapDataString;
						while(!(mapDataString = bufferedReader.readLine()).equals("MAP_END")) {
							String[] tileData = mapDataString.split(";");
							tiles[Integer.parseInt(tileData[0])][Integer.parseInt(tileData[1])] = Integer.parseInt(tileData[2]);
						}

						map = new Map(tiles);
						astar = new AStar(map);
						gameStartTime = -1;
						randomPhraseTime = -1;

						/*String s = "";
						for(int y = 0; y < map.getHeight(); ++y) {
							for(int x = 0; x < map.getWidth(); ++x) {
								s += map.getTile(x, y);
							}
						}
						System.out.println(s);*/
					}
					else if(line.equals("RESTART")) {
						while(!path.isEmpty()) path.pop();
					}
				}
			}
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void randomPhrase(DataOutputStream out) throws IOException {
		out.writeBytes("NAME " + randomPhrases[new Random().nextInt(randomPhrases.length)] + "\n");
		randomPhraseTime = System.nanoTime();
	}

	// This is useful when connecting to a remote host
	public static void main(String[] args) {
		try {
			// Get IP and port
			BufferedReader consoleInputReader = new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Host IP:");
			hostIp = consoleInputReader.readLine();
			System.out.print("Port:");
			hostPort = consoleInputReader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		new DemoAIPlayer().run();
	}
}
