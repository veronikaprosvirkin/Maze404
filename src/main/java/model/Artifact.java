package model;

import lombok.Data;
import enums.ArtifactType;

@Data
public class Artifact {
    private final Position position;
    private final ArtifactType type;
    private boolean collected = false;


    public Artifact(Position position, ArtifactType type) {
        this.position = position;
        this.type = type;
    }

    public void collect() {
        this.collected = true;
    }
}