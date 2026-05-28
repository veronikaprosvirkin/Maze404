package model;

import lombok.AllArgsConstructor;
import lombok.Data;


import enums.ArtifactType;

@Data
@AllArgsConstructor
public class Artifact {
    private final Position position;
    private final ArtifactType type;
    private boolean collected = false;

    public Artifact(Position position, ArtifactType type, Position position1, ArtifactType type1) {
        this.position = position1;
        this.type = type1;
    }

    public void collect() {
        this.collected = true;
    }
}

