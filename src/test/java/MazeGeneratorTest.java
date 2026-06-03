import enums.CellType;
import logic.generation.MazeGenerator;
import model.Grid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MazeGeneratorTest {
    @Test
    void testMazeGenerationBoundsAndExit() {
        MazeGenerator generator = new MazeGenerator();
        int rows = 11;
        int cols = 11;

        Grid grid = generator.generate(rows, cols);

        assertNotNull(grid);
        assertEquals(cols, grid.getWidth());
        assertEquals(rows, grid.getHeight());

        // Перевіряємо контрактне розміщення виходу (EXIT)
        assertEquals(CellType.EXIT, grid.getCell(rows - 2, cols - 2).getType());
    }

    @Test
    void testStartPointIsFloor() {
        MazeGenerator generator = new MazeGenerator();
        Grid grid = generator.generate(11, 11);

        // Точка старту гравця завжди має бути вільною
        assertEquals(CellType.FLOOR, grid.getCell(1, 1).getType());
    }
}
