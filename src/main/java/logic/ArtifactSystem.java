package logic;

import events.EventBus;
import events.GameEvent;
import model.Artifact;
import model.GameState;
import model.Player;
import model.Position;

public class ArtifactSystem {
    public void processArtifacts(GameState state) {
        Player player = state.getPlayer();
        Position playerPos =  new Position( player.getRow(), player.getCol());

        for (Artifact artifact : state.getArtifacts()) {
            if (artifact.isCollected()) {
                continue;
            }
            if (playerPos.equals(artifact.getPosition())) {
                artifact.collect();
                switch (artifact.getType()) {
                    case CRYSTAL -> {
                        player.addCrystals(1);
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                    case RADAR -> {
                        player.addRadarCharge(1);
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                    case SHIELD -> {
                        player.addShield(1);
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                    case BEACON -> {
                        player.addBeacon(1);
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                    case ELIXIR -> {
                        player.addElixir(1);
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                    case MINI_GAME -> {
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.MINI_GAME_TRIGGERED, playerPos));
                    }
                    case KEY -> {
                        player.addKey();
                        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.ARTIFACT_COLLECTED, playerPos));
                    }
                }
            }
        }
    }
}
