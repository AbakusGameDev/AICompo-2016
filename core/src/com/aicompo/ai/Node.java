package com.aicompo.ai;

public class Node {
	public Node parent;
	public int F, G, H;
	public int x, y;
	
	public Node(int x, int y) {
		this.x = x;
		this.y = y;
		parent = null;
		F = 0;
		G = 0;
		H = 0;
	}
}
