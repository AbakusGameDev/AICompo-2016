package com.aicompo.game;

import com.aicompo.ai.Bullet;
import com.aicompo.ai.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class AISuperClass {
    public Player player;
    public ArrayList<Player> otherPlayers;
    public ArrayList<Bullet> bullets;
    public DataOutputStream outputStream;

    public final static int TURN_LEFT = 0;
    public final static int TURN_RIGHT = 1;
    public final static int STOP_TURN = 2;
    public final static int MOVE_FORWARDS = 3;
    public final static int MOVE_BACKWARDS = 4;
    public final static int STOP_MOVE = 5;
    public final static int SHOOT = 6;
    public final static int NAME = 7;

    public AISuperClass() {
        otherPlayers = new ArrayList<>();
        bullets = new ArrayList<>();
    }

    protected void send(int action) {
        try {
            outputStream.writeBytes(action + "\n");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setName(String name) {
        try {
            outputStream.writeBytes(NAME + "\n" + name + "\n");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void mapChanged();
    public abstract void update();
}
