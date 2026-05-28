package ua.mazegame.core.model;

import ua.mazegame.core.model.enums.CellType;

public class Cell {

    private CellType type;
    private boolean revealed = false;
    private boolean flagged = false;

    public Cell(CellType type) {
        this.type = type;
    }

    public CellType getType() { return type; }
    public boolean isRevealed() { return revealed; }
    public boolean isFlagged() { return flagged; }

    public void setType(CellType type) { this.type = type; }
    public void reveal() { this.revealed = true; }
    public void hide() { this.revealed = false; }
    public void setFlagged(boolean flag) { this.flagged = flag; }
}
