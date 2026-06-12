package model;

import enums.PlayerSkin;
import lombok.Getter;
import lombok.Setter;
import events.EventBus;
import events.GameEvent;

public class Player {
    private static final int MAX_HEALTH = 3;
    private static final long ENEMY_DAMAGE_COOLDOWN_NANOS = 1_000_000_000L;
    private static final long SEMI_INVISIBLE_IDLE_NANOS = 5_000_000_000L;
    private static final long SEMI_INVISIBLE_MOVE_GRACE_NANOS = 2_000_000_00L;

    //  Геттери (контракт)
    @Getter
    private int row;
    @Getter
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
    @Getter
    @Setter
    private PlayerSkin skin = PlayerSkin.CIRCLE;
    private boolean shieldActive = false;
    @Getter
    private boolean hasKey = false;
    private long lastEnemyDamageAtNanos = Long.MIN_VALUE;
    private long lastPositionChangeAtNanos = System.nanoTime();
    private long semiInvisibleGraceUntilNanos = Long.MIN_VALUE;

    public Player(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public boolean hasShield()       { return shieldActive; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
    public void setRow(int row) {
        setRow(row, System.nanoTime());
    }

    public void setCol(int col) {
        setCol(col, System.nanoTime());
    }

    void setRow(int row, long nowNanos) {
        if (this.row != row) {
            boolean wasSemiInvisible = isSemiInvisible(nowNanos);
            this.row = row;
            onPositionChanged(nowNanos, wasSemiInvisible);
        }
    }

    void setCol(int col, long nowNanos) {
        if (this.col != col) {
            boolean wasSemiInvisible = isSemiInvisible(nowNanos);
            this.col = col;
            onPositionChanged(nowNanos, wasSemiInvisible);
        }
    }

    public boolean isSemiInvisible() {
        return isSemiInvisible(System.nanoTime());
    }

    //adding artifacts
    public void addCrystals(int amount)    { crystals += amount; }
    public void addRadarCharge(int amount) { radarCharges += amount; }
    public void addShield(int amount)      { shieldCount += amount; }
    public void addBeacon(int amount)      { beaconCount += amount; }
    public void addElixir(int amount)      { elixirCount += amount; }
    public void addKey()                 { hasKey = true; }


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
        takeDamage(amount, System.nanoTime());
    }

    void takeDamage(int amount, long nowNanos) {
        if (isSemiInvisible(nowNanos)) {
            return;
        }
        if (shieldActive) {
            // Щит поглинає 1 удар і ламається
            shieldActive = false;
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.SHIELD_BROKEN));
            return;
        }
        health = Math.max(0, health - amount);
        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.PLAYER_DAMAGED));
        if (health == 0)
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.PLAYER_DIED));
    }

    public void takeEnemyDamage(int amount) {
        takeEnemyDamage(amount, System.nanoTime());
    }

    boolean takeEnemyDamage(int amount, long nowNanos) {
        if (isSemiInvisible(nowNanos)) {
            return false;
        }
        if (lastEnemyDamageAtNanos != Long.MIN_VALUE
                && nowNanos - lastEnemyDamageAtNanos < ENEMY_DAMAGE_COOLDOWN_NANOS) {
            return false;
        }

        lastEnemyDamageAtNanos = nowNanos;
        takeDamage(amount, nowNanos);
        return true;
    }

    boolean isSemiInvisible(long nowNanos) {
        return nowNanos - lastPositionChangeAtNanos >= SEMI_INVISIBLE_IDLE_NANOS
                || nowNanos < semiInvisibleGraceUntilNanos;
    }

    public void heal(int amount) {
        health = Math.min(MAX_HEALTH, health + amount);
    }

    private void onPositionChanged(long nowNanos, boolean wasSemiInvisible) {
        if (wasSemiInvisible) {
            semiInvisibleGraceUntilNanos = nowNanos + SEMI_INVISIBLE_MOVE_GRACE_NANOS;
        }
        lastPositionChangeAtNanos = nowNanos;
    }


}
