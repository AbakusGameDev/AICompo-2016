package com.aicompo.main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import com.aicompo.ai.*;
import com.aicompo.ai.Bullet;
import com.aicompo.ai.Map;
import com.badlogic.gdx.math.Vector2;

public class Main implements Runnable {
	private static String hostIp = "127.0.0.1";
	private static String hostPort = "45556";

	private AI ai = new AI();
	private int playerID;

	public HashMap<Integer, com.aicompo.ai.Player> playerMap;
	public HashMap<Integer, com.aicompo.ai.Bullet> bulletMap;

	private DataOutputStream outputStream;

	public Main() {
		playerMap = new HashMap<Integer, com.aicompo.ai.Player>();
		bulletMap = new HashMap<Integer, com.aicompo.ai.Bullet>();
	}

	@Override
	public void run() {
		try {
			// Connect to host
			Socket socket = new Socket(hostIp, Integer.parseInt(hostPort));
			if(socket.isConnected()) {

				// Write player name to the socket
				new DataOutputStream(socket.getOutputStream()).writeBytes(AI.PLAYER_NAME + "\n");

				// Get our player ID
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				playerID = Integer.parseInt(bufferedReader.readLine());
				
				// Do stuff while we're connected
				while(socket.isConnected()) {

					// Read server packets
					String line;
					while(!(line = bufferedReader.readLine()).isEmpty()) {
						if(line.equals("PLAYERS_BEGIN")) {
							// Input: PLAYERS_BEGIN
							// This is where we parse the otherPlayers.
							// The data is send in the given format:
							// id;name;x;y;angle
							// until PLAYERS_END is received.

							ArrayList<Integer> ids = new ArrayList<Integer>();

							// Parse otherPlayers
							String playerDataString;
							while(!(playerDataString = bufferedReader.readLine()).equals("PLAYERS_END")) {
								// Split player data
								String[] playerData = playerDataString.split(";");

								// Get player ID
								int playerID = Integer.parseInt(playerData[0]);

								// If player doesn't exist, create it
								if(!playerMap.containsKey(playerID)) {
									Player player = new com.aicompo.ai.Player(Integer.parseInt(playerData[0]));

									// Add to map
									playerMap.put(playerID, player);

									if(this.playerID == playerID) {
										// If this is our player, store it
										ai.player = player;
									}
									else {
										// Else add to list of other players
										ai.otherPlayers.add(player);
									}
								}

								// Update player values
								playerMap.get(playerID).updateValues(playerData[1], new Vector2(Float.parseFloat(playerData[2]), Float.parseFloat(playerData[3])), Float.parseFloat(playerData[4]));

								// Store id
								ids.add(playerID);
							}

							// Remove players
							if(!ids.contains(this.playerID)) {
								playerMap.remove(this.playerID);
								ai.player = null;
							}

							for(int i = ai.otherPlayers.size() - 1; i >= 0; i--) {
								int playerID = ai.otherPlayers.get(i).getID();
								if(!ids.contains(playerID)) {
									playerMap.remove(playerID);
									ai.otherPlayers.remove(i);
								}
							}
						}
						else if(line.equals("BULLETS_BEGIN")) {
							// Input: BULLETS_BEGIN
							// This is where we parse the bullets.
							// The data is send in the given format:
							// id;ownerid;x;y;angle
							// until BULLETS_END is received.

							/*ArrayList<Integer> ids = new ArrayList<Integer>();

							// Parse otherPlayers
							String bulletsDataString;
							while(!(bulletsDataString = bufferedReader.readLine()).equals("BULLETS_END")) {
								// Split player data
								String[] bulletData = bulletsDataString.split(";");

								// Get player ID
								int bulletID = Integer.parseInt(bulletData[0]);

								// If player doesn't exist, create it
								if(!bulletMap.containsKey(bulletID)) {
									Bullet bullet = new com.aicompo.ai.Bullet(Integer.parseInt(bulletDataData[0]));

									// Add to map
									playerMap.put(playerID, player);

									if(this.playerID == playerID) {
										// If this is our player, store it
										ai.player = player;
									}
									else {
										// Else add to list of other otherPlayers
										ai.otherPlayers.add(player);
									}
								}

								// Update player values
								playerMap.get(playerID).updateValues(playerData[1], new Vector2(Float.parseFloat(playerData[2]), Float.parseFloat(playerData[3])), Float.parseFloat(playerData[4]));

								// Store id
								ids.add(playerID);
							}

							// Remove otherPlayers
							if(!ids.contains(this.playerID)) {
								playerMap.remove(this.playerID);
								ai.player = null;
							}

							for(int i = ai.otherPlayers.size() - 1; i >= 0; i--) {
								int playerID = ai.otherPlayers.get(i).getID();
								if(!ids.contains(playerID)) {
									playerMap.remove(playerID);
									ai.otherPlayers.remove(i);
								}
							}*/

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
							com.aicompo.ai.Map.initialize(Integer.parseInt(mapSizeData[0]), Integer.parseInt(mapSizeData[1]));

							// Parse map
							String mapDataString;
							while(!(mapDataString = bufferedReader.readLine()).equals("MAP_END")) {
								String[] tileData = mapDataString.split(";");
								Map.setTile(Integer.parseInt(tileData[0]), Integer.parseInt(tileData[1]), Integer.parseInt(tileData[2]));
							}

							ai.mapReceived();
						}
					}

					ai.outputStream = new DataOutputStream(socket.getOutputStream());
					if(playerMap.containsKey(playerID)) {
						ai.update();
					}

					// Ends your turn. DON'T FORGET IT!
					ai.outputStream.writeBytes("\n");

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
        
		new Main().run();
	}
}
