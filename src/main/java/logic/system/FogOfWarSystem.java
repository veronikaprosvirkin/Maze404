package logic.system;

import events.EventBus;
import events.GameEvent;
import jdk.jfr.EventType;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;
import org.w3c.dom.events.EventTarget;

public class FogOfWarSystem {
    private int visibilityRadius = 2;

    public  FogOfWarSystem() {
        EventBus.getInstance().subscribe(GameEvent.Type.FOG_RADIUS_CHANGED,
                event ->{
            visibilityRadius = event.getPayload();
                });
    }

    public void updateVisibility(GameState state) {
        Player player = state.getPlayer();
        Grid grid = state.getGrid();
        for (int i = 0; i < grid.getHeight(); i++) {
            for (int j = 0; j < grid.getWidth(); j++) {
                Position p = new Position(i, j);
                Position playerPos = new Position(player.getRow(), player.getCol());
                int distance = playerPos.manhattanDistance(p);
                if (distance <= visibilityRadius) {
                    grid.getCell(i, j).reveal();
                }
            }
        }
    }

    public int getVisibilityRadius() {
        return visibilityRadius;
    }

}
