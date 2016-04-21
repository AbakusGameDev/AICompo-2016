package com.aicompo.game;

import java.net.Socket;

public class PlayerDescriptor {
	enum Status {
		READY("READY"),
		ALIVE("ALIVE"),
		DEAD("DEAD");
		
		private String str;
		
		private Status(String str) {
			this.str = str;
		}
		
		public String toString() {
			return str;
		}
	}
	
	private static int idCounter = 0;

	private final int id;
	private final Socket socket;
	private String name;
	private Thread thread;
	private Status status;
	public long prevTickTime;
	
	public PlayerDescriptor(Socket socket, String name) {
		this.id = idCounter++;
		this.name = name;
		this.socket = socket;
		this.status = Status.READY;
		this.prevTickTime = 0;
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}
	
	public Thread getThread() {
		return thread;
	}
}
