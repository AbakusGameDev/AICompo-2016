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
								playerMap.get(playerID).updateValues(playerData[1], new Vector2(Float.parseFloat(playerData[2]), Float.parseFloat(playerData[3])), Float.parseFloat(playerData[4]), Boolean.parseBoolean(playerData[5]));
							}
						}
						else if(line.equals("BULLETS_BEGIN")) {
							// Input: BULLETS_BEGIN
							// This is where we parse the bullets.
							// The data is send in the given format:
							// id;ownerid;x;y;angle
							// until BULLETS_END is received.

							ArrayList<Integer> ids = new ArrayList<Integer>();

							// Parse otherPlayers
							String bulletsDataString;
							while(!(bulletsDataString = bufferedReader.readLine()).equals("BULLETS_END")) {
								// Split player data
								String[] bulletData = bulletsDataString.split(";");

								// Get player ID
								int bulletID = Integer.parseInt(bulletData[0]);

								// If player doesn't exist, create it
								if(!bulletMap.containsKey(bulletID)) {
									Bullet bullet = new com.aicompo.ai.Bullet(Integer.parseInt(bulletData[0]), playerMap.get(Integer.parseInt(bulletData[1])));

									// Add to map
									bulletMap.put(bulletID, bullet);

									// Add to list of bullets
									ai.bullets.add(bullet);
								}

								// Update bullet values
								bulletMap.get(bulletID).updateValues(new Vector2(Float.parseFloat(bulletData[2]), Float.parseFloat(bulletData[3])), Float.parseFloat(bulletData[4]));

								// Store id
								ids.add(bulletID);
							}

							// Remove missing bullets
							for(int i = ai.bullets.size() - 1; i >= 0; i--) {
								int bulletID = ai.bullets.get(i).getID();
								if(!ids.contains(bulletID)) {
									bulletMap.remove(bulletID);
									ai.bullets.remove(i);
								}
							}
						}
						else if(line.equals("MAP_BEGIN")) {
							// Input: MAP_BEGIN
							// This is where we parse the map.
							// The data are send in the format:
							// x;y;tile
							// until MAP_END is received.

							boolean changed = false;

							// Parse map
							String mapDataString;
							while(!(mapDataString = bufferedReader.readLine()).equals("MAP_END")) {
								String[] tileData = mapDataString.split(";");
								Map.setTile(Integer.parseInt(tileData[0]), Integer.parseInt(tileData[1]), Integer.parseInt(tileData[2]));
								changed = true;
							}

							if(changed) {
								ai.mapChanged();
							}
						}
					}

					ai.outputStream = new DataOutputStream(socket.getOutputStream());
					if(ai.player.isAlive()) {
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
