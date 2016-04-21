package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.awt.*;
import java.util.ArrayList;
import java.util.Random;

// This AI selects a random point on the map, moves towards it for 1 second before choosing a new point while shooting
public class AI extends AISuperClass {
    // This is the initial name for your tank when the match starts
    public static final String PLAYER_NAME = "AI";

    // Variables for this AI
    private Random random;
    private Vector2 target;
    private long prevTargetTime;
    private AStar astar;

    @Override
    public void init() {
        // Called when the countdown for a new match begins
        // At this point the players and the map have been received

        // Initialize variables
        random = new Random();
        target = null;
        prevTargetTime = 0;
        astar = new AStar();
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
        // for(Player otherPlayer : otherPlayers) {
        //      // Code there
        // }

        // You can find (all) bullets with the bullets ArrayList
        // Example (will loop through all bullets):
        // for(Bullet bullet : bullets) {
        //      // Code there
        //      if(bullet.getOwner() == player) {
        //          // Your bullet
        //      }
        //      else {
        //          // Not your bullet
        //      }
        // }

        // Set a random target every second
        if (target == null || (System.currentTimeMillis() - prevTargetTime) > 1000) {
            // random.nextInt returns a value from 0 to the given number. Map.TILE_SIZE is the size of a tile (in pixels). Map.WIDTH and Map.HEIGHT is given in # tiles
            ArrayList<Point> path = astar.getPath(player.getPosition(), otherPlayers.get(0).getPosition());

            if(!path.isEmpty()) {
                target = new Vector2((path.get(0).x+0.5f) * Map.TILE_SIZEF, (path.get(0).y+0.5f) * Map.TILE_SIZEF);//new Vector2(random.nextInt(Map.WIDTH * Map.TILE_SIZE), random.nextInt(Map.HEIGHT * Map.TILE_SIZE));
                prevTargetTime = System.currentTimeMillis();
            }
        }

        // Calculate cross product
        Vector2 playerToTarget = new Vector2(target).sub(player.getPosition());
        Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
        float cross = playerToTarget.crs(playerDirection);

        // Rotate towards the target position
        if (cross < 0.0f) {
            send(TURN_RIGHT);
        } else if (cross > 0.0f) {
            send(TURN_LEFT);
        }

        // Alternate method. More precise and avoids jittering
        //send(TURN_TOWARDS, playerToTarget.angle());

        // Move forwards and shoot
        send(MOVE_FORWARDS);
        send(SHOOT);
    }

    @Override
    public void tileRemoved(int x, int y) {
        // When the time has run out, tiles will be removed every 2 seconds to avoid long games
        // This function is automatically called whenever a tile is removed
        // x and y is the position of the tile removed
    }

    @Override
    public void matchEnded() {
        // Called when the match has ended
    }
}
