package com.aicompo.ai;

import com.badlogic.gdx.math.Vector2;

import java.awt.*;
import java.util.ArrayList;

public class AStar {
    class Node {
        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        final public int x, y;

        public float f;
        public float g;
        public float h;

        public Node parent;
    }

    public ArrayList<Point> getPath(Vector2 start, Vector2 end) {
        return getPath(new Point((int)(start.x / Map.TILE_SIZEF), (int)(start.y / Map.TILE_SIZEF)), new Point((int)(end.x / Map.TILE_SIZEF), (int)(end.y / Map.TILE_SIZEF)));
    }

    public ArrayList<Point> getPath(Point start, Point end) {
        ArrayList<Node> open = new ArrayList<>();
        ArrayList<Node> closed = new ArrayList<>();
        ArrayList<Point> path = new ArrayList<>();

        Node[][] nodeGrid = new Node[Map.WIDTH][Map.HEIGHT];
        for(int y = 0; y < Map.HEIGHT; y++) {
            for(int x = 0; x < Map.WIDTH; x++) {
                nodeGrid[x][y] = new Node(x, y);
            }
        }

        Node startNode = nodeGrid[start.x][start.y];
        Node endNode = nodeGrid[end.x][end.y];

        open.add(startNode);

        while(!open.isEmpty()) {
            // Extract the node with the min f score
            Node node = extractMin(open);

            // If we've arrived, construct the path
            if(node == endNode) {
                while(node != startNode) {
                    path.add(0, new Point(node.x, node.y));
                    node = node.parent;
                }
                break;
            }

            Node[] neighbours = new Node[] { nodeGrid[node.x][node.y - 1],  nodeGrid[node.x - 1][node.y], nodeGrid[node.x + 1][node.y], nodeGrid[node.x][node.y + 1]};
            for(Node neighbourNode : neighbours) {
                if(!Map.isTile(neighbourNode.x, neighbourNode.y) && !closed.contains(neighbourNode)) {
                    if(!open.contains(neighbourNode) || node.g + 10 < neighbourNode.g) {
                        neighbourNode.parent = node;
                        neighbourNode.g = node.g + 10;
                        neighbourNode.h = getHeuristic(neighbourNode, endNode);
                        neighbourNode.f = neighbourNode.g + neighbourNode.f;
                        if(!open.contains(neighbourNode)) {
                            open.add(neighbourNode);
                        }
                    }
                }
            }

            closed.add(node);
        }

        return path;
    }

    float getHeuristic(Node start, Node end) {
        return Math.abs(end.x - start.x) + Math.abs(end.y - start.y);
    }

    Node extractMin(ArrayList<Node> list) {
        Node lowestNode = list.get(0);
        for(int i = 1; i < list.size(); i++) {
            Node node = list.get(i);
            if(node.f < lowestNode.f) {
                lowestNode = node;
            }
        }
        list.remove(lowestNode);
        return lowestNode;
    }
}
