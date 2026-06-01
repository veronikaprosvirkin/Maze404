package logic;

import enums.ArtifactType;
import enums.CellType;
import enums.Difficulty;
import model.Artifact;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArtifactSpawner {

    private List<Position> getFloorCells(Grid grid) {
        List<Position> floorCells = new ArrayList<>();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                if (grid.getCell(x, y).getType() == CellType.FLOOR) {
                    floorCells.add(new Position(x, y));
                }
            }
        }
        return floorCells;
    }

    public List<Artifact> spawnArtifacts(Grid grid, Difficulty difficulty, Position playerStart) {
        List<Position> floorCells = getFloorCells(grid);
        floorCells.removeIf(p -> p.manhattanDistance(playerStart) < 2);
        Collections.shuffle(floorCells);

        int crystalCount = 0;
        int miniGameCount = 0;
        int shieldCount = 0;
        int radarCount = 0;
        int flashlightCount = 0;

        switch (difficulty) {
            case EASY -> {
                crystalCount = 7;
                miniGameCount = 1;
                shieldCount = 2;
                radarCount = 1;
                flashlightCount = 2;
            }
            case MEDIUM -> {
                crystalCount = 8;  // На 2 гри (6 шт) + 2 в запас
                miniGameCount = 2;
                shieldCount = 2;
                radarCount = 1;
                flashlightCount = 1;
            }
            case HARD -> {
                crystalCount = 12; // На 3 гри (9 шт) + 3 в запас
                miniGameCount = 3;
                shieldCount = 2;
                radarCount = 1;
                flashlightCount = 2;
            }
        }

        List<Artifact> artifacts = new ArrayList<>();
        int index = 0;

        //crystals
        for (int i = 0; i < crystalCount && index < floorCells.size(); i++) {
            artifacts.add(new Artifact(floorCells.get(index), ArtifactType.CRYSTAL));
            index++;
        }

        //mini games
        for (int i = 0; i < miniGameCount && index < floorCells.size(); i++) {
            artifacts.add(new Artifact(floorCells.get(index), ArtifactType.MINI_GAME));
            index++;
        }

        //shields
        for (int i = 0; i < shieldCount && index < floorCells.size(); i++) {
            artifacts.add(new Artifact(floorCells.get(index), ArtifactType.SHIELD));
            index++;
        }

        // radars
        for (int i = 0; i < radarCount && index < floorCells.size(); i++) {
            artifacts.add(new Artifact(floorCells.get(index), ArtifactType.RADAR));
            index++;
        }

        // flashlights
        for (int i = 0; i < flashlightCount && index < floorCells.size(); i++) {
            artifacts.add(new Artifact(floorCells.get(index), ArtifactType.FLASHLIGHT));
            index++;
        }

        return artifacts;
    }
}