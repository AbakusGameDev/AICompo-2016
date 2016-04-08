package com.aicompo.ai;

public class Map {
	public static final int WIDTH = 18;
	public static final int HEIGHT = 18;
	public static final int TILE_SIZE = 40;
	public static final float TILE_SIZEF = TILE_SIZE;
	public static int tiles[][] = new int[HEIGHT][WIDTH];

	public static void setTile(int x, int y, int tile) {
		tiles[y][x] = tile;
	}

	public static int getTile(int x, int y) {
		return tiles[y][x];
	}

	public static boolean isTile(int x, int y) {
		return tiles[y][x] == 1;
	}
}