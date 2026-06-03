package logic.system;

import events.EventBus;
import events.GameEvent;
import model.GameState;
import model.Player;
import model.Position;

public class ShieldSystem {
    public void activateShield(GameState state) {
        Player player = state.getPlayer();
        if(player.hasShield())
            return;
        player.activateShield();
        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.SHIELD_ACTIVATED,
                new Position(player.getRow(), player.getCol())));
    }
}
