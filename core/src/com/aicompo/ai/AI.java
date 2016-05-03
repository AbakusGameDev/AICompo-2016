package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// This AI selects a random tile on the map, moves to it, and repeats these steps. If the player is facing another player with a clear line of sight, it shoots
public class AI extends AISuperClass {
    // This is the initial name for your tank when the match starts
    public static final String PLAYER_NAME = "BitsauceAI";

    // Variables for this AI
    private Random random;
    private ArrayList<Node> path;
    int pathIndex;
    AStar pathFinder;
    LineOfSight lineOfSight;
    long lastTickTime;
    long sampleDataTimer;
    long updatePathTimer;
    Player targetPlayer;

    class PlayerMetaData {
        PlayerMetaData(PlayerMetaData prevData, Vector2 position, long time) {
            this.position = position;
            this.time = time;

            if(prevData != null) {
                direction = prevData.position.cpy().sub(position).nor();
                distance =  prevData.position.cpy().sub(position).len();
                speed = (1000.0f * distance) / (time - prevData.time);
                velocity = direction.cpy().scl(speed);

                System.out.println("Dt: "+(time-prevData.time));
            } else {
                direction = new Vector2(0.0f, 0.0f);
                distance = 0;
                speed = 0;
                velocity = new Vector2(0.0f, 0.0f);
            }
        }

        public final Vector2 position;
        public final Vector2 velocity;
        public final Vector2 direction;
        public final long time;
        public final float distance;
        public final float speed;
    }

    HashMap<Player, LinkedList<PlayerMetaData>> playerPositions;

    enum State {
        PATH_FINDING,
        AIMING,
        CHASING
    }

    private State state;

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
        state = State.PATH_FINDING;

        playerPositions = new HashMap<>();
        for(Player player : otherPlayers) {
            playerPositions.put(player, new LinkedList<>());
        }
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

        final long tickTime = System.currentTimeMillis();

        sampleDataTimer += tickTime - lastTickTime;
        if(sampleDataTimer > 0) {
            for (Player player : otherPlayers) {
                LinkedList<PlayerMetaData> metaDataArray = playerPositions.get(player);
                PlayerMetaData data = new PlayerMetaData(metaDataArray.isEmpty() ? null : metaDataArray.getFirst(), player.getPosition(), tickTime);
                metaDataArray.addFirst(data);
                if(metaDataArray.size() > 20) {
                    metaDataArray.removeLast();
                }
            }
            sampleDataTimer = 0;
        }

        switch (state) {
            case PATH_FINDING: {
                // Update path every 500 ms
                updatePathTimer += tickTime - lastTickTime;
                if(updatePathTimer > 500) {
                    // Find closest player
                    targetPlayer = otherPlayers.get(0);
                    for (int i = 1; i < otherPlayers.size(); i++) {
                        if(player.getPosition().cpy().sub(targetPlayer.getPosition()).len2() > player.getPosition().cpy().sub(otherPlayers.get(i).getPosition()).len2()) {
                            targetPlayer = otherPlayers.get(i);
                        }
                    }

                    // Calculate the path from the tank to the empty tile. Math.floor(x / Map.TILE_SIZEF) converts x from pixel coordinates to tile coordinates
                    path = pathFinder.calculatePath(
                            (int) Math.floor(player.getPosition().x / Map.TILE_SIZEF),
                            (int) Math.floor(player.getPosition().y / Map.TILE_SIZEF),
                            (int) Math.floor(targetPlayer.getPosition().x / Map.TILE_SIZEF),
                            (int) Math.floor(targetPlayer.getPosition().y / Map.TILE_SIZEF)
                    );

                    // pathIndex indicates the node our tank will approach in the path list
                    pathIndex = 0;
                    send(CHANGE_NAME, "Pathfinding to: " + targetPlayer.getName());
                    updatePathTimer = 0;
                }

                if(lineOfSight.check(player.getPosition(), targetPlayer.getPosition())) {
                    float avgSpeed = 0.0f;
                    for(PlayerMetaData data : playerPositions.get(targetPlayer)) {
                        avgSpeed += data.speed;
                    }
                    avgSpeed /= (float) playerPositions.get(targetPlayer).size();

                    if(avgSpeed > 60.0) {
                        state = State.CHASING;
                    }
                    else {
                        state = State.AIMING;
                    }

                } else if (pathIndex < path.size()) {
                    // Get the position of the node our tank is currently approaching, and convert it from tile coordinates to pixel coordinates
                    Vector2 target = new Vector2((path.get(pathIndex).x + 0.5f) * Map.TILE_SIZEF, (path.get(pathIndex).y + 0.5f) * Map.TILE_SIZEF);

                    Vector2 targetDirection = new Vector2(target).sub(player.getPosition()).nor();
                    Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));

                    // If the angle between the target direction and the player direction is less than 60 degrees, move forwards, otherwise stop moving forward to avoid moving too far off the path
                    float dot = targetDirection.dot(playerDirection);
                    if (dot > MathUtils.cosDeg(60)) send(MOVE_FORWARDS); else send(STOP_MOVE);

                    // Turn towards the target
                    send(TURN_TOWARDS, targetDirection.angle());

                    // If the distance (.dst()) between the player and the target is small enough, start approaching the next node in the path
                    if (player.getPosition().dst(target) < Map.TILE_SIZEF * 0.5) ++pathIndex;
                }
            }
            break;

            case AIMING: {
                // Aim at the average position
                send(CHANGE_NAME, "Aiming at: " + targetPlayer.getName());
            }
            //break;

            case CHASING: {
                send(CHANGE_NAME, "Chasing: " + targetPlayer.getName());

                if(!lineOfSight.check(player.getPosition(), targetPlayer.getPosition())) {
                    state = State.PATH_FINDING;
                    break;
                }

                PlayerMetaData data = playerPositions.get(targetPlayer).getFirst();
                Vector2 target = getShootAtPoint(data.velocity, data.position);
                if(target != null)
                {
                    Vector2 targetDirection = target.cpy().sub(player.getPosition()).nor();
                    Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));

                    // If the angle between the target direction and the player direction is less than 60 degrees, move forwards, otherwise stop moving forward to avoid moving too far off the path
                    float dot = targetDirection.dot(playerDirection);
                    if (dot > MathUtils.cosDeg(60)) {
                        //send(MOVE_FORWARDS);
                    } else {
                        send(STOP_MOVE);
                    }

                    if (dot > MathUtils.cosDeg(2)) {
                        send(SHOOT);
                    }

                    // Turn towards the target
                    send(TURN_TOWARDS, targetDirection.angle());
                } else {
                    state = State.PATH_FINDING;
                }
            }
            break;
        }

        lastTickTime = tickTime;
    }

    Vector2 getShootAtPoint(Vector2 targetVelocity, Vector2 targetPosition) {
        Vector2 position = player.getPosition();

        float a = targetVelocity.x * targetVelocity.x + targetVelocity.y * targetVelocity.y - Bullet.SPEED * Bullet.SPEED;
        float b = 2 * (targetVelocity.x * (targetPosition.x - position.x) + targetVelocity.y * (targetPosition.y - position.y));
        float c = ((targetPosition.x - position.x) * (targetPosition.x - position.x)) + ((targetPosition.y - position.y) * (targetPosition.y - position.y));

        float disc = b * b - 4 * a * c;
        if(disc < 0.0f) {
            return null;
        }

        float t1 = (-b + (float) Math.sqrt(disc)) / (2 * a);
        float t2 = (-b - (float) Math.sqrt(disc)) / (2 * a);

        return targetPosition.cpy().add(targetVelocity.cpy().scl(Math.min(t1, t2)));
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
