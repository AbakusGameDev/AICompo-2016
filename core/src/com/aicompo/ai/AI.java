package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

// This AI selects a random tile on the map, moves to it, and repeats these steps. If the player is facing another player with a clear line of sight, it shoots
public class AI extends AISuperClass {
    // This is the initial name for your tank when the match starts
    public static final String PLAYER_NAME = "BitsauceAI";

    // Variables for this AI
    private ArrayList<Node> path;
    int pathIndex;
    AStar pathFinder;
    LineOfSight lineOfSight;
    long lastTickTime;
    long updatePathTimer;
    long sampleDataTimer;
    Player targetPlayer;
    Bullet prevClosestBullet;

    int dodgeTurnDirection;

    class PlayerMetaData {
        PlayerMetaData(PlayerMetaData prevData, Vector2 position, long time) {
            this.position = position;
            this.time = time;

            if(prevData != null) {
                direction = prevData.position.cpy().sub(position).nor();
                distance =  prevData.position.cpy().sub(position).len();
                speed = (1000.0f * distance) / (time - prevData.time);
                velocity = direction.cpy().scl(speed);
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

    HashMap<Player, LinkedList<PlayerMetaData>> playerMetaDataMap;

    enum State {
        PATH_FINDING,
        AIMING,
        CHASING,
        DODGING
    }

    private State state;

    @Override
    public void init() {
        // Called when the countdown for a new match begins
        // At this point the players and the map have been received

        // Initialize variables
        pathFinder = new AStar();
        lineOfSight = new LineOfSight();
        path = new ArrayList<Node>();
        pathIndex = 0;
        state = State.PATH_FINDING;

        playerMetaDataMap = new HashMap<>();
        for(Player player : otherPlayers) {
            playerMetaDataMap.put(player, new LinkedList<>());
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
        if(sampleDataTimer > 50) {
            for (Player player : otherPlayers) {
                LinkedList<PlayerMetaData> metaDataArray = playerMetaDataMap.get(player);
                PlayerMetaData data = new PlayerMetaData(metaDataArray.isEmpty() ? null : metaDataArray.getFirst(), player.getPosition(), tickTime);
                metaDataArray.addFirst(data);
                if (metaDataArray.size() > 5) {
                    metaDataArray.removeLast();
                }
            }
        }

        // Dodging
        for(Bullet bullet : bullets) {
            if(bullet.getOwner() != player) {
                Vector2 bulletToPlayer = new Vector2(player.getPosition()).sub(bullet.getPosition());
                Vector2 targetDirection = bulletToPlayer.cpy().nor();
                Vector2 bulletDirection = new Vector2(MathUtils.cosDeg(bullet.getAngle()), MathUtils.sinDeg(bullet.getAngle()));
                float dot = targetDirection.dot(bulletDirection);
                if(dot > MathUtils.cosDeg(Math.max((1.0f - (bulletToPlayer.len() / 500.0f)) * 180.0f, 20.0f)) &&
                        (lineOfSight.check(player.getPosition().cpy().add(Player.SIZE * 0.5f, Player.SIZE * 0.5f), bullet.getPosition()) ||
                        lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f,  Player.SIZE * 0.5f), bullet.getPosition()) ||
                        lineOfSight.check(player.getPosition().cpy().add( Player.SIZE * 0.5f, -Player.SIZE * 0.5f), bullet.getPosition()) ||
                        lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f, -Player.SIZE * 0.5f), bullet.getPosition()))) {
                    state = State.DODGING;
                    break;
                }
            }
        }

        switch (state) {
            case DODGING: {
                if(bullets.size() > 0) {
                    // Get closes bullet heading in our direction
                    Bullet closestBullet = null;
                    for (Bullet bullet : bullets) {
                        if (bullet.getOwner() != player && (closestBullet == null || player.getPosition().cpy().sub(bullet.getPosition()).len2() < player.getPosition().cpy().sub(closestBullet.getPosition()).len2())) {
                            Vector2 bulletToPlayer = new Vector2(player.getPosition()).sub(bullet.getPosition());
                            Vector2 targetDirection = bulletToPlayer.cpy().nor();
                            Vector2 bulletDirection = new Vector2(MathUtils.cosDeg(bullet.getAngle()), MathUtils.sinDeg(bullet.getAngle()));
                            float dot = targetDirection.dot(bulletDirection);
                            if (dot > MathUtils.cosDeg(Math.max((1.0f - (bulletToPlayer.len() / 500.0f)) * 180.0f, 20.0f)) &&
                                    (lineOfSight.check(player.getPosition().cpy().add(Player.SIZE * 0.5f, Player.SIZE * 0.5f), bullet.getPosition()) ||
                                    lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f, Player.SIZE * 0.5f), bullet.getPosition()) ||
                                    lineOfSight.check(player.getPosition().cpy().add(Player.SIZE * 0.5f, -Player.SIZE * 0.5f), bullet.getPosition()) ||
                                    lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f, -Player.SIZE * 0.5f), bullet.getPosition()))) {
                                closestBullet = bullet;
                            }
                        }
                    }

                    if(closestBullet != null) {
                        Vector2 targetDirection = player.getPosition().cpy().sub(closestBullet.getPosition()).nor();
                        Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
                        Vector2 bulletDirection = new Vector2(MathUtils.cosDeg(closestBullet.getAngle()), MathUtils.sinDeg(closestBullet.getAngle()));

                        if(prevClosestBullet != closestBullet) {
                            int dodgeMoveDir = 0;
                            if (bulletDirection.dot(targetDirection) > 0) {
                                if (playerDirection.dot(bulletDirection) < 0.1f) {
                                    dodgeMoveDir = -1;
                                } else if (playerDirection.dot(bulletDirection) > -0.1f) {
                                    dodgeMoveDir = 1;
                                }
                            }

                            Vector2 toPos;

                            if(playerDirection.crs(bulletDirection) > 0) {
                                if(dodgeMoveDir == -1) {
                                    toPos = player.getPosition().cpy().sub(new Vector2(MathUtils.cosDeg(closestBullet.getAngle() - 45), MathUtils.sinDeg(closestBullet.getAngle() - 45)).scl(50));
                                }
                                else {
                                    toPos = player.getPosition().cpy().add(new Vector2(MathUtils.cosDeg(closestBullet.getAngle() - 45), MathUtils.sinDeg(closestBullet.getAngle() - 45)).scl(50));
                                }
                            }
                            else {
                                if(dodgeMoveDir == -1) {
                                    toPos = player.getPosition().cpy().sub(new Vector2(MathUtils.cosDeg(closestBullet.getAngle() + 45), MathUtils.sinDeg(closestBullet.getAngle() + 45)).scl(50));
                                }
                                else {
                                    toPos = player.getPosition().cpy().add(new Vector2(MathUtils.cosDeg(closestBullet.getAngle() + 45), MathUtils.sinDeg(closestBullet.getAngle() + 45)).scl(50));
                                }
                            }

                            if (playerDirection.crs(bulletDirection) > 0 && lineOfSight.check(player.getPosition().cpy(), toPos)) {
                                dodgeTurnDirection = -1;
                            } else {
                                dodgeTurnDirection = 1;
                            }
                            prevClosestBullet = closestBullet;
                        }

                        if (dodgeTurnDirection == -1) {
                            send(TURN_TOWARDS, bulletDirection.angle() - 90);
                        } else if(dodgeTurnDirection == 1) {
                            send(TURN_TOWARDS, bulletDirection.angle() + 90);
                        }

                        if (bulletDirection.dot(targetDirection) > 0) {
                            if (playerDirection.dot(bulletDirection) < 0.1f) {
                                send(MOVE_BACKWARDS);
                            } else if (playerDirection.dot(bulletDirection) > -0.1f) {
                                send(MOVE_FORWARDS);
                            }
                        }
                    } else {
                        state = State.PATH_FINDING;
                    }
                } else {
                    state = State.PATH_FINDING;
                }
            }
            break;

            case PATH_FINDING: {
                // Update path every 500 ms
                updatePathTimer += tickTime - lastTickTime;
                if(targetPlayer == null || !targetPlayer.isAlive() || updatePathTimer > 500) {
                    // Find closest player
                    targetPlayer = null;
                    for (Player otherPlayer : otherPlayers) {
                        if(!otherPlayer.isAlive()) continue;
                        if(targetPlayer == null || player.getPosition().cpy().sub(targetPlayer.getPosition()).len2() > player.getPosition().cpy().sub(otherPlayer.getPosition()).len2()) {
                            targetPlayer = otherPlayer;
                        }
                    }

                    // Calculate the path
                    path = pathFinder.calculatePath(
                            (int) Math.floor(player.getPosition().x / Map.TILE_SIZEF),
                            (int) Math.floor(player.getPosition().y / Map.TILE_SIZEF),
                            (int) Math.floor(targetPlayer.getPosition().x / Map.TILE_SIZEF),
                            (int) Math.floor(targetPlayer.getPosition().y / Map.TILE_SIZEF)
                    );

                    // pathIndex indicates the node our tank will approach in the path list
                    pathIndex = 0;
                    updatePathTimer = 0;
                }

                if(lineOfSight.check(player.getPosition(), targetPlayer.getPosition())) {
                    float avgSpeed = 0.0f;
                    for(PlayerMetaData data : playerMetaDataMap.get(targetPlayer)) {
                        avgSpeed += data.speed;
                    }
                    avgSpeed /= (float) playerMetaDataMap.get(targetPlayer).size();

                    if(avgSpeed > 105.0) {
                        if(lineOfSight.check(player.getPosition().cpy().add(Player.SIZE * 0.5f, Player.SIZE * 0.5f), targetPlayer.getPosition().cpy().add(Player.SIZE * 0.5f, Player.SIZE * 0.5f)) &&
                                lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f,  Player.SIZE * 0.5f), targetPlayer.getPosition().cpy().add(-Player.SIZE * 0.5f,  Player.SIZE * 0.5f)) &&
                                lineOfSight.check(player.getPosition().cpy().add( Player.SIZE * 0.5f, -Player.SIZE * 0.5f), targetPlayer.getPosition().cpy().add( Player.SIZE * 0.5f, -Player.SIZE * 0.5f)) &&
                                lineOfSight.check(player.getPosition().cpy().add(-Player.SIZE * 0.5f, -Player.SIZE * 0.5f), targetPlayer.getPosition().cpy().add(-Player.SIZE * 0.5f, -Player.SIZE * 0.5f))) {
                            state = State.CHASING;
                        }
                    }
                    else {
                        state = State.AIMING;
                    }
                }

                if (pathIndex < path.size()) {
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
                // Make sure there is still line of sight
                if (targetPlayer.isAlive() && lineOfSight.check(player.getPosition(), targetPlayer.getPosition())) {
                    Vector2 avgVel = new Vector2(0.0f, 0.0f);
                    Vector2 avgPos = new Vector2(0.0f, 0.0f);
                    for(PlayerMetaData data : playerMetaDataMap.get(targetPlayer)) {
                        avgVel.add(data.velocity);
                        avgPos.add(data.position);
                    }
                    avgVel.scl(1.0f / playerMetaDataMap.get(targetPlayer).size());
                    avgPos.scl(1.0f / playerMetaDataMap.get(targetPlayer).size());

                    Vector2 target = getShootAtPoint(avgVel, avgPos);
                    if (target != null) {
                        Vector2 targetDirection = target.cpy().sub(player.getPosition()).nor();
                        Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));

                        // If the angle between the target direction and the player direction is less than 60 degrees, move forwards, otherwise stop moving forward to avoid moving too far off the path
                        float dot = targetDirection.dot(playerDirection);
                        if (dot > MathUtils.cosDeg(2)) {
                            send(SHOOT);
                        }

                        // Turn towards the target
                        send(TURN_TOWARDS, targetDirection.angle());
                        send(STOP_MOVE);
                    } else {
                        state = State.PATH_FINDING;
                    }
                } else {
                    state = State.PATH_FINDING;
                }
            }
            break;

            case CHASING: {
                // Make sure there is still line of sight
                if(targetPlayer.isAlive() && lineOfSight.check(player.getPosition(), targetPlayer.getPosition())) {
                    // Get point to shoot at
                    PlayerMetaData data = playerMetaDataMap.get(targetPlayer).getFirst();
                    Vector2 target = getShootAtPoint(data.velocity, data.position);

                    // target == null if bullet cant reach in time
                    if (target != null) {
                        // Get vectors
                        Vector2 targetDirection = target.cpy().sub(player.getPosition()).nor();
                        Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));

                        // If the angle between the target direction and the player direction is less than 60 degrees, move forwards, otherwise stop moving forward to avoid moving too far off the path
                        float dot = targetDirection.dot(playerDirection);
                        if (dot > MathUtils.cosDeg(60) && player.getPosition().cpy().sub(targetPlayer.getPosition()).len() > 300) {
                            send(MOVE_FORWARDS);
                        } else {
                            send(STOP_MOVE);
                        }

                        // Shoot if aiming at target
                        if (dot > MathUtils.cosDeg(2)) {
                            send(SHOOT);
                        }

                        // Turn towards the target
                        send(TURN_TOWARDS, targetDirection.angle());
                    } else {
                        state = State.PATH_FINDING;
                    }
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
