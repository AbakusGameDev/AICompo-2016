package com.aicompo.game;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Random;

import com.aicompo.example.DemoAIPlayer;
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
	private static Sprite pannelSprite;
	private static Sprite floorSprite;
	private static Sprite wallSprite;
	private static float startCountDown;
	private static float gameRunningTime;
	private static BitmapFont arial15;
	private static BitmapFont arial11;
	private static ArrayList<Player> players;
	private static ArrayList<PlayerDescriptor> playerDescriptors;
	private static ArrayList<Entity> entities;
	private static ArrayList<Bullet> bullets;
	private static Random random;
	private static int mapIndex;
	private static FileHandle[] mapFiles;
	
	public static final int SERVER_PORT = 45556;
	public static final String TEXT_HOTKEYS = "Press CTRL + 1 to add example AI player\nPress CTRL + 2 to add controlled player\n";
	public static final String TEXT_NEED_PLAYERS = "Need at lest two players to start\n\n" + TEXT_HOTKEYS;
	public static final String TEXT_PRESS_TO_START = "Press CTRL + ENTER to start the game\n\n" + TEXT_HOTKEYS;
	public static final String TEXT_STARTING_IN = "Starting in";
	public static final String TEXT_GAME_STARTED = "GO!";
	public static final String TEXT_TIE = "Its a Tie!";
	
	static public final String DEFAULT_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F\u00A0\u00A1\u00A2\u00A3\u00A4\u00A5\u00A6\u00A7\u00A8\u00A9\u00AA\u00AB\u00AC\u00AD\u00AE\u00AF\u00B0\u00B1\u00B2\u00B3\u00B4\u00B5\u00B6\u00B7\u00B8\u00B9\u00BA\u00BB\u00BC\u00BD\u00BE\u00BF\u00C0\u00C1\u00C2\u00C3\u00C4\u00C5\u00C6\u00C7\u00C8\u00C9\u00CA\u00CB\u00CC\u00CD\u00CE\u00CF\u00D0\u00D1\u00D2\u00D3\u00D4\u00D5\u00D6\u00D7\u00D8\u00D9\u00DA\u00DB\u00DC\u00DD\u00DE\u00DF\u00E0\u00E1\u00E2\u00E3\u00E4\u00E5\u00E6\u00E7\u00E8\u00E9\u00EA\u00EB\u00EC\u00ED\u00EE\u00EF\u00F0\u00F1\u00F2\u00F3\u00F4\u00F5\u00F6\u00F7\u00F8\u00F9\u00FA\u00FB\u00FC\u00FD\u00FE\u00FF";

	static void addEntity(Entity e) {
		entities.add(e);
	}
	
	public static void removeEntity(Entity e) {
		entities.remove(e);
	}
	
	public static void addBullet(Bullet b) {
		synchronized(AICompoGame.class) {
			bullets.add(b);
		}
	}
	
	public static void removeBullet(Bullet b) {
		synchronized(AICompoGame.class) {
			bullets.remove(b);
		}
	}
	
	public static void addPlayer(Player p) {
		synchronized(AICompoGame.class) {
			players.add(p);
		}
	}
	
	public static void removePlayer(Player p) {
		synchronized(AICompoGame.class) {
			players.remove(p);
			if(players.size() <= 1) {
				state = State.GAME_DONE;
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void create() {
		batch = new SpriteBatch();
		random = new Random();
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.setToOrtho(true, camera.viewportWidth, camera.viewportHeight);
		entities = new ArrayList<Entity>();
		players = new ArrayList<Player>();
		playerDescriptors = new ArrayList<PlayerDescriptor>();
		bullets = new ArrayList<Bullet>();
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("arial.ttf"));
		arial15 = generator.generateFont(15, DEFAULT_CHARS, true);
		arial15.setColor(Color.WHITE);
		arial15.setUseIntegerPositions(true);
		arial11 = generator.generateFont(11, DEFAULT_CHARS, true);
		arial11.setColor(Color.WHITE);
		arial11.setUseIntegerPositions(true);
		generator.dispose();
		startCountDown = 0.0f;
		gameRunningTime = 0.0f;
		state = State.WAITING_FOR_PLAYERS;
		pannelSprite = new Sprite(new Texture("pannel.png"));
		pannelSprite.flip(false, true);
		pannelSprite.setPosition(Map.WIDTH * Map.TILE_SIZE, 0.0f);
		pannelSprite.setSize(280, 720);
		mapIndex = 0;
		mapFiles = Gdx.files.internal("./bin/maps").list();
		
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
					
					PlayerDescriptor descriptor = new PlayerDescriptor(sc.socket(), name);
					playerDescriptors.add(descriptor);
					new DataOutputStream(sc.socket().getOutputStream()).writeBytes(Integer.toString(descriptor.getID()) + "\n");
					
					System.out.println("'" + name + "' (" + descriptor.getID() + ") connected.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
				if(playerDescriptors.size() < 2) {
					System.err.println("Two or more players required to begin.");
				}
				else {
					startGame();
				}
			}
			else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
				new Thread(new DemoAIPlayer()).start();
			}
			else if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
				new Thread(new ControlledPlayer()).start();
			}
			
			break;
			
		case GAME_STARTING:
			startCountDown -= Gdx.graphics.getDeltaTime();
			if(startCountDown <= 0.0f) {
				state = State.GAME_RUNNING;
				for(PlayerDescriptor descriptor : playerDescriptors) {
					descriptor.prevTickTime = System.nanoTime();
					descriptor.getThread().start();
				}
			}
			break;
			
		case GAME_RUNNING:
			if(players.size() >= 2) { 
				gameRunningTime += Gdx.graphics.getDeltaTime();
			}
			break;
			
		case GAME_DONE:
			if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
				if(playerDescriptors.size() < 2) {
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
		synchronized(AICompoGame.class) {
			for(PlayerDescriptor descriptor : new ArrayList<PlayerDescriptor>(playerDescriptors)) {
				if(descriptor.getThread() != null && descriptor.getThread().isAlive()) {
					if(System.nanoTime() - descriptor.prevTickTime > 3000000000L) {
						try {
							descriptor.getSocket().close();
							descriptor.getThread().interrupt();
						} catch (IOException e) {
							e.printStackTrace();
						}
						playerDescriptors.remove(descriptor);
						System.out.println("'" + descriptor.getName() + "' (" + descriptor.getID() + ") was disconnected.");
					}
				}
			}
		}

		if(players.size() >= 2) { 
			for(Entity entity : new ArrayList<Entity>(entities)) {
				entity.update();
			}
		}
		
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
		
		for(Entity entity : new ArrayList<Entity>(entities)) {
			entity.draw(batch);
		}
		
		pannelSprite.draw(batch);
		
		// Show current player list
		arial15.draw(batch, "Players:", 740, 10);
		{
			int i = 0;
			for(PlayerDescriptor desc : playerDescriptors) {
				arial15.draw(batch, desc.getName() + " [" + desc.getStatus() + "]", 740, 30 + 20 * i++);
			}
			
			if(state != State.WAITING_FOR_PLAYERS) {
				i++;
				arial15.draw(batch, "Remaining Players: " + players.size(), 740, 30 + 20 * i++);
				arial15.draw(batch, "Match Time: " + (int) gameRunningTime + " seconds", 740, 30 + 20 * i++);
			}
		}
		
		// Set center text
		String centerText = "";
		if(state == State.WAITING_FOR_PLAYERS) {
			if(playerDescriptors.size() < 2) {
				centerText = TEXT_NEED_PLAYERS;
			}
			else {
				centerText = TEXT_PRESS_TO_START;
			}
		}
		else if(state == State.GAME_STARTING) {
			centerText = TEXT_STARTING_IN + "\n" + Integer.toString((int)Math.ceil(startCountDown));
		}
		else if(state == State.GAME_RUNNING && gameRunningTime <= 3.0f) {
			centerText = TEXT_GAME_STARTED;
		}
		else if(state == State.GAME_DONE) {
			if(players.size() == 1) {
				centerText = players.get(0).getDescriptor().getName() + " Won!";
			}
			else if(players.size() == 0) {
				centerText = TEXT_TIE;
			}
			centerText += "\n\n\nPress CTRL + ENTER to restart\n";
		}
		
		if(state == State.WAITING_FOR_PLAYERS || state == State.GAME_DONE) {
			centerText += "\nCurrent map: " + mapFiles[mapIndex].nameWithoutExtension() + "\nPress LEFT/RIGHT to change map";
		}
		
		// Draw center text
		{
			int i = 0;
			for(String text : centerText.split("\n")) {
				arial15.draw(batch, text, (camera.viewportWidth - 280 - arial15.getBounds(text).width) / 2, (camera.viewportHeight - arial15.getBounds(text).height) / 2 - 200 + 20 * i++);
			}
		}
		
		batch.end();
	}
	
	private class SpawnPoint {
		public SpawnPoint(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		int x, y;
	}
	
	private void startGame() {
		// Clear stuff
		synchronized(AICompoGame.class) {
			players.clear();
			bullets.clear();
			entities.clear();
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
							Map.setTile(x, y, y == 0 || x == 0 || y == Map.HEIGHT - 1 || x == Map.WIDTH - 1 || random.nextFloat() < 0.1f ? 1 : 0);
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
		int spriteIndex = 0;
		for(PlayerDescriptor descriptor : playerDescriptors) {
			SpawnPoint spawn = getSpawnPoint();
			Player player = new Player(descriptor, arial11, 1 + (spriteIndex++ % 6), bullets, spawn.x, spawn.y);
			
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Send newline
						DataOutputStream out = new DataOutputStream(descriptor.getSocket().getOutputStream());
						out.writeBytes("\n");
						out.flush();
						
						// Listen for input
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(descriptor.getSocket().getInputStream()));
						String line;
						while((line = bufferedReader.readLine()) != null) {
							if(line.equals("TURN_LEFT")) {
								player.setTurnScale(-1.0f);
							}
							else if(line.equals("TURN_RIGHT")) {
								player.setTurnScale(1.0f);
							}
							else  if(line.equals("STOP_TURN")) {
								player.setTurnScale(0.0f);
							}
							else if(line.equals("MOVE_FORWARDS")) {
								player.setMovementScale(1.0f);
							}
							else if(line.equals("MOVE_BACKWARDS")) {
								player.setMovementScale(-1.0f);
							}
							else if(line.equals("STOP_MOVE")) {
								player.setMovementScale(0.0f);
							}
							else if(line.equals("SHOOT")) {
								player.shoot();
							}
							else if(line.contains("NAME ")) {
								descriptor.setName(line.substring(5));
							}
							else if(line.isEmpty()) {
								synchronized(AICompoGame.class) {
									// If the game has ended
									if(state == State.GAME_DONE) {
										out.writeBytes("RESTART\n");
										out.flush();
										return;
									}
									
									descriptor.prevTickTime = System.nanoTime();

									// Send game state
									String gameStatePacket = "";
									gameStatePacket += "PLAYERS_BEGIN\n";
									for(Player player : players) {
										gameStatePacket += player.getDescriptor().getID() + ";" + player.getDescriptor().getName() + ";" + player.getCenter().x + ";" + player.getCenter().y + ";" + player.getAngle() + "\n";
									}
									gameStatePacket += "PLAYERS_END\n";
									gameStatePacket += "BULLETS_BEGIN\n";
									for(Bullet bullet : bullets) {
										gameStatePacket += bullet.getID() + ";" + bullet.getOwner().getID() + ";" + bullet.getCenter().x + ";" + bullet.getCenter().y + ";" + bullet.getAngle() + "\n";
									}
									gameStatePacket += "BULLETS_END\n";
									gameStatePacket += "\n";
									out.writeBytes(gameStatePacket);
									out.flush();
								}
							}
							else {
								System.err.println("'" + player.getDescriptor().getName() + "' tried to perform unknown command '" + line + "'.");
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
			descriptor.setThread(thread);
		}
		
		try {
			// Send game state
			String gameStatePacket = "";
			gameStatePacket += "MAP_BEGIN\n";
			gameStatePacket += Integer.toString(Map.WIDTH) + ";" + Integer.toString(Map.HEIGHT) + "\n";
			for(int y = 0; y < Map.HEIGHT; ++y) {
				for(int x = 0; x < Map.WIDTH; ++x) {
					gameStatePacket += Integer.toString(x) + ";" + Integer.toString(y) + ";" + Integer.toString(Map.getTile(x, y)) + "\n";
				}
			}
			gameStatePacket += "MAP_END\n";
			gameStatePacket += "PLAYERS_BEGIN\n";
			synchronized(AICompoGame.class) {
				for(Player player : players) {
					gameStatePacket += player.getDescriptor().getID() + ";" + player.getDescriptor().getName() + ";" + player.getCenter().x + ";" + player.getCenter().y + ";" + player.getAngle() + "\n";
				}
				gameStatePacket += "PLAYERS_END\n";

				for(Player player : players) {
					new DataOutputStream(player.getDescriptor().getSocket().getOutputStream()).writeBytes(gameStatePacket);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		state = State.GAME_STARTING;
		startCountDown = 3.0f;
		gameRunningTime = 0.0f;
	}

	private SpawnPoint getSpawnPoint() {
		int i = 1;
		do {
			ArrayList<SpawnPoint> spawnPoints = new ArrayList<SpawnPoint>();
			for(int x = i; x < Map.WIDTH - i; ++x) {
				for(int y = i; y < Map.HEIGHT - i; ++y) {
					if(Map.isTile(x, y)) continue;
					if(x == i || y == i || x == Map.WIDTH - i - 1 || y == Map.HEIGHT - i - 1) {
						spawnPoints.add(new SpawnPoint(x, y));
					}
				}
			}
			
			while(!spawnPoints.isEmpty()) {
				int idx = random.nextInt(spawnPoints.size());
				SpawnPoint spawnPoint = spawnPoints.get(idx);
				spawnPoints.remove(idx);
				
				boolean tooClose = false;
				for(Player player : players) {
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
