package com.aicompo.main;

import com.aicompo.ai.*;
import com.aicompo.ai.Bullet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class AISuperClass {
    public Player player;
    public ArrayList<Player> otherPlayers;

    public ArrayList<Player> bullets;

    public DataOutputStream outputStream;

    public enum Action {
        TURN_LEFT("TURN_LEFT"),
        TURN_RIGHT("TURN_RIGHT"),
        STOP_TURN("STOP_TURN"),
        MOVE_FORWARDS("MOVE_FORWARDS"),
        MOVE_BACKWARDS("MOVE_BACKWARDS"),
        STOP_MOVE("STOP_MOVE"),
        SHOOT("SHOOT");
        final private String text;
        Action(String text) {
            this.text = text;
        }
        public String getText() {
            return text;
        }
    }

    public AISuperClass() {
        otherPlayers = new ArrayList<>();
        bullets = new ArrayList<>();
    }

    protected void send(AI.Action action) {
        try {
            outputStream.writeBytes(action.getText() + "\n");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void setName(String name) {
        try {
            outputStream.writeBytes("NAME " + name + "\n");
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public abstract void mapReceived();
    public abstract void update();
}
