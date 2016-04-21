package com.aicompo.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class AStar {
	private Map map;
	private Node[][][] nodes;
	
	public AStar(Map map) {
		this.map = map;

		// Create node grid
		nodes = new Node[map.getWidth()][map.getHeight()][4];
		for(int y = 1; y < map.getHeight() - 1; ++y) {
			for(int x = 1; x < map.getWidth() - 1; ++x) {
				if(map.getTile(x, y) == 0) {
					if(map.getTile(x, y - 1) == 0) {
						nodes[x][y][0] = nodes[x][y - 1][2] = new Node(x + 0.5f, y); // top node
					}
					if(map.getTile(x - 1, y) == 0) {
						nodes[x][y][1] = nodes[x - 1][y][3] = new Node(x, y + 0.5f); // left node
					}
				}
			}
		}
		
		// Find neighbors
		for(int y = 1; y < map.getHeight() - 1; ++y) {
			for(int x = 1; x < map.getWidth() - 1; ++x) {
				Node topNode = nodes[x][y][0];
				if(topNode != null) {
					for(int i = 0; i < 4; ++i) {
						if(i != 0 && nodes[x][y][i] != null) {
							topNode.neighbors.add(nodes[x][y][i]);
						}
						if(i != 2 && nodes[x][y - 1][i] != null) {
							topNode.neighbors.add(nodes[x][y - 1][i]);
						}
					}
				}

				Node leftNode = nodes[x][y][1];
				if(leftNode != null) {
					for(int i = 0; i < 4; ++i) {
						if(i != 1 && nodes[x][y][i] != null) {
							leftNode.neighbors.add(nodes[x][y][i]);
						}
						if(i != 3 && nodes[x - 1][y][i] != null) {
							leftNode.neighbors.add(nodes[x - 1][y][i]);
						}
					}
				}
			}
		}
	}
	
	static public class Node {
		public Node(float x, float y) {
			this.x = x;
			this.y = y;
			this.g = 0;
			this.f = 0;
			this.neighbors = new ArrayList<Node>();
		}
		
		float x, y;
		float g;
		float f;
		Node parent;
		ArrayList<Node> neighbors;
	}
	
	public Node extractMinF(ArrayList<Node> nodes) {
		Node minNode = null;
		float minF = Float.MAX_VALUE;
		for(Node node : nodes) {
			if(node.f < minF) {
				minF = node.f;
				minNode = node;
			}
		}
		nodes.remove(minNode);
		return minNode;
	}
	
	public float calcHeuristicCost(Node start, Node end) {
		return Math.abs(start.x - end.x) + Math.abs(start.y - end.y);
	}
	
	public Stack<Node> getPathToTarget(float startX, float startY, float endX, float endY) {
		
		ArrayList<Node> openList = new ArrayList<Node>();
		ArrayList<Node> closedList = new ArrayList<Node>();
		HashMap<Node, Node> cameFrom = new HashMap<Node, Node>();
		
		// Get start and end node
		Node startNode = null;
		Node endNode = null;
		float minStartNodeDist = Float.MAX_VALUE;
		float minEndNodeDist = Float.MAX_VALUE;
		for(int y = 1; y < map.getHeight() - 1; ++y) {
			for(int x = 1; x < map.getWidth() - 1; ++x) {
				for(int i = 0; i < 2; ++i) {
					Node node = nodes[x][y][i];
					if(node != null) {
						float startNodeDist = (float) Math.sqrt(Math.pow(node.x - startX, 2.0f) + Math.pow(node.y - startY, 2.0f));
						if(startNodeDist < minStartNodeDist) {
							startNode = node;
							minStartNodeDist = startNodeDist;
						}
						float endNodeDist = (float) Math.sqrt(Math.pow(node.x - endX, 2.0f) + Math.pow(node.y - endY, 2.0f));
						if(endNodeDist < minEndNodeDist) {
							endNode = node;
							minEndNodeDist = endNodeDist;
						}
					}
				}
			}
		}
		
		// Do A*
		startNode.g = 0;
		startNode.f = calcHeuristicCost(startNode, endNode);

		openList.add(startNode);
		while(!openList.isEmpty()) {

			/*int w = map.getWidth() + 1, h = map.getHeight() + 1;
			char state[][] = new char[w][h];
			for(int y = 0; y < h; ++y) {
				for(int x = 0; x < w; ++x) {
					state[x][y] = '-';
				}
			}
			
			for(Node n : openList) {
				state[(int)(n.x)][(int)(n.y)] = 'O';
			}
			
			for(Node n : closedList) {
				state[(int)(n.x)][(int)(n.y)] = 'X';
			}

			state[(int)(startNode.x)][(int)(startNode.y)] = 'S';
			state[(int)(endNode.x)][(int)(endNode.y)] = 'G';
			
			for(int y = 0; y < h; ++y) {
				String line = y % 2 == 0 ? "" : " ";
				for(int x = 0; x < (y % 2 == 0 ? w - 1 : w); ++x) {
					line += state[x][y] + " ";
				}
				System.out.println(line);
			}
			System.out.println();*/
			
			Node currentNode = extractMinF(openList);
			if(currentNode == endNode) {
				return reconstructPath(cameFrom, endNode);
			}
			
			closedList.add(currentNode);
			
			for(Node neighborNode : currentNode.neighbors) {
				if(neighborNode == null || closedList.contains(neighborNode)) {
					continue;
				}

				float gScore = currentNode.g + (float) (Math.sqrt(Math.pow(neighborNode.x - currentNode.x, 2.0f) + Math.pow(neighborNode.y - currentNode.y, 2.0f)));
				if(!openList.contains(neighborNode) || gScore < neighborNode.g) {
					cameFrom.put(neighborNode, currentNode);
					neighborNode.g = gScore;
					neighborNode.f = neighborNode.g + calcHeuristicCost(neighborNode, endNode);
					if(!openList.contains(neighborNode)) {
						openList.add(neighborNode);
					}
				}
			}
		}
		return null;
	}

	private Stack<Node> reconstructPath(HashMap<Node, Node> cameFrom, Node currentNode) {
		Stack<Node> totalPath = new Stack<Node>(); totalPath.push(currentNode);
		while(cameFrom.containsKey(currentNode)) {
			currentNode = cameFrom.get(currentNode);
			totalPath.push(currentNode);
		}
		return totalPath;
	}
}
