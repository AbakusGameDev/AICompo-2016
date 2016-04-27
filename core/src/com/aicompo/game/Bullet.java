package com.aicompo.game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class Bullet {
	private static int idCounter = 0;
	
	private final int id;
	private Vector2 position;
	private Vector2 velocity;
	private Sprite sprite;
	private Player owner;
	private float angle;
	private boolean destroyed;
	
	public int getID() {
		return id;
	}
	
	public Player getOwner() {
		return owner;
	}
	
	public Vector2 getCenter() {
		return new Vector2(position).add(4, 4);
	}
	
	public float getAngle() {
		return angle;
	}
	
	public Bullet(Player owner, Vector2 position, float angle) {
		this.id = idCounter++;
		this.owner = owner;
		this.position = position.sub(4, 4);
		this.angle = angle;
		this.destroyed = false;
		
		velocity = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).nor().scl(10.0f);
		sprite = new Sprite(new Texture("assets/bullet.png"));
		sprite.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		sprite.setSize(8, 8);
		sprite.setOrigin(4, 4);
	}

	public void update() {
		position.x += velocity.x;
		if(Map.isTile((int) (getCenter().x / Map.TILE_SIZEF), (int) (getCenter().y / Map.TILE_SIZEF))) {
			destroyed = true;
		}
		
		position.y += velocity.y;
		if(Map.isTile((int) (getCenter().x / Map.TILE_SIZEF), (int) (getCenter().y / Map.TILE_SIZEF))) {
			destroyed = true;
		}
	}
	
	public void draw(SpriteBatch batch) {
		sprite.setPosition(position.x, position.y);
		sprite.draw(batch);
	}

	public void destroy() { destroyed = true; }

	public boolean isDestroyed() { return destroyed; }
}
