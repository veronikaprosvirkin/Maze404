package logic.system;

import enums.CellType;
import events.EventBus;
import events.GameEvent;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;

public class ScanSystem {
    /**
     * Підраховує кількість пасток у 8 сусідніх клітинках
     * @return кількість пасток (0–8)
     */
    public int scan(GameState state) {
        Player player = state.getPlayer();
        Grid grid = state.getGrid();
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = player.getRow() + dr;
                int c = player.getCol() + dc;
                if (grid.isInBounds(r, c) && grid.getCell(r, c).getType() == CellType.TRAP)
                    count++;
            }
        }
        EventBus.getInstance().publish(
                new GameEvent(GameEvent.Type.SCAN_ACTIVATED,
                        new Position(player.getRow(), player.getCol())));
        return count;
    }
}
