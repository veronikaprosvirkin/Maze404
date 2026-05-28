package ua.mazegame.core.model;

import java.util.ArrayList;
import java.util.Collections;

public class GameState {

    private final Grid grid;
    private final Player player;
    private final List<Enemy> enemies;
    private final List<Artifact> artifacts;
    private int score         = 0;
    private int currentLevel  = 1;
    private boolean gameOver  = false;
    private boolean levelComplete = false;

    public GameState(Grid grid, Player player,
                     List<Enemy> enemies, List<Artifact> artifacts,
                     int level) {
        this.grid      = grid;
        this.player    = player;
        this.enemies   = new ArrayList<>(enemies);
        this.artifacts = new ArrayList<>(artifacts);
        this.currentLevel = level;
    }

    //  Геттери (контракт)
    public Grid           getGrid()          { return grid; }
    public Player         getPlayer()        { return player; }
    public List<Enemy>    getEnemies()       { return Collections.unmodifiableList(enemies); }
    public List<Artifact> getArtifacts()     { return artifacts; } // Вероніка мутує список
    public int            getScore()         { return score; }
    public int            getCurrentLevel()  { return currentLevel; }
    public boolean        isGameOver()       { return gameOver; }
    public boolean        isLevelComplete()  { return levelComplete; }

    //  Методи запису (тільки через GameEngine та *System-класи)
    public void addScore(int points)       { score += points; }
    public void setGameOver(boolean v)     { gameOver = v; }
    public void setLevelComplete(boolean v){ levelComplete = v; }
}

}
