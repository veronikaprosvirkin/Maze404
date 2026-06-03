package logic.system;

import events.EventBus;
import events.GameEvent;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;

public class RadarSystem {
    private static final int RADAR_ZONE = 2;    // радіус: 5×5 = ±2 від гравця
    private static final int RADAR_DURATION = 3; // ходів

    private int turnsRemaining = 0;

    public void activateRadar(GameState state) {
        Player player = state.getPlayer();
        if (player.getRadarCharges() <= 0) return;

        player.useRadarCharge();
        turnsRemaining = RADAR_DURATION;
        revealZone(state);

        EventBus.getInstance().publish(
                new GameEvent(GameEvent.Type.RADAR_ACTIVATED,
                        new Position(player.getRow(), player.getCol())));
    }

    public void revealZone(GameState state) {
        setZoneRevealed(state,true);
    }
    private void setZoneRevealed(GameState state, boolean revealed) {
        Player player = state.getPlayer();
        Grid grid = state.getGrid();
        for (int dr = -RADAR_ZONE; dr <= RADAR_ZONE; dr++) {
            for (int dc = -RADAR_ZONE; dc <= RADAR_ZONE; dc++) {
                int r = player.getRow() + dr;
                int c = player.getCol() + dc;
                if (!grid.isInBounds(r, c)) continue;
                if (revealed) grid.getCell(r, c).reveal();
                else          grid.getCell(r, c).hide();
            }
        }
    }

    private void hideZone(GameState state) {
        setZoneRevealed(state,false);
    }

    public void onPlayerTurn(GameState state) {
        if(turnsRemaining <= 0) return;
        turnsRemaining--;
        if(turnsRemaining == 0)
            hideZone(state); //under fog
    }
}
