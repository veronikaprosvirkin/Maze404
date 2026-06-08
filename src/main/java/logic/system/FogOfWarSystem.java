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
    private static final int BEACON_VISIBILITY_RADIUS = 5;

    public  FogOfWarSystem() {
        EventBus.getInstance().subscribe(GameEvent.Type.FOG_RADIUS_CHANGED,
                event ->{
            visibilityRadius = event.getPayload();
                });
    }

    public void updateVisibility(GameState state) {
        Player player = state.getPlayer();
        Grid grid = state.getGrid();
        Position playerPos = new Position(player.getRow(), player.getCol());
        for (int i = 0; i < grid.getHeight(); i++) {
            for (int j = 0; j < grid.getWidth(); j++) {
                Position p = new Position(i, j);
                if (playerPos.manhattanDistance(p) <= visibilityRadius || isWithinBeaconRadius(grid, p)) {
                    grid.getCell(i, j).reveal();
                }
            }
        }
    }

    private boolean isWithinBeaconRadius(Grid grid, Position target) {
        for (int row = 0; row < grid.getHeight(); row++) {
            for (int col = 0; col < grid.getWidth(); col++) {
                if (!grid.getCell(row, col).isFlagged()) {
                    continue;
                }
                if (new Position(row, col).manhattanDistance(target) <= BEACON_VISIBILITY_RADIUS) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getVisibilityRadius() {
        return visibilityRadius;
    }

}
