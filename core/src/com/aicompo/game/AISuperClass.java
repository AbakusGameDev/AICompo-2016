package com.aicompo.game;

import com.aicompo.ai.Bullet;
import com.aicompo.ai.Player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class AISuperClass {
    protected DataOutputStream outputStream;

    public Player player;
    public ArrayList<Player> otherPlayers;
    public ArrayList<Bullet> bullets;

    public final static int TURN_LEFT = 0;
    public final static int TURN_RIGHT = 1;
    public final static int STOP_TURN = 2;
    public final static int MOVE_FORWARDS = 3;
    public final static int MOVE_BACKWARDS = 4;
    public final static int STOP_MOVE = 5;
    public final static int SHOOT = 6;
    public final static int CHANGE_NAME = 7;
    public final static int TURN_TOWARDS = 8;

    public AISuperClass() {
        otherPlayers = new ArrayList<>();
        bullets = new ArrayList<>();
    }

    protected void send(int action, String data) {
        try {
            outputStream.writeBytes(action + "\n" + (!data.isEmpty() ? (data + "\n") : ""));
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void send(int action, float data) {
        send(action, "" + data);
    }

    protected void send(int action) {
        send(action, "");
    }

    public abstract void update();
    public abstract void init();
    public abstract void matchEnded();
    public abstract void tileRemoved(int x, int y);
}
