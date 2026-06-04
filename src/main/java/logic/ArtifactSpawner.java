package logic;

import Utilities.Util;
import enums.ArtifactType;
import enums.Difficulty;
import model.Artifact;
import model.Grid;
import model.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ArtifactSpawner {

    public List<Artifact> spawnArtifacts(Grid grid, Difficulty difficulty, Position playerStart) {
        List<Position> floorCells = Util.getFloorCells(grid);
        floorCells.removeIf(p -> p.manhattanDistance(playerStart) < 2);
        Collections.shuffle(floorCells);

        int crystalCount = 0;
        int miniGameCount = 0;
        int shieldCount = 0;
        int radarCount = 0;
        int beaconCount = 0;
        int elixirCount = 0;

        switch (difficulty) {
            case EASY -> {
                crystalCount = 10;
                miniGameCount = 3;
                shieldCount = 2;
                radarCount = 1;
                beaconCount = 2;
                elixirCount = 2;
            }
            case MEDIUM -> {
                crystalCount = 8;
                miniGameCount = 2;
                shieldCount = 2;
                radarCount = 1;
                beaconCount = 1;
                elixirCount = 2;
            }
            case HARD -> {
                crystalCount = 6;
                miniGameCount = 2;
                shieldCount = 2;
                radarCount = 1;
                beaconCount = 2;
                elixirCount = 1;
            }
        }

        List<ArtifactType> typesToSpawn = new ArrayList<>();
        for (int i = 0; i < crystalCount; i++) typesToSpawn.add(ArtifactType.CRYSTAL);
        for (int i = 0; i < miniGameCount; i++) typesToSpawn.add(ArtifactType.MINI_GAME);
        for (int i = 0; i < shieldCount; i++) typesToSpawn.add(ArtifactType.SHIELD);
        for (int i = 0; i < radarCount; i++) typesToSpawn.add(ArtifactType.RADAR);
        for (int i = 0; i < beaconCount; i++) typesToSpawn.add(ArtifactType.BEACON);
        for (int i = 0; i < elixirCount; i++) typesToSpawn.add(ArtifactType.ELIXIR);

        List<Artifact> artifacts = new ArrayList<>();
        int typeIndex = 0;

        int minDistance = 3;

        for (Position candidate : floorCells) {
            if (typeIndex >= typesToSpawn.size()) {
                break;
            }
            boolean isTooClose = false;
            for (Artifact spawnedArtifact : artifacts) {
                if (candidate.manhattanDistance(spawnedArtifact.getPosition()) < minDistance) {
                    isTooClose = true;
                    break;
                }
            }

            if (!isTooClose) {
                ArtifactType typeToPlace = typesToSpawn.get(typeIndex);
                artifacts.add(new Artifact(candidate, typeToPlace));
                typeIndex++;
            }
        }

        return artifacts;
    }
}