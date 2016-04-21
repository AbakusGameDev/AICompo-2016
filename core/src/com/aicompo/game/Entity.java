package com.aicompo.game;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Entity {
	Entity() {
		AICompoGame.addEntity(this);
	}
	
	public void destroy() {
		AICompoGame.removeEntity(this);
	}
	
	public void update() {
	}
	
	public void draw(SpriteBatch spriteBatch) {
	}
	
	public void draw(ShapeRenderer shapeRenderer) {
	}
}
