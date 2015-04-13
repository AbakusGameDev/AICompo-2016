package com.aicompo.example;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

// This AI selects a random point on the map, moves towards it
// for 1 second before choosing a new point while shooting.
public class DemoAIPlayer implements Runnable {
	private static final String PLAYER_NAME = "DEMO AI";
	private static String hostIp = "127.0.0.1";
	private static String hostPort = "45556";
	
	private int playerID;
	private HashMap<Integer, Player> playerMap;
	private HashMap<Integer, Bullet> bulletMap;
	private Random random;
	private Vector2 target;
	private long prevTargetTime;
	
	public DemoAIPlayer() {
		// Initialize variables
		playerMap = new HashMap<Integer, Player>();
		bulletMap = new HashMap<Integer, Bullet>();
		random = new Random();
		target = null;
		prevTargetTime = 0;
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
						if(playerMap.containsKey(playerID))
						{
							Player player = playerMap.get(playerID);
							
							// Set a random target every second
							if(target == null || (System.nanoTime() - prevTargetTime) > 1000000000) {
								target = new Vector2(random.nextFloat() * Map.getWidth() * Map.TILE_SIZEF, random.nextFloat() * Map.getHeight() * Map.TILE_SIZEF);
								prevTargetTime = System.nanoTime();
							}
							
							float cross = new Vector2(target).sub(player.getPosition()).crs(new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle())));
							
							// Rotate towards it
							if(cross < 0.0f) {
								out.writeBytes("TURN_RIGHT\n");
							}
							else if(cross > 0.0f) {
								out.writeBytes("TURN_LEFT\n");
							}
							
							// Move forwards and shoot
							out.writeBytes("SHOOT\n");
							out.writeBytes("MOVE_FORWARDS\n");
						}
							
						// Ends your turn. DON'T FORGET IT!
						out.writeBytes("\n"); 
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
						Map.initialize(Integer.parseInt(mapSizeData[0]), Integer.parseInt(mapSizeData[1]));
	
						// Parse map
						String mapDataString;
						while(!(mapDataString = bufferedReader.readLine()).equals("MAP_END")) {
							String[] tileData = mapDataString.split(";");
							Map.setTile(Integer.parseInt(tileData[0]), Integer.parseInt(tileData[1]), Integer.parseInt(tileData[2]));
						}
					}

					// This gives a steady frame rate
					try {
						Thread.sleep(16);
					} catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
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
