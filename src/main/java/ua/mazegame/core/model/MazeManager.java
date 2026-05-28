package ua.mazegame.core.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Відповідає за читання файлу, зберігання матриці лабіринту. бере файл, парсить його, створює об'єкт Grid, заповнює його і повертає вже готовим.
public class MazeManager {
    public Grid loadTxtLevel(String filePath) throws IOException {
            List<String> lines = new ArrayList<>();
            try(BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while( (line = reader.readLine()) != null ) {
                    if(!line.trim().isEmpty()){
                        lines.add(line);
                    }
                }
            }

            int rows = lines.size();
            int cols = lines.get(0).length();
            Grid grid = new Grid(rows, cols);
            for(int i = 0; i < rows; i++) {
                String line = lines.get(i);
                for(int j = 0; j < cols; j++) {
                    char ch = line.charAt(j);
                    Cell.Type type = switch (ch){
                        case '#' -> Cell.Type.WALL;
                        case 'T' -> Cell.Type.TRAP;
                        case 'A' -> Cell.Type.ARTIFACT;
                        case 'E' -> Cell.Type.EXIT;
                        default -> Cell.Type.EMPTY;
                    };
                    grid.cells[i][j] = new Cell(type);
                }

            }
        return grid;
    }
}
