package logic.system;

import enums.CellType;
import enums.Difficulty;
import events.EventBus;
import events.GameEvent;
import javafx.geometry.Pos;
import model.*;

public class MovementSystem {
    public void movePlayer(GameState state, int dRow, int dCol){
        Player player = state.getPlayer();
        int newRow = player.getRow() + dRow;
        int newCol = player.getCol() + dCol;

        Grid grid = state.getGrid();
        if(!grid.isInBounds(newRow,newCol)){return;}
        Cell target = grid.getCell(newRow,newCol);
        if(target.getType() == CellType.WALL) return;

        player.setRow(newRow);
        player.setCol(newCol);
        target.reveal();
        Position pos = new Position(newRow, newCol);
        EventBus.getInstance().publish(new GameEvent(GameEvent.Type.PLAYER_MOVED, pos));

        if(target.getType() == CellType.TRAP){
            EventBus.getInstance().publish(new GameEvent(GameEvent.Type.MINI_GAME_TRIGGERED, pos));
            player.takeDamage(1);
        }
        else if(target.getType() ==  CellType.EXIT){
            // Перевіряємо, чи це середній/складний рівень і чи порожній слот ключа
            if (state.getCurrentLevel() == 1 || state.getPlayer().hasKey()) {
                state.setLevelComplete(true);
                EventBus.getInstance().publish(new GameEvent(GameEvent.Type.LEVEL_COMPLETE));
            } else {
                EventBus.getInstance().publish(new GameEvent(GameEvent.Type.EXIT_BLOCKED));
            }
        }

    }
}
