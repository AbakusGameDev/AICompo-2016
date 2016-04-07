package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

public class Bullet {
	private int id;
	private int ownerId;
	private Vector2 position;
	private float angle;
	
	public Bullet(int id, int ownerId, Vector2 position, float angle) {
		this.id = id;
		this.ownerId = ownerId;
		this.position = position;
		this.angle = angle;
	}
	
	public int getID() {
		return id;
	}
	
	public int getOwnerID() {
		return ownerId;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public float getAngle() {
		return angle;
	}
}
