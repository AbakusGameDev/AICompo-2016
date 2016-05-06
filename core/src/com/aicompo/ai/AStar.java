package com.aicompo.ai;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

public class AStar {
	private Node[][] nodeGrid;
	
	public AStar() {
		nodeGrid = new Node[Map.WIDTH][Map.HEIGHT];
    	for (int y = 0; y < Map.HEIGHT; y++) {
    		for (int x = 0; x < Map.WIDTH; x++) {
    			nodeGrid[x][y] = new Node(x, y);
    		}
    	}
	}
	
	private Node extractMin(ArrayList<Node> list) {
        Node lowestNode = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            Node node = list.get(i);
            if (node.F < lowestNode.F) {
                lowestNode = node;
            }
        }
        list.remove(lowestNode);
        return lowestNode;
    }
	
    public ArrayList<Node> calculatePath(int startX, int startY, int targetX, int targetY) {
    	ArrayList<Node> open = new ArrayList<Node>(),
    					closed = new ArrayList<Node>(),
    					path = new ArrayList<Node>();
    	
    	Node startNode = nodeGrid[startX][startY];
    	Node targetNode = nodeGrid[targetX][targetY];
    	
    	open.add(startNode);
    	
    	while (open.size() > 0) {
    		// Finds the node with the lowest cost in the list of open nodes. Breaks if the list is empty.
    		Node centerNode = extractMin(open);
    		
    		// If it's the target node, break. Adds it to the list of closed nodes.
    		if (centerNode == targetNode) {
    			Node node = targetNode;
    			while (node != startNode) {
    				path.add(0, node);
    				node = node.parent;
    			}
    			break;
    		}
    		closed.add(centerNode);
    		
    		int x = centerNode.x, y = centerNode.y;
    		
    		// Loops through the orthogonal nodes.
    		Node[] nodesToLoopThrough = new Node[] {nodeGrid[x][y - 1], nodeGrid[x - 1][y], nodeGrid[x + 1][y], nodeGrid[x][y + 1]};
    		for (Node node : nodesToLoopThrough) {
    			if (!Map.isTile(node.x, node.y) && !closed.contains(node)) {
					if (!open.contains(node) || centerNode.G + 10 < node.G) {
						node.parent = centerNode;
						node.G = centerNode.G + 10;
						node.H = (Math.abs(targetX - node.x) + Math.abs(targetY - node.y)) * 10;
						node.F = node.G + node.H;
						if (!open.contains(node)) open.add(node);
					}
    			}
    		}
    		
    		// Loops through the diagonal nodes.
    		nodesToLoopThrough = new Node[] {nodeGrid[x - 1][y - 1], nodeGrid[x + 1][y - 1], nodeGrid[x - 1][y + 1], nodeGrid[x + 1][y + 1]};
    		for (Node node : nodesToLoopThrough) {
    			if (!Map.isTile(node.x, node.y) && !closed.contains(node) && !Map.isTile(x, node.y) && !Map.isTile(node.x, y)) {
					if (!open.contains(node) || centerNode.G + 14 < node.G) {
						node.parent = centerNode;
						node.G = centerNode.G + 14;
						node.H = (Math.abs(targetX - node.x) + Math.abs(targetY - node.y)) * 10;
						node.F = node.G + node.H;
						if (!open.contains(node)) open.add(node);
					}
    			}
    		}
    	}
    	
    	return path;
    }
    
    public ArrayList<Node> calculatePath(Vector2 start, Vector2 target){
    	return calculatePath((int)Math.floor(start.x / Map.TILE_SIZEF), (int)Math.floor(start.y / Map.TILE_SIZEF),
    						 (int)Math.floor(target.x / Map.TILE_SIZEF), (int)Math.floor(target.y / Map.TILE_SIZEF));
    }
}
