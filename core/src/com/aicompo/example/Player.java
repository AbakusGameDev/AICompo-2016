package com.aicompo.example;

import com.badlogic.gdx.math.Vector2;

public class Player {
	private int id;
	private String name;
	private Vector2 position;
	private float angle;

	public Player(int id, String name, Vector2 position, float angle) {
		this.id = id;
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
	
	public boolean equals(Object object) {
		return object instanceof Player && this.id == ((Player)object).id;
	}
}
