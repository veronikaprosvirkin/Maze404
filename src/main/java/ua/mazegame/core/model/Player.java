package ua.mazegame.core.model;

import ua.mazegame.core.events.EventBus;
import ua.mazegame.core.events.GameEvent;

public class Player {
    private static final int MAX_HEALTH = 3;

    private int row;
    private int col;
    private int health = MAX_HEALTH;
    private int crystals = 0;
    private int radarCharges = 0;
    private int beaconCount = 0;
    private boolean shieldActive = false;

    public Player(int row, int col) {
        this.row = row;
        this.col = col;
    }

    //  Геттери (контракт)
    public int     getRow()          { return row; }
    public int     getCol()          { return col; }
    public int     getHealth()       { return health; }
    public int     getCrystals()     { return crystals; }
    public int     getRadarCharges() { return radarCharges; }
    public int     getBeaconCount()  { return beaconCount; }
    public boolean hasShield()       { return shieldActive; }

    public void setRow(int row) { this.row = row; }
    public void setCol(int col) { this.col = col; }

    public void addCrystals(int amount)    { crystals += amount; }
    public void activateShield()           { shieldActive = true; }
    public void deactivateShield()         { shieldActive = false; }
    public void addRadarCharge(int amount) { radarCharges += amount; }
    public void useRadarCharge()           { if (radarCharges > 0) radarCharges--; }
    public void addBeacon(int amount)      { beaconCount += amount; }
    public void useBeacon()                { if (beaconCount > 0) beaconCount--; }

    public void takeDamage(int amount) {
        if (shieldActive) {
            // Щит поглинає 1 удар і ламається
            shieldActive = false;
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.SHIELD_BROKEN));
            return;
        }
        health = Math.max(0, health - amount);
        if (health == 0)
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.PLAYER_DIED));
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }
}
