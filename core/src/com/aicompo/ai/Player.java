package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

public class Player {
	private int id;
	private String name;
	private Vector2 position;
	private float angle;

	public Player(int id) {
		this.id = id;
	}

	public void updateValues(String name, Vector2 position, float angle) {
		this.name = name;
		this.position = position;
		this.angle = angle;
	}
	
	public int getID() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public float getAngle() {
		return angle;
	}
}
