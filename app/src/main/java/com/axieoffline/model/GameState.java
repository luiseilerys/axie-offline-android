package com.axieoffline.model;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    public enum Screen {
        HOME, COLLECTION, BATTLE_SETUP, BATTLE, BREED, DETAIL
    }

    public Screen screen = Screen.HOME;
    public PlayerData player = new PlayerData();
    public BattleSystem battle = new BattleSystem();

    public List<Creature> selectedTeam = new ArrayList<>();
    public Creature detailCreature;
    public Creature breedA;
    public Creature breedB;
    public String message = "";
    public long messageUntil = 0;

    public void showMessage(String msg, long durationMs) {
        message = msg;
        messageUntil = System.currentTimeMillis() + durationMs;
    }

    public boolean hasMessage() {
        return System.currentTimeMillis() < messageUntil && message != null && !message.isEmpty();
    }
}
