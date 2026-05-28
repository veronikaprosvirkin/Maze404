package model;

import enums.CellType;

public class Grid {
    private final int rows;
    private final int cols;
    private final Cell[][] cells;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];

        // Ініціалізуємо базове поле підлогою
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell(CellType.FLOOR);
            }
        }
    }

    public int getWidth() { return cols; }
    public int getHeight() { return rows; }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public Cell getCell(int row, int col) {
        if (!isInBounds(row, col)) {
            throw new IndexOutOfBoundsException("Координати за межами сітки: " + row + ", " + col);
        }
        return cells[row][col];
    }

    public void setType(int row, int col, CellType type) {
        if (isInBounds(row, col)) {
            cells[row][col].setType(type);
        }
    }

    public void setCell(int row, int col, Cell cell) {
        this.cells[row][col] = cell;
    }

}
