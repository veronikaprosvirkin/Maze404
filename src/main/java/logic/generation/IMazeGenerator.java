package logic.generation;

import model.Grid;

public interface IMazeGenerator {

    Grid generate(int rows, int cols);
}