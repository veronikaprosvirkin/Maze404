package ua.mazegame.core.events;

import ua.mazegame.core.model.Position;

public class GameEvent {

    public enum Type {
        PLAYER_MOVED, TRAP_TRIGGERED, ARTIFACT_COLLECTED,
        SCAN_ACTIVATED, RADAR_ACTIVATED,
        SHIELD_ACTIVATED, SHIELD_BROKEN,
        FLASHLIGHT_ACTIVATED, FOG_RADIUS_CHANGED,
        ENEMY_MOVED, MINI_GAME_TRIGGERED,
        LEVEL_COMPLETE, PLAYER_DIED
    }

    private final Type type;
    private final Position position; // null для подій без координат
    private final int payload;       // для FOG_RADIUS_CHANGED — новий радіус

    public GameEvent(Type type)                    { this(type, null, 0); }
    public GameEvent(Type type, Position position) { this(type, position, 0); }
    public GameEvent(Type type, int payload)       { this(type, null, payload); }

    private GameEvent(Type type, Position position, int payload) {
        this.type     = type;
        this.position = position;
        this.payload  = payload;
    }

    public Type     getType()     { return type; }
    public Position getPosition() { return position; }
    public int      getPayload()  { return payload; }
}
