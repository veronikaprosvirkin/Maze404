import enums.CellType;
import events.EventBus;
import logic.system.ScanSystem;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanSystemTest {

    @BeforeEach
    void resetBus() {
        EventBus.getInstance().reset();
    }

    private GameState makeState(Grid grid, int playerRow, int playerCol) {
        Player player = new Player(playerRow, playerCol);
        return new GameState(grid, player, List.of(), List.of(), 1);
    }

    @Test
    void scanCountsAdjacentTraps() {
        // Сітка 5×5, гравець у (2,2), пастки в 4 сусідніх клітинках
        Grid grid = new Grid(5, 5);
        grid.setType(1, 1, CellType.TRAP); // вліво-вгору
        grid.setType(1, 2, CellType.TRAP); // вгору
        grid.setType(2, 1, CellType.TRAP); // вліво
        grid.setType(3, 3, CellType.TRAP); // вправо-вниз

        GameState state = makeState(grid, 2, 2);
        int count = new ScanSystem().scan(state);
        assertEquals(4, count);
    }

    @Test
    void scanReturnsZeroWhenNoTraps() {
        // Жодної пастки , scan має повернути 0
        Grid grid = new Grid(5, 5);
        GameState state = makeState(grid, 2, 2);
        assertEquals(0, new ScanSystem().scan(state));
    }

    @Test
    void scanIgnoresCellsOutsideGrid() {
        // Гравець у кутку (0,0) - більшість сусідів поза межами
        // Тільки (0,1) і (1,0) є в сітці і обидві TRAP
        Grid grid = new Grid(3, 3);
        grid.setType(0, 1, CellType.TRAP);
        grid.setType(1, 0, CellType.TRAP);
        GameState state = makeState(grid, 0, 0);
        assertEquals(2, new ScanSystem().scan(state));
    }

    @Test
    void scanDoesNotCountTrapUnderPlayer() {
        Grid grid = new Grid(5, 5);
        grid.setType(2, 2, CellType.TRAP); // під гравцем
        grid.setType(2, 3, CellType.TRAP); // сусід праворуч

        GameState state = makeState(grid, 2, 2);
        assertEquals(1, new ScanSystem().scan(state)); // тільки (2,3)
    }
}