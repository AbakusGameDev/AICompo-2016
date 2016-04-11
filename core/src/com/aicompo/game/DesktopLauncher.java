package com.aicompo.game;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.aicompo.game.AICompoGame;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "AI Compo";
		config.vSyncEnabled = true;
		config.foregroundFPS = 60;
		config.width = 1000;
		config.height = 720;
		new LwjglApplication(new AICompoGame(), config);
	}
}
