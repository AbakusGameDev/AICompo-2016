package com.aicompo.ai;

import com.aicompo.game.AISuperClass;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Random;

// This AI selects a random tile on the map, moves to it, and repeats these steps. If the player is facing another player with a clear line of sight, it shoots
public class AI extends AISuperClass {
    // This is the initial name for your tank when the match starts
    public static final String PLAYER_NAME = "JAB_2.0";

    // Variables for this AI
    private Random random;
    private ArrayList<Node> path;
    private int progress;
    AStar pathFinder;
    LineOfSight lineOfSight;
    private Vector2 target;
    private boolean findNewPath;
    private Vector2 targetLastPos;
    private long dangerTime;
    private float dodgeAngel;

    @Override
    public void init() {
        // Called when the countdown for a new match begins
        // At this point the players and the map have been received

        // Initialize variables
        random = new Random();
        pathFinder = new AStar();
        lineOfSight = new LineOfSight();
        path = new ArrayList<Node>();
        progress = 0;
        findNewPath = true;
        dangerTime = System.currentTimeMillis();
        dodgeAngel = 0.0f;
    }
    
    private boolean isDodgePathObscoured(float tryAngle, float bulletAngle) {
		dodgeAngel = bulletAngle + tryAngle;
		Vector2 rot = new Vector2(MathUtils.cosDeg(dodgeAngel), MathUtils.sinDeg(dodgeAngel));
		target = player.getPosition().cpy().add(rot.scl(Map.TILE_SIZEF));
		return Map.isTile((int)Math.floor(target.x / Map.TILE_SIZEF), (int)Math.floor(target.y / Map.TILE_SIZEF));
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

    	Vector2 playerDirection = new Vector2(MathUtils.cosDeg(player.getAngle()), MathUtils.sinDeg(player.getAngle()));
    	
    	float lowestDist = 9999;
    	Player nearestPlayer = null;
    	for(Player otherPlayer : otherPlayers) {
    		float dist = player.getPosition().dst(otherPlayer.getPosition());
    		if (otherPlayer.isAlive() && dist < lowestDist) {
    			nearestPlayer = otherPlayer;
    			lowestDist = dist;
    		}
    	}
    	target = nearestPlayer.getPosition();
    	
    	// BULLET DODGING
    	//setName("JAB_2.0");
    	boolean isDanger = false;
    	for (Bullet bullet : bullets) {
    		bullet.getPosition().cpy();
    		Vector2 bulletToPlayer = (player.getPosition().cpy().sub(bullet.getPosition())).nor();
            Vector2 bulletDirection = new Vector2(MathUtils.cosDeg(bullet.getAngle()), MathUtils.sinDeg(bullet.getAngle()));
            float playerBulletDot = bulletToPlayer.dot(bulletDirection);
            if (MathUtils.cosDeg(10) < playerBulletDot) {
            	// TRY DODGING IF IN DANGER
            	if (lineOfSight.check(bullet.getPosition(), player.getPosition())) {
            		float playerBulletCross = bulletToPlayer.crs(bulletDirection);
            		//setName("DANGER!");
            		isDanger = true;
            		float angle = 45.0f;
            		if (playerBulletCross < 0.0f) angle *= -1.0f;
            		// FIND TARGET
            		for (int i = 0; i < 8; i++) {
            			if (isDodgePathObscoured(angle, bullet.getAngle())) {
            				angle -= 22.0f;
            				//send(CHANGE_NAME, "PATH OBSCOURED");
            				System.out.println("PATH OBSCOURED");
            			} else {
            				//send(CHANGE_NAME, "FOUND SOLUTION");
            				System.out.println("FOUND SOLUTION");
            				break;
            			}
            		}
            	}
            }
    	}
    	Vector2 playerToTarget = (target.cpy().sub(player.getPosition())).nor();
        float targetPlayerDot = playerToTarget.dot(playerDirection);
        // IN DANGER
    	if (isDanger) {
    		dangerTime = System.currentTimeMillis();
    		//setName("DANGER!");
    		// Back or forth
            if (targetPlayerDot < 0) {
            	send(MOVE_FORWARDS);
            	send(TURN_TOWARDS, dodgeAngel+180.0f);
            } else {
            	send(MOVE_BACKWARDS);
            	send(TURN_TOWARDS, dodgeAngel);            	
            }
            return;
    	}
    	if (System.currentTimeMillis() - dangerTime < 250) {
    		return;
    	}
    	// FIND NEW PATH
    	if (findNewPath) {
    		progress = 0;
    		path = pathFinder.calculatePath(player.getPosition(), target);
    		targetLastPos = target.cpy();
    		findNewPath = false;
    		//setName("New path acquired!");
    	}
    	// TARGET IN LINE OF SIGHT
    	boolean los = lineOfSight.check(player.getPosition(), target);
    	if (progress < path.size()) {
    		if (!target.equals(targetLastPos)) {
    			findNewPath = true;
    			//setName("I need a new Path!");
    		}
    		if (!los) {
    			target = new Vector2(
        				(path.get(progress).x + 0.5f) * Map.TILE_SIZEF,
        				(path.get(progress).y + 0.5f) * Map.TILE_SIZEF);
        		if (target.dst(player.getPosition()) < Map.TILE_SIZEF * 0.5f) progress++;
    		}
            playerToTarget = (new Vector2(target).sub(player.getPosition())).nor();
            
            send(TURN_TOWARDS, playerToTarget.angle());

            // Move forwards and shoot
            //send(SHOOT);
            if (!los) {
            	send(MOVE_FORWARDS);
            } else {
            	send(STOP_MOVE);
            }
    	}
    	
    	if (los) {
    		playerToTarget = (new Vector2(target).sub(player.getPosition())).nor();
            float cross = playerToTarget.crs(playerDirection);
            if (cross < 0.5f && cross > -0.5f) {
            	//send(SHOOT);
            }
        	//setName("LOS!");
        }
    }

    @Override
    public void tileRemoved(int x, int y) {
        // When the time has run out, tiles will be removed every 2 seconds to avoid long games
        // This method is automatically called whenever a tile is removed
        // x and y is the position of the tile removed
    	findNewPath = true;
    	progress = 0;
    }

    @Override
    public void matchEnded() {
        // Called when the match has ended
    	findNewPath = true;
    	progress = 0;
    }
}
