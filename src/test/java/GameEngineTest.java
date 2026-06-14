import AI.IEnemyAI;
import enums.CellType;
import enums.EnemyMode;
import events.EventBus;
import logic.ArtifactSystem;
import logic.*;
import model.*;
import model.Grid;
import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import logic.GameEngine;
import model.*;
import ui.input.GameAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    // Заглушка ArtifactSystem
    private static final ArtifactSystem NO_ARTIFACTS = new ArtifactSystem();
    private static final Map<Enemy, IEnemyAI> enemyAI = new HashMap<>();

    @BeforeEach
    void resetBus() {
        EventBus.getInstance().reset();
    }

    private GameEngine makeEngine() {
        return new GameEngine(NO_ARTIFACTS, enemyAI);
    }

    /**
     * Проста сітка 3×3:
     */
    private GameState makeSimpleState(int playerRow, int playerCol) {
        Grid grid = new Grid(3, 3);
        grid.setType(0, 2, CellType.WALL);
        grid.setType(1, 2, CellType.WALL);
        grid.setType(2, 2, CellType.EXIT);
        Player player = new Player(playerRow, playerCol);
        return new GameState(grid, player, List.of(), List.of(), 1);
    }

    // Рух

    @Test
    void playerCannotMoveIntoWall() {
        GameEngine engine = makeEngine();
        GameState state = makeSimpleState(1, 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT); // (1,2) = WALL

        assertEquals(1, state.getPlayer().getRow());
        assertEquals(1, state.getPlayer().getCol());
    }

    @Test
    void playerMovesCorrectly() {
        GameEngine engine = makeEngine();
        GameState state = makeSimpleState(1, 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_DOWN); // (2,1) = FLOOR

        assertEquals(2, state.getPlayer().getRow());
        assertEquals(1, state.getPlayer().getCol());
    }

    // Пастки

    @Test
    void trapDealsDamageOnStep() {
        GameEngine engine = makeEngine();
        Grid grid = new Grid(3, 3);
        grid.setType(0, 2, CellType.TRAP); // гравець зайде сюди
        Player player = new Player(0, 1);
        GameState state = new GameState(grid, player, List.of(), List.of(), 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT); // наступає на TRAP

        assertEquals(2, state.getPlayer().getHealth()); // 3 - 1 = 2
    }

    @Test
    void threeTrapsKillPlayer() {
        GameEngine engine = makeEngine();

        // Ряд пасток: гравець іде вправо три рази
        Grid grid = new Grid(1, 5);
        for (int c = 1; c <= 3; c++)
            grid.setType(0, c, CellType.TRAP);

        Player player = new Player(0, 0);
        GameState state = new GameState(grid, player, List.of(), List.of(), 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT); // HP = 2
        engine.processAction(GameAction.MOVE_RIGHT); // HP = 1
        engine.processAction(GameAction.MOVE_RIGHT); // HP = 0 → PLAYER_DIED

        assertEquals(0, state.getPlayer().getHealth());
        assertTrue(state.isGameOver());
    }

    // shield
    @Test
    void shieldAbsorbsDamage() {
        GameEngine engine = makeEngine();
        Grid grid = new Grid(3, 3);
        grid.setType(0, 2, CellType.TRAP);
        Player player = new Player(0, 1);

        // ДОДАНО: Спочатку кладемо 1 заряд щита в інвентар гравця!
        player.addShield(1);

        GameState state = new GameState(grid, player, List.of(), List.of(), 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.SHIELD);          // активуємо щит
        assertTrue(state.getPlayer().hasShield());        // ТЕПЕР ЦЕ БУДЕ TRUE!

        engine.processAction(GameAction.MOVE_RIGHT);      // наступаємо на TRAP

        assertEquals(3, state.getPlayer().getHealth());   // HP не змінився
        assertFalse(state.getPlayer().hasShield());       // щит зламався
    }

    @Test
    void repeatedEnemyCollisionUsesCooldown() {
        GameEngine engine = makeEngine();
        Grid grid = new Grid(3, 3);
        Player player = new Player(1, 0);
        Enemy enemy = new Enemy(1, 1, EnemyMode.CHASE, null);
        GameState state = new GameState(grid, player, List.of(enemy), List.of(), 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT);

        assertEquals(2, state.getPlayer().getHealth());
    }


    @Test
    void actionsIgnoredAfterGameOver() {
        GameEngine engine = makeEngine();
        GameState state = makeSimpleState(1, 1);
        state.setGameOver(true);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_DOWN);

        assertEquals(1, state.getPlayer().getRow()); // позиція не змінилась
    }

    @Test
    void levelCompletesOnExitStep() {
        // 1. Явно вказуємо, що це ЛЕГКИЙ рівень, щоб вихід був дозволений без ключа
        enums.Difficulty.current = enums.Difficulty.EASY;

        GameEngine engine = makeEngine();
        // Гравець у (2,1), EXIT у (2,2)
        Grid grid = new Grid(3, 3);
        grid.setType(2, 2, CellType.EXIT);
        Player player = new Player(2, 1);

        // Рівень 1 — ключ не потрібен на Easy
        GameState state = new GameState(grid, player, List.of(), List.of(), 1);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT);

        assertTrue(state.isLevelComplete());
    }

    @Test
    void exitBlockedOnLevel2WithoutKey() {
        // 1. Явно вказуємо, що поточна складність гри - СЕРЕДНЯ (Medium),
        // щоб обійти умову Difficulty.current == Difficulty.EASY
        enums.Difficulty.current = enums.Difficulty.MEDIUM;

        GameEngine engine = makeEngine();
        Grid grid = new Grid(3, 3);
        grid.setType(2, 2, CellType.EXIT);
        Player player = new Player(2, 1);

        // Рівень 2, ключа немає
        GameState state = new GameState(grid, player, List.of(), List.of(), 2);
        engine.loadLevel(state);

        engine.processAction(GameAction.MOVE_RIGHT); // гравець робить крок на двері

        assertFalse(state.isLevelComplete()); // Тепер тест буде успішним, вихід заблоковано!
    }
}
