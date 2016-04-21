package com.aicompo.game;

import java.util.ArrayList;

import com.aicompo.game.PlayerDescriptor.Status;
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

public class Player extends Entity {
	
	private final PlayerDescriptor descriptor;
	private Vector2 position;
	private Vector2 size;
	private float angle;
	private Sprite sprite;
	private float turnSpeed;
	private float moveSpeed;
	private BitmapFont font;
	private float shootCooldown;
	private ArrayList<Bullet> bullets;
	private float moveScale;
	private float turnScale;
	private boolean shooting;
	
	public PlayerDescriptor getDescriptor() {
		return descriptor;
	}
	
	public Vector2 getCenter() {
		return new Vector2(position).add(new Vector2(size).scl(0.5f));
	}
	
	public float getAngle() {
		return angle;
	}
	
	public Player(PlayerDescriptor descriptor, BitmapFont font, int spriteIndex, ArrayList<Bullet> bullets, int x, int y) {
		this.descriptor = descriptor;
		this.bullets = bullets;
		this.font = font;
		sprite = new Sprite(new Texture("tank_" + spriteIndex + ".png"));
		sprite.getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
		sprite.setSize(48.0f, 48.0f);
		sprite.setOrigin(48.0f / 2, 48.0f / 2);
		position = new Vector2(x * Map.TILE_SIZE, y * Map.TILE_SIZE);
		size = new Vector2(31.0f, 31.0f);
		turnSpeed = 3.0f;
		moveSpeed = 2.0f;
		angle = 0.0f;
		shootCooldown = 0.0f;
		moveScale = 0.0f;
		turnScale = 0.0f;
		shooting = false;

		descriptor.setStatus(Status.ALIVE);
		AICompoGame.addPlayer(this);
	}
	
	@Override
	public void destroy() {
		descriptor.setStatus(Status.DEAD);
		AICompoGame.removePlayer(this);
		super.destroy();
	}
	
	@Override
	public void update() {
		Vector2 velocity = new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(moveSpeed * moveScale);
		angle += turnSpeed * turnScale;
		if(shooting && shootCooldown <= 0.0f) {
			new Bullet(descriptor, getCenter().add(new Vector2(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(16.0f)), angle);
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
			if(bullet.getOwner() != descriptor) {
				Vector2 point = bullet.getCenter().sub(getCenter());
				Matrix3 mat = new Matrix3();
				mat.rotate(angle);
				point.mul(mat);
				if((new Rectangle(-size.x / 2, -size.y/ 2, size.x, size.y)).contains(point)) {
					destroy();
					bullet.destroy();
					break;
				}
			}
		}
		
		shootCooldown -= Gdx.graphics.getDeltaTime();
	}

	@Override
	public void draw(SpriteBatch batch) {
		sprite.setRotation(angle);
		sprite.setPosition(position.x + (size.x - sprite.getWidth()) / 2, position.y + (size.y - sprite.getHeight()) / 2);
		sprite.draw(batch);
		font.draw(batch, descriptor.getName(), getCenter().x - font.getBounds(descriptor.getName()).width / 2, position.y - 20);
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
}
