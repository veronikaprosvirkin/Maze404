package ua.mazegame.core.model;

import java.util.List;
import java.util.Objects;

public final class Position {
    private final int row;
    private final int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public int manhattanDistance(Position other) {
        return Math.abs(this.row - other.row) + Math.abs(this.col - other.col);
    }

    public List<Position> getNeighbors() {
        return List.of(
                new Position(row - 1, col),  // вгору
                new Position(row + 1, col),  // вниз
                new Position(row, col - 1),  // вліво
                new Position(row, col + 1)   // вправо
        );
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Position p)) return false;
        return row == p.row && col == p.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }
}
