package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.Random;

// This AI selects a random point on the map, moves towards it
// for 1 second before choosing a new point while shooting.
public class AI extends AISuperClass {
    public static final String PLAYER_NAME = "AI";

    // Variables for this AI
    private Random random;
    private Vector2 target;
    private long prevTargetTime;

    public AI() {
        // Initialize variables
        random = new Random();
        target = null;
        prevTargetTime = 0;
    }

    @Override
    public void update() {
        // To perform an action use the send() function
        // with one of the following values:
        // Action.TURN_LEFT - Turn your tank left
        // Action.TURN_RIGHT - Turn your tank right
        // Action.STOP_TURN - Stop your tank turning
        // Action.MOVE_FORWARDS - Move your tank forwards
        // Action.MOVE_BACKWARDS - Move your tank backwards
        // Action.STOP_MOVE - Stop your tank moving
        // Action.SHOOT - Shoot a bullet (has a 1 second cooldown)
        // You can also use setName to change your name

        // You can find other players with the otherPlayers ArrayList
        // Example (will loop through all other players):
        // for(Player otherPlayer : otherPlayers) {
        //      // Code there
        // }

        // Set a random target every second
        if (target == null || (System.currentTimeMillis() - prevTargetTime) > 1000) {
            target = new Vector2(random.nextFloat() * Map.WIDTH * Map.TILE_SIZEF, random.nextFloat() * Map.HEIGHT * Map.TILE_SIZEF);
            prevTargetTime = System.currentTimeMillis();
        }

        // Calculate cross product
        Vector2 playerToTarget = new Vector2(target).sub(player.getPosition());
        Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
        float cross = playerToTarget.crs(playerDirection);

        // Rotate towards it
        if (cross < 0.0f) {
            send(Action.TURN_RIGHT);
        } else if (cross > 0.0f) {
            send(Action.TURN_LEFT);
        }

        // Move forwards and shoot
        send(Action.SHOOT);
        send(Action.MOVE_FORWARDS);
    }

    public void mapChanged() {
        // This function is called whenever the map changes
    }
}
