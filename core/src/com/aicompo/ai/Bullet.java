package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

public class Bullet {
	private int id;
	private Player owner;
	private Vector2 position;
	private float angle;

	public final static float SPEED = 10.0f; // pixels / sec
	public final static float SIZE = 1.0f; // A point
	
	public Bullet(int id, Player owner) {
		this.id = id;
		this.owner = owner;
	}
	
	public int getID() {
		return id;
	}
	
	public Player getOwner() {
		return owner;
	}
	
	public Vector2 getPosition() {
		return position;
	}
	
	public float getAngle() {
		return angle;
	}

	public void updateValues(Vector2 position, float angle) {
		this.position = position;
		this.angle = angle;
	}
}
