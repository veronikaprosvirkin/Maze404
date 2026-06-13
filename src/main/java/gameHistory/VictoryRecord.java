package gameHistory;

import java.util.Map;

public class VictoryRecord {
    private final int levelNumber;
    private final long timeSeconds;
    private final Map<String, Integer> collectedArtifacts;
    private final String date;
    private final int health;

    public VictoryRecord(int levelNumber, long timeSeconds, Map<String, Integer> collectedArtifacts, String date, int health) {
        this.levelNumber = levelNumber;
        this.timeSeconds = timeSeconds;
        this.collectedArtifacts = collectedArtifacts;
        this.date = date;
        this.health = health;
    }

    public int getLevelNumber() { return levelNumber; }
    public long getTimeSeconds() { return timeSeconds; }
    public Map<String, Integer> getCollectedArtifacts() { return collectedArtifacts; }
    public String getDate() { return date; }
    public int getHealth() { return health; }
}