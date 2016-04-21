package com.aicompo.example;

public class Map {
	private int[][] tiles;
	private int width, height;

	public static int TILE_SIZE = 40;
	public static float TILE_SIZEF = TILE_SIZE;

	public Map(int[][] tiles) {
		this.width = tiles.length;
		this.height = tiles[0].length;

		this.tiles = new int[width][height];
		for(int y = 0; y < height; ++y) {
			for(int x = 0; x < width; ++x) {
				this.tiles[x][y] = tiles[x][y];
			}
		}
	}

	public int getTile(int x, int y) {
		return tiles[x][y];
	}

	public boolean isEmpty(int x, int y) {
		return (tiles[x][y] == 0);
	}

	public boolean isOccupied(int x, int y) {
		return (tiles[x][y] == 1);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}