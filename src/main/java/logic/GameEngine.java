package logic;

import AI.IEnemyAI;
import enums.CellType;
import events.EventBus;
import events.GameEvent;
import logic.system.*;
import model.Enemy;
import model.GameState;
import model.Position;
import ui.input.GameAction;

import java.util.Map;

public class GameEngine {

    private GameState gameState;
    private boolean paused = false;

    private final MovementSystem movementSystem = new MovementSystem();
    private final logic.system.CollisionDetector collisionSystem = new logic.system.CollisionDetector();
    private final ScanSystem scanSystem      = new ScanSystem();
    private final RadarSystem      radarSystem     = new RadarSystem();
    private final ShieldSystem     shieldSystem    = new ShieldSystem();
    private final BeaconSystem     beaconSystem    = new BeaconSystem();
    private final FogOfWarSystem   fogSystem       = new FogOfWarSystem();

    private final ArtifactSystem artifactSystem;
    public GameEngine(ArtifactSystem artifactSystem, Map<Enemy, IEnemyAI> enemyAI) {
        this.artifactSystem = artifactSystem;
    }
    public void loadLevel(GameState newState) {
        this.gameState = newState;
        this.paused = false;
        fogSystem.updateVisibility(gameState);
    }


    public void processAction(GameAction action) {
        if (paused || gameState.isGameOver() || gameState.isLevelComplete()) return;

        switch (action) {
            case MOVE_UP    -> doMove(-1,  0);
            case MOVE_DOWN  -> doMove( 1,  0);
            case MOVE_LEFT  -> doMove( 0, -1);
            case MOVE_RIGHT -> doMove( 0,  1);
            case SCAN       -> scanSystem.scan(gameState);
            case RADAR      -> radarSystem.activateRadar(gameState);
            case SHIELD     -> shieldSystem.activateShield(gameState);
            case BEACON     -> beaconSystem.placeBeacon(gameState);
        }

        checkWinLoseConditions();
    }

    public void resume()
    {
        paused = false;
    }
    public void pause(){ paused = true; }

    public  GameState getState() {
        return gameState;
    }


    //--------------- PRIVATE METHODS -------------
    private void checkWinLoseConditions()
    {
        if(gameState.getPlayer().getHealth() <= 0 ){
            gameState.setGameOver(true);
        }
    }
    private void doMove(int row, int col)
    {
        movementSystem.movePlayer(gameState, row, col);
        fogSystem.updateVisibility(gameState);
        artifactSystem.processArtifacts(gameState);

        moveEnemies();
        collisionSystem.checkEnemyCollisions(gameState);
        radarSystem.onPlayerTurn(gameState);
    }

    private void moveEnemies(){
        for (Enemy enemy : gameState.getEnemies()) {

            IEnemyAI ai = enemy.getAi();
            if (ai == null) continue;

            Position next = ai.computeNextMove(enemy, gameState.getGrid(), gameState.getPlayer());

            if (gameState.getGrid().isInBounds(next.getRow(), next.getCol()) &&
                    gameState.getGrid().getCell(next.getRow(), next.getCol()).getType() != CellType.WALL) {
                enemy.setRow(next.getRow());
                enemy.setCol(next.getCol());
                EventBus.getInstance().publish(
                        new GameEvent(GameEvent.Type.ENEMY_MOVED, next));
            }
        }
    }
}
