package model;

import lombok.Getter;
import lombok.Setter;
import events.EventBus;
import events.GameEvent;

public class Player {
    private static final int MAX_HEALTH = 3;

    //  Геттери (контракт)
    @Getter
    @Setter
    private int row;
    @Getter
    @Setter
    private int col;
    @Getter
    private int health = MAX_HEALTH;
    @Getter
    private int crystals = 0;
    @Getter
    private int radarCharges = 0;
    @Getter
    private int beaconCount = 0;
    @Getter
    private int shieldCount = 0;
    @Getter
    private int elixirCount = 0;
    private boolean shieldActive = false;

    public Player(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean hasShield()       { return shieldActive; }

    //adding artifacts
    public void addCrystals(int amount)    { crystals += amount; }
    public void addRadarCharge(int amount) { radarCharges += amount; }
    public void addShield(int amount)      { shieldCount += amount; }
    public void addBeacon(int amount)      { beaconCount += amount; }
    public void addElixir(int amount)      { elixirCount += amount; }


    //using artifacts
    public void activateShield()           { shieldActive = true; }
    public void deactivateShield()         { shieldActive = false; }
    public void useRadarCharge()           { if (radarCharges > 0) radarCharges--; }
    public void useBeacon()                { if (beaconCount > 0) beaconCount--; }
    public void useElixir()                { if (elixirCount > 0) elixirCount--; }
    public boolean useCrystal()              {
        if (crystals > 0) {
            crystals--;
            return true;
        }
        return false;
    }

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
