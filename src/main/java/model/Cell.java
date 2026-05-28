package model;

import enums.CellType;
import lombok.Setter;

public class Cell {

    @Setter
    private CellType type;
    private boolean revealed = false;
    @Setter
    private boolean flagged = false;

    public Cell(CellType type) {
        this.type = type;
    }

    public CellType getType() { return type; }
    public boolean isRevealed() { return revealed; }
    public boolean isFlagged() { return flagged; }

    public void reveal() { this.revealed = true; }
    public void hide() { this.revealed = false; }
}
