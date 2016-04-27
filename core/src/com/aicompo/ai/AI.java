package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Random;

// This AI selects a random tile on the map, moves to it, and repeats these steps. If the player is facing another player with a clear line of sight, it shoots
public class AI extends AISuperClass {
    // This is the initial name for your tank when the match starts
    public static final String PLAYER_NAME = "AI";

    // Variables for this AI
    private Random random;
    private ArrayList<Node> path;
    int pathIndex;
    AStar pathFinder;
    LineOfSight lineOfSight;

    @Override
    public void init() {
        // Called when the countdown for a new match begins
        // At this point the players and the map have been received

        // Initialize variables
        random = new Random();
        pathFinder = new AStar();
        lineOfSight = new LineOfSight();
        path = new ArrayList<Node>();
        pathIndex = 0;
    }

    @Override
    public void update() {
        // To perform an action use the send() function with one of the following values:
        // TURN_LEFT - Turn your tank left
        // TURN_RIGHT - Turn your tank right
        // STOP_TURN - Stop your tank from turning
        // TURN_TOWARDS - Turns your tank towards a given angle. (Example: send(TURN_TOWARDS, 90.0) to make your tank approach a 90-degree angle)
        // MOVE_FORWARDS - Move your tank forwards
        // MOVE_BACKWARDS - Move your tank backwards
        // STOP_MOVE - Stop your tank from moving
        // SHOOT - Shoot a bullet (has a 1 second cooldown)
        // CHANGE_NAME - Changes your name. (Example: send(CHANGE_NAME, "name") to change your name to "name")

        // You can find other players with the otherPlayers ArrayList
        // Example (will loop through all other players):
        // for (Player otherPlayer : otherPlayers) {
        //      // Code there
        // }

        // You can find (all) bullets with the bullets ArrayList
        // Example (will loop through all bullets):
        // for (Bullet bullet : bullets) {
        //      // Code there
        //      if (bullet.getOwner() == player) {
        //          // Your bullet
        //      } else {
        //          // Not your bullet
        //      }
        // }
    	
    	// Loop through all other players
    	for (Player otherPlayer : otherPlayers) {
    		// Get the vector from your player to the other player and normalize it with .nor() (to make its length equal to 1). Then get the vector for your player's direction
    		Vector2 targetDirection = new Vector2(otherPlayer.getPosition()).sub(player.getPosition()).nor(),
        			playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
    		
    		// Get the dot product between these two vectors. The dot product between two (normalized) vectors is equal to cosine of the angle between them: a • b = cos(angle between a and b)
    		// The dot product will be 0 if the vectors are perpendicular and 1 if they are equal. So the more they are facing the same direction, the closer the value will be to 1
    		// If the dot product is greater than cos(10), it means the angle between the vectors is less than 10 degrees and that your player is facing the other player
    		float dot = targetDirection.dot(playerDirection);
    		if (dot > MathUtils.cosDeg(10)) {
    			// Shoot if there's an open line between your player's position and the other player's position
    			if (lineOfSight.check(player.getPosition(), otherPlayer.getPosition())) {
    				send(SHOOT);
    			}
    		}
    	}
        
        // Calculate path towards a random tile
        if (pathIndex == path.size()) {
        	// Find a random empty tile. random.nextInt(v) returns a value from 0 to v. Map.WIDTH and Map.HEIGHT is given in number of tiles
        	int x = 0, y = 0;
        	while (Map.isTile(x, y)) {x = random.nextInt(Map.WIDTH); y = random.nextInt(Map.HEIGHT);}
        	
        	// Calculate the path from the tank to the empty tile. Math.floor(x / Map.TILE_SIZEF) converts x from pixel coordinates to tile coordinates 
        	path = pathFinder.calculatePath(
        		(int) Math.floor(player.getPosition().x / Map.TILE_SIZEF),
        		(int) Math.floor(player.getPosition().y / Map.TILE_SIZEF),
        		x, y
        	);
        	
        	// pathIndex indicates the node our tank will approach in the path list
        	pathIndex = 0;
        }
    	
        // If the player has not reached the end of the path, keep moving
    	if (pathIndex < path.size()) {
    		// Get the position of the node our tank is currently approaching, and convert it from tile coordinates to pixel coordinates
    		Vector2 target = new Vector2((path.get(pathIndex).x + 0.5f) * Map.TILE_SIZEF, (path.get(pathIndex).y + 0.5f) * Map.TILE_SIZEF);
    		
    		Vector2 targetDirection = new Vector2(target).sub(player.getPosition()).nor(),
        			playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
    		
    		// If the angle between the target direction and the player direction is less than 60 degrees, move forwards, otherwise stop moving forward to avoid moving too far off the path
    		float dot = targetDirection.dot(playerDirection);
        	if (dot > MathUtils.cosDeg(60)) send(MOVE_FORWARDS); else send(STOP_MOVE);
        	
        	// Turn towards the target
        	send(TURN_TOWARDS, targetDirection.angle());
    		
        	// If the distance (.dst()) between the player and the target is small enough, start approaching the next node in the path
    		if (player.getPosition().dst(target) < Map.TILE_SIZEF * 0.5) ++pathIndex;
    	}
    }

    @Override
    public void tileRemoved(int x, int y) {
        // When the time has run out, tiles will be removed every 2 seconds to avoid long games
        // This method is automatically called whenever a tile is removed
        // x and y is the position of the tile removed
    }

    @Override
    public void matchEnded() {
        // Called when the match has ended
    }
}
