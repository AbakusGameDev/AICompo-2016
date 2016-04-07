package com.aicompo.game;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class ControlledPlayer implements Runnable {
	private static final String PLAYER_NAME = "Controlled player";

	@Override
	public void run() {
		try {
			// Connect to host
			Socket socket = new Socket("127.0.0.1", 45556);
			if(socket.isConnected()) {
				// Write player name to the socket
				new DataOutputStream(socket.getOutputStream()).writeBytes(PLAYER_NAME + "\n");
				
				// Do stuff while we're connected
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				while(socket.isConnected()) {

					while(!bufferedReader.readLine().isEmpty());

					DataOutputStream out = new DataOutputStream(socket.getOutputStream());

					// Player movement (WASD / cursor keys)
					if(Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
						out.writeBytes("MOVE_FORWARDS\n");
					}
					else if(Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
						out.writeBytes("MOVE_BACKWARDS\n");
					}
					else {
						out.writeBytes("STOP_MOVE\n");
					}

					if(Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
						out.writeBytes("TURN_RIGHT\n");
					}
					else if(Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
						out.writeBytes("TURN_LEFT\n");
					}
					else {
						out.writeBytes("STOP_TURN\n");
					}

					if(Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
						out.writeBytes("SHOOT\n");
					}

					// End turn
					out.writeBytes("\n");

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
}
