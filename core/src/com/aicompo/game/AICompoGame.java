package com.aicompo.game;

import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Vector2;

public class AICompoGame extends ApplicationAdapter {
	public enum State {
		WAITING_FOR_PLAYERS,
		GAME_STARTING,
		GAME_RUNNING,
		GAME_DONE
	}
	
	private static SpriteBatch batch;
	private static OrthographicCamera camera;

	private static ServerSocketChannel serverSocketChannel;

	private static State state;
	private static Sprite panelSprite;
	private static Sprite floorSprite;
	private static Sprite wallSprite;

	private static float gameTimer;
	private static boolean suddenDeath;

	private static BitmapFont font;
	private static BitmapFont fontPanel;
	private static BitmapFont fontPlayer;

	private static ArrayList<Player> players;
	private static ArrayList<Player> playersAlive;
	private static ArrayList<Bullet> bullets;

	private static Random random;
	private static int mapIndex;
	private static FileHandle[] mapFiles;
	private ArrayList<Point> tilesToRemove;

	public static final float GAME_TIME = 60.0f; // Sec
	public static final int SERVER_PORT = 45556;
	public static final String TEXT_HOTKEYS = "Press <1> to add AI player\nPress <2> to add controlled player\n";
	public static final String TEXT_NEED_PLAYERS = "Need at least two players to start\n\n" + TEXT_HOTKEYS;
	public static final String TEXT_PRESS_TO_START = "Press <ENTER> to start the game\n\n" + TEXT_HOTKEYS;
	public static final String TEXT_STARTING_IN = "Starting in";
	public static final String TEXT_GAME_STARTED = "GO!";
	public static final String TEXT_TIE = "It's a tie!";
	public static final String TEXT_GAME_RESTART = "\n\n\nPress <ENTER> to restart\n";
	
	static public final String DEFAULT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F\u00A0\u00A1\u00A2\u00A3\u00A4\u00A5\u00A6\u00A7\u00A8\u00A9\u00AA\u00AB\u00AC\u00AD\u00AE\u00AF\u00B0\u00B1\u00B2\u00B3\u00B4\u00B5\u00B6\u00B7\u00B8\u00B9\u00BA\u00BB\u00BC\u00BD\u00BE\u00BF\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C6\u00C7\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF\u00D0\u00D1\u00D2\u00D3\u00D4\u00D5\u00D6\u00D7\u00D8\u00D9\u00DA\u00DB\u00DC\u00DD\u00DE\u00DF\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E6\u00E7\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F7\u00F8\u00F9\u00FA\u00FB\u00FC\u00FD\u00FE\u00FF";
	
	@SuppressWarnings("deprecation")
	@Override
	public void create() {
		batch = new SpriteBatch();
		random = new Random();
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.setToOrtho(true, camera.viewportWidth, camera.viewportHeight);
		players = new ArrayList<Player>();
		playersAlive = new ArrayList<Player>();
		bullets = new ArrayList<Bullet>();
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Days.ttf"));
		font = generator.generateFont(18, DEFAULT_CHARS, true);
		font.setColor(Color.WHITE);
		font.setUseIntegerPositions(true);
		fontPanel = generator.generateFont(15, DEFAULT_CHARS, true);
		fontPanel.setColor(Color.WHITE);
		fontPanel.setUseIntegerPositions(true);
		fontPlayer = generator.generateFont(12, DEFAULT_CHARS, true);
		fontPlayer.setColor(Color.WHITE);
		fontPlayer.setUseIntegerPositions(true);
		generator.dispose();
		gameTimer = 0.0f;
		state = State.WAITING_FOR_PLAYERS;
		panelSprite = new Sprite(new Texture("panel.png"));
		panelSprite.flip(false, true);
		panelSprite.setPosition(Map.WIDTH * Map.TILE_SIZE, 0.0f);
		panelSprite.setSize(280, 720);

		tilesToRemove = new ArrayList<Point>();
		mapIndex = 0;
		mapFiles = Gdx.files.internal("maps/").list();
		
		wallSprite = new Sprite(new Texture("wall.png"));
		wallSprite.flip(false, true);
		wallSprite.setSize(Map.TILE_SIZEF, Map.TILE_SIZEF);
		floorSprite = new Sprite(new Texture("floor.png"));
		floorSprite.flip(false, true);
		floorSprite.setSize(Map.TILE_SIZEF, Map.TILE_SIZEF);

		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(new InetSocketAddress(SERVER_PORT));
			serverSocketChannel.configureBlocking(false);
		} catch (IOException e) {
			System.err.println("Unable to listen to port " + SERVER_PORT);
			Gdx.app.exit();
		}
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		switch(state) {
		case WAITING_FOR_PLAYERS:
			SocketChannel sc;
			try {
				sc = serverSocketChannel.accept();
				if(sc != null) {
					String name = new BufferedReader(new InputStreamReader(sc.socket().getInputStream())).readLine();
					
					Player player = new Player(sc.socket(), name, bullets);
					players.add(player);
					new DataOutputStream(sc.socket().getOutputStream()).writeBytes(Integer.toString(player.getID()) + "\n");
					
					System.out.println("'" + name + "' (" + player.getID() + ") connected.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
				if(players.size() < 2) {
					System.err.println("Two or more players required to begin.");
				}
				else {
					startGame();
				}
			}
			else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
				new Thread(new AIMain()).start();
			}
			else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
				new Thread(new ControlledPlayer()).start();
			}
			
			break;
			
		case GAME_STARTING:
			gameTimer -= Gdx.graphics.getDeltaTime();
			if(gameTimer <= 0.0f) {
				state = State.GAME_RUNNING;
				gameTimer = GAME_TIME;
				for(Player player : players) {
					player.previousPacketTime = System.nanoTime();
					player.getThread().start();
				}
			}
			break;
			
		case GAME_RUNNING:
			// Check for restart
			if(Gdx.input.isKeyPressed(Input.Keys.R) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))) {
				state = State.GAME_DONE;
			}

			// Update players
			for(Player player : new ArrayList<Player>(playersAlive)) {
				player.update();
				if(player.getStatus() == Player.Status.DEAD) {
					playersAlive.remove(player);
					if(playersAlive.size() < 2) {
						state = State.GAME_DONE;
					}
				}
			}

			// Update bullets
			for(Bullet bullet : new ArrayList<Bullet>(bullets)) {
				bullet.update();
				if(bullet.isDestroyed()) {
					bullets.remove(bullet);
				}
			}

			// Update game time
			gameTimer -= Gdx.graphics.getDeltaTime();
			if(gameTimer <= 0.0f) { // Sudden death
				suddenDeath = true;
				gameTimer = 2.0f;
				if(tilesToRemove.size() > 0) {
					Point point = tilesToRemove.remove(random.nextInt(tilesToRemove.size()));
					Map.setTile(point.x, point.y, 0);
					for(Player player : playersAlive) {
						synchronized (player.removedTiles) {
							player.removedTiles.add(point);
						}
					}
				}
			}
			break;
			
		case GAME_DONE:
			if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
				if(players.size() < 2) {
					System.err.println("Two or more players required to begin.");
				}
				else {
					startGame();
				}
			}
			break;
			
		default:
			break;
		}

		// com.aicompo.game.Map switching
		if(state == State.WAITING_FOR_PLAYERS || state == State.GAME_DONE) {
			if(Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
				if(mapIndex == 0) {
					mapIndex = mapFiles.length - 1;
				}
				else {
					mapIndex--;
				}
			}
			else if(Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
				if(mapIndex == mapFiles.length - 1) {
					mapIndex = 0;
				}
				else {
					mapIndex++;
				}
			}
		}

		// Disconnect players that don't respond
		for(Player player : new ArrayList<Player>(players)) {
			if(player.getThread() != null && player.getThread().isAlive()) {
				if(System.nanoTime() - player.previousPacketTime > 3000000000L) {
					synchronized(AICompoGame.class) {
						try {
							player.getSocket().close();
							player.getThread().interrupt();
						} catch (IOException e) {
							e.printStackTrace();
						}
						players.remove(player);
						System.out.println("'" + player.getName() + "' was disconnected (timeout).");
					}
				}
			}
		}

		// Setup sprite batch
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		
		// Draw map
		for(int y = 0; y < Map.HEIGHT; ++y) {
			for(int x = 0; x < Map.WIDTH; ++x) {
				if(Map.isTile(x, y)) {
					wallSprite.setPosition(x * Map.TILE_SIZE, y * Map.TILE_SIZE);
					wallSprite.draw(batch);
				}
				else {
					floorSprite.setPosition(x * Map.TILE_SIZE, y * Map.TILE_SIZE);
					floorSprite.draw(batch);
				}
			}
		}

		// Draw bullets
		for(Bullet bullet : new ArrayList<Bullet>(bullets)) {
			bullet.draw(batch);
		}

		// Draw players
		for(Player player : new ArrayList<Player>(playersAlive)) {
			player.draw(batch, fontPlayer);
		}
		
		panelSprite.draw(batch);
		
		// Show current player list
		font.draw(batch, "Players:", 740, 10);
		{
			int i = 0;
			for(Player player : players) {
				fontPanel.draw(batch, player.getName() + " [" + player.getStatus() + "]", 740, 42 + 20 * i++);
			}
			
			if(state != State.WAITING_FOR_PLAYERS) {
				i++;
				fontPanel.draw(batch, "Players remaining: " + playersAlive.size(), 740, 42 + 20 * i++);
				fontPanel.draw(batch, "Time remaining: " + (state == State.GAME_RUNNING ? (suddenDeath ? "Sudden death" : (int) gameTimer + " seconds") : ((int) GAME_TIME)), 740, 42 + 20 * i++);
			}
		}
		
		// Set center text
		String centerText = "";
		if(state == State.WAITING_FOR_PLAYERS) {
			if(players.size() < 2) {
				centerText = TEXT_NEED_PLAYERS;
			}
			else {
				centerText = TEXT_PRESS_TO_START;
			}
		}
		else if(state == State.GAME_STARTING) {
			centerText = TEXT_STARTING_IN + "\n" + Integer.toString((int)Math.ceil(gameTimer));
		}
		else if(state == State.GAME_RUNNING && gameTimer >= GAME_TIME - 2.0f) {
			centerText = TEXT_GAME_STARTED;
		}
		else if(state == State.GAME_DONE) {
			if(playersAlive.size() == 1) {
				centerText = playersAlive.get(0).getName() + " Won!";
			}
			else if(playersAlive.size() == 0) {
				centerText = TEXT_TIE;
			}
			centerText += TEXT_GAME_RESTART;
		}
		
		if(state == State.WAITING_FOR_PLAYERS || state == State.GAME_DONE) {
			centerText += "\nCurrent map: " + mapFiles[mapIndex].nameWithoutExtension() + "\nPress <LEFT> or <RIGHT> to change map";
		}
		
		// Draw center text
		{
			int i = 0;
			for(String text : centerText.split("\n")) {
				font.draw(batch, text, (camera.viewportWidth - 280 - font.getBounds(text).width) / 2, (camera.viewportHeight - font.getBounds(text).height) / 2 - 200 + 20 * i++);
			}
		}
		
		batch.end();
	}

	private void startGame() {
		// Clear stuff
		synchronized(AICompoGame.class) {
			playersAlive.clear();
			bullets.clear();
			tilesToRemove.clear();
		}
		
		// Load map
		{
			BufferedReader mapReader = new BufferedReader(mapFiles[mapIndex].reader());
			String line = null;
			int y = 0;
			try {
				while((line = mapReader.readLine()) != null) {
					if(line.length() != Map.WIDTH) {
						System.err.println("Line " + y + " in map file '" + mapFiles[mapIndex].name() + "' has an incorrect amount of characters");
						break;
					}
					
					for(int x = 0; x < Map.WIDTH; ++x) {
						char c = line.charAt(x);
						if(c == '#') {
							Map.setTile(x, y, 1);
						}
						else if(c == '.') {
							Map.setTile(x, y, 0);
						}
						else if(c == '?') {
							Map.setTile(x, y, random.nextFloat() < 0.1f ? 1 : 0);
						}

						if(y != 0 && x != 0 && y != Map.HEIGHT - 1 && x != Map.WIDTH - 1 && Map.getTile(x, y) == 1) {
							tilesToRemove.add(new Point(x, y));
						}
					}
					
					y++;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			if(y != Map.HEIGHT) {
				System.err.println("Map file '" + mapFiles[mapIndex].name() + "' has an incorrect amount of lines");
			}
		}

		// Spawn players
		for(final Player player : players) {
			Point spawn = getSpawnPoint();
			float angle = Math.round((new Vector2(Map.WIDTH / 2.0f - spawn.x, Map.HEIGHT / 2.0f - spawn.y).scl(Map.TILE_SIZEF)).angle() / 90.0f) * 90.0f;
			player.spawn(spawn.x, spawn.y, angle);
			playersAlive.add(player);

			// Create connection thread
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Send newline
						DataOutputStream out = new DataOutputStream(player.getSocket().getOutputStream());
						out.writeBytes("\n");
						out.flush();
						
						// Listen for input
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(player.getSocket().getInputStream()));
						String line;
						while((line = bufferedReader.readLine()) != null) {
							if(!line.isEmpty()) {
								int id = Integer.parseInt(line);
								if(id == AISuperClass.TURN_LEFT) {
									player.setTurnScale(-1.0f);
								}
								else if(id == AISuperClass.TURN_RIGHT) {
									player.setTurnScale(1.0f);
								}
								else  if(id == AISuperClass.STOP_TURN) {
									player.setTurnScale(0.0f);
								}
								else if(id == AISuperClass.MOVE_FORWARDS) {
									player.setMovementScale(1.0f);
								}
								else if(id == AISuperClass.MOVE_BACKWARDS) {
									player.setMovementScale(-1.0f);
								}
								else if(id == AISuperClass.STOP_MOVE) {
									player.setMovementScale(0.0f);
								}
								else if(id == AISuperClass.SHOOT) {
									player.shoot();
								}
								else if(id == AISuperClass.NAME) {
									player.setName(bufferedReader.readLine());
								}
								else {
									System.err.println("'" + player.getName() + "' tried to perform unknown command '" + id + "'.");
								}
							}
							else {
								// If the game has ended
								if(state == State.GAME_DONE) {
									out.writeBytes("END\n");
									out.flush();
									return;
								}

								player.previousPacketTime = System.nanoTime();

								// Send game state
								String gameStatePacket = "";
								gameStatePacket += "PLAYERS_BEGIN\n";
								for(Player player : new ArrayList<Player>(players)) {
									gameStatePacket += player.getID() + ";" + player.getName() + ";" + player.getCenter().x + ";" + player.getCenter().y + ";" + player.getAngle() + ";" + (player.getStatus() == Player.Status.ALIVE) + "\n";
								}
								gameStatePacket += "PLAYERS_END\n";
								gameStatePacket += "BULLETS_BEGIN\n";
								for(Bullet bullet : new ArrayList<Bullet>(bullets)) {
									gameStatePacket += bullet.getID() + ";" + bullet.getOwner().getID() + ";" + bullet.getCenter().x + ";" + bullet.getCenter().y + ";" + bullet.getAngle() + "\n";
								}
								gameStatePacket += "BULLETS_END\n";

								if(player.removedTiles.size() > 0) {
									gameStatePacket += "MAP_MODIFIED_BEGIN\n";
									synchronized (player.removedTiles) {
										for (Point tile : player.removedTiles) {
											gameStatePacket += Integer.toString(tile.x) + ";" + Integer.toString(tile.y) + ";" + Integer.toString(Map.getTile(tile.x, tile.y)) + "\n";
										}
										player.removedTiles.clear();
									}
									gameStatePacket += "MAP_MODIFIED_END\n";
								}
								gameStatePacket += "\n";
								out.writeBytes(gameStatePacket);
								out.flush();
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			player.setThread(thread);
		}
		
		try {
			// Send game state
			String gameStatePacket = "INIT\n";
			gameStatePacket += "PLAYERS_BEGIN\n";
			for(Player player : players) {
				gameStatePacket += player.getID() + ";" + player.getName() + ";" + player.getCenter().x + ";" + player.getCenter().y + ";" + player.getAngle() + ";" + (player.getStatus() == Player.Status.ALIVE) + "\n";
			}
			gameStatePacket += "PLAYERS_END\n";
			gameStatePacket += "MAP_BEGIN\n";
			for(int y = 0; y < Map.HEIGHT; ++y) {
				for(int x = 0; x < Map.WIDTH; ++x) {
					gameStatePacket += Integer.toString(x) + ";" + Integer.toString(y) + ";" + Integer.toString(Map.getTile(x, y)) + "\n";
				}
			}
			gameStatePacket += "MAP_END\n";
			gameStatePacket += "START\n";

			for(Player player : players) {
				new DataOutputStream(player.getSocket().getOutputStream()).writeBytes(gameStatePacket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		state = State.GAME_STARTING;
		gameTimer = 3.0f;
		suddenDeath = false;
	}

	private Point getSpawnPoint() {
		int i = 1;
		do {
			ArrayList<Point> spawnPoints = new ArrayList<Point>();
			for(int x = i; x < Map.WIDTH - i; ++x) {
				for(int y = i; y < Map.HEIGHT - i; ++y) {
					if(Map.isTile(x, y)) continue;
					if(x == i || y == i || x == Map.WIDTH - i - 1 || y == Map.HEIGHT - i - 1) {
						spawnPoints.add(new Point(x, y));
					}
				}
			}
			
			while(!spawnPoints.isEmpty()) {
				int idx = random.nextInt(spawnPoints.size());
				Point spawnPoint = spawnPoints.get(idx);
				spawnPoints.remove(idx);
				
				boolean tooClose = false;
				for(Player player : playersAlive) {
					int px = (int) (player.getCenter().x / Map.TILE_SIZEF), py = (int) (player.getCenter().y / Map.TILE_SIZEF);
					if(Math.abs(px - spawnPoint.x) <= 5 - i && Math.abs(py - spawnPoint.y) <= 5 - i) {
						tooClose = true;
						break;
					}
				}
				
				if(tooClose) {
					continue;
				}
				
				return spawnPoint;
			}
		} while(++i < Map.WIDTH);
		
		return null;
	}

	@Override
	public void resize(int width, int height) {
	}
	
	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}
}
