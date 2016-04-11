package com.aicompo.game;

import java.awt.*;
import java.net.Socket;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Player {
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
	public long previousPacketTime;

	private Vector2 position;
	private Vector2 size;
	private float angle;
	private Sprite sprite;
	private float turnSpeed;
	private float moveSpeed;
	private float shootCooldown;
	private ArrayList<Bullet> bullets;
	private float moveScale;
	private float turnScale;
	private boolean shooting;

	private float desiredAngle;

	public ArrayList<Point> removedTiles;
	
	public Player(Socket socket, String name, ArrayList<Bullet> bullets) {
		// Persisten player data
		this.id = idCounter;
		this.name = name;
		this.socket = socket;

		this.status = Status.READY;
		this.previousPacketTime = 0;

		this.bullets = bullets;
		removedTiles = new ArrayList<>();

		sprite = new Sprite(new Texture("tank_" + (1 + (idCounter % 6)) + ".png"));
		sprite.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		sprite.setSize(48.0f, 48.0f);
		sprite.setOrigin(48.0f / 2, 48.0f / 2);
		size = new Vector2(31.0f, 31.0f);
		turnSpeed = 3.0f;
		moveSpeed = 2.0f;
		shootCooldown = 0.0f;
		moveScale = 0.0f;
		turnScale = 0.0f;
		shooting = false;

		idCounter++;
	}

	public void spawn(int x, int y, float initAngle) {
		position = new Vector2(x * Map.TILE_SIZE, y * Map.TILE_SIZE);
		angle = initAngle;
		status = Status.ALIVE;
	}

	public void update() {
		Vector2 velocity = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(moveSpeed * moveScale);
		if(turnScale == -2.0f) {
			float angleDiff = ((((desiredAngle - angle) % 360) + 540) % 360) - 180;
			angle += Math.max(-turnSpeed, Math.min(angleDiff, turnSpeed));
		}
		else {
			angle += turnSpeed * turnScale;
		}
		if(shooting && shootCooldown <= 0.0f) {
			bullets.add(new Bullet(this, getCenter().add(new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(16.0f)), angle));
			shootCooldown = 1.0f;
		}
		shooting = false;
		
		position.y += velocity.y;
		if(velocity.y > 0.0f) {
			int x0 = (int) Math.floor(position.x / Map.TILE_SIZEF), x1 = (int) Math.floor((position.x + size.x - 1) / Map.TILE_SIZEF);
			int y0 = (int) Math.floor((position.y + size.y - 1) / Map.TILE_SIZEF), y1 = (int) Math.floor((position.y + size.y - 1 + velocity.y) / Map.TILE_SIZEF);
			for(int y = y0; y <= y1 && velocity.y > 0.0f; ++y) {
				for(int x = x0; x <= x1 && velocity.y > 0.0f; ++x) {
					if(Map.isTile(x, y)) {
						position.y = y * Map.TILE_SIZEF - size.y;
					}
				}
			}
		}
		else if(velocity.y < 0.0f) {
			int x0 = (int) Math.floor(position.x / Map.TILE_SIZEF), x1 = (int) Math.floor((position.x + size.x - 1) / Map.TILE_SIZEF);
			int y0 = (int) Math.floor(position.y / Map.TILE_SIZEF), y1 = (int) Math.floor((position.y + velocity.y) / Map.TILE_SIZEF);
			for(int y = y0; y >= y1 && velocity.y < 0.0f; --y) {
				for(int x = x0; x <= x1 && velocity.y < 0.0f; ++x) {
					if(Map.isTile(x, y)) {
						position.y = y * Map.TILE_SIZEF + Map.TILE_SIZEF;
					}
				}
			}
		}
		
		position.x += velocity.x;
		if(velocity.x > 0.0f) {
			int y0 = (int) Math.floor(position.y / Map.TILE_SIZEF), y1 = (int) Math.floor((position.y + size.y - 1) / Map.TILE_SIZEF);
			int x0 = (int) Math.floor((position.x + size.x - 1) / Map.TILE_SIZEF), x1 = (int) Math.floor((position.x + size.x - 1 + velocity.x) / Map.TILE_SIZEF);
			for(int x = x0; x <= x1 && velocity.x > 0.0f; ++x) {
				for(int y = y0; y <= y1 && velocity.x > 0.0f; ++y) {
					if(Map.isTile(x, y)) {
						position.x = x * Map.TILE_SIZEF - size.x;
					}
				}
			}
		}
		else if(velocity.x < 0.0f) {
			int y0 = (int) Math.floor(position.y / Map.TILE_SIZEF), y1 = (int) Math.floor((position.y + size.y - 1) / Map.TILE_SIZEF);
			int x0 = (int) Math.floor(position.x / Map.TILE_SIZEF), x1 = (int) Math.floor((position.x + velocity.x) / Map.TILE_SIZEF);
			for(int x = x0; x >= x1 && velocity.x < 0.0f; --x) {
				for(int y = y0; y <= y1 && velocity.x < 0.0f; ++y) {
					if(Map.isTile(x, y)) {
						position.x = x * Map.TILE_SIZEF + Map.TILE_SIZEF;
					}
				}
			}
		}
		
		for(Bullet bullet : bullets) {
			if(bullet.getOwner() != this) {
				Vector2 point = bullet.getCenter().sub(getCenter());
				Matrix3 mat = new Matrix3();
				mat.rotate(angle);
				point.mul(mat);
				if((new Rectangle(-size.x / 2, -size.y/ 2, size.x, size.y)).contains(point)) {
					status = Status.DEAD;
					bullet.destroy();
					break;
				}
			}
		}
		
		shootCooldown -= Gdx.graphics.getDeltaTime();
	}

	public void draw(SpriteBatch batch, BitmapFont font) {
		sprite.setRotation(angle);
		sprite.setPosition(position.x + (size.x - sprite.getWidth()) / 2, position.y + (size.y - sprite.getHeight()) / 2);
		sprite.draw(batch);
		font.draw(batch, name, getCenter().x - font.getBounds(name).width / 2, position.y - 20);
	}
	
	public void shoot() {
		shooting = true;
	}

	public void setMovementScale(float scl) {
		moveScale = scl;
	}

	public void setTurnScale(float scl) {
		turnScale = scl;
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

	public void setDesiredAngle(float angle) {
		this.desiredAngle = angle;
		this.turnScale = -2.0f;
	}

	public Socket getSocket() {
		return socket;
	}

	public Status getStatus() {
		return status;
	}

	public void setThread(Thread thread) {
		this.thread = thread;
	}

	public Thread getThread() {
		return thread;
	}

	public Vector2 getCenter() {
		return new Vector2(position).add(new Vector2(size).scl(0.5f));
	}

	public float getAngle() {
		return angle;
	}
}
