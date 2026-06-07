package logic.system;

import events.EventBus;
import events.GameEvent;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;

public class BeaconSystem {
    public void placeBeacon(GameState state) {
        Player player = state.getPlayer();
        if (player.getBeaconCount() <= 0) {
            return;
        }

        Grid gr  = state.getGrid();
        gr.getCell(player.getRow(), player.getCol()).setFlagged(true);
        player.useBeacon();
        EventBus.getInstance().publish(new GameEvent(
                GameEvent.Type.BEACON_ACTIVATED,
                new Position(player.getRow(), player.getCol())
        ));

    }
}
