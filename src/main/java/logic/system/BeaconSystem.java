package logic.system;

import model.GameState;
import model.Grid;
import model.Player;

public class BeaconSystem {
    public void placeBeacon(GameState state) {
        Player player = state.getPlayer();
        if(player.getBeaconCount() <= 0)
            return;
        Grid gr  = state.getGrid();
        gr.getCell(player.getRow(), player.getCol()).setFlagged(true);

    }
}
