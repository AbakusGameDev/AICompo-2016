package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

public class Player {
	private int id;
	private String name;
	private Vector2 position;
	private float angle;
	private boolean alive;

	public Player(int id) {
		this.id = id;
	}

	public void updateValues(String name, Vector2 position, float angle, boolean alive) {
		this.name = name;
		this.position = position;
		this.angle = angle;
		this.alive = alive;
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public boolean isAlive() {
		return alive;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public float getAngle() {
		return angle;
	}
}
