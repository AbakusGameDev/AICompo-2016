package com.aicompo.ai;

public class Map {
	private static int[][] tiles;
	
	public static int width, height;
	
	public static int TILE_SIZE = 40;
	public static float TILE_SIZEF = TILE_SIZE;
	
	public static void initialize(int width, int height) {
		Map.width = width;
		Map.height = height;
		tiles = new int[height][width];
	}
	
	public static void setTile(int x, int y, int tile) {
		tiles[y][x] = tile;
	}
	
	public static int getTile(int x, int y) {
		return tiles[y][x];
	}
	
	public static int getWidth() {
		return width;
	}
	
	public static int getHeight() {
		return height;
	}
}
