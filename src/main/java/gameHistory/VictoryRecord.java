package gameHistory;

import java.util.Map;

public class VictoryRecord {
    private final int levelNumber;
    private final long timeSeconds;
    private final Map<String, Integer> collectedArtifacts;
    private final String date;

    public VictoryRecord(int levelNumber, long timeSeconds, Map<String, Integer> collectedArtifacts, String date) {
        this.levelNumber = levelNumber;
        this.timeSeconds = timeSeconds;
        this.collectedArtifacts = collectedArtifacts;
        this.date = date;
    }

    public int getLevelNumber() { return levelNumber; }
    public long getTimeSeconds() { return timeSeconds; }
    public Map<String, Integer> getCollectedArtifacts() { return collectedArtifacts; }
    public String getDate() { return date; }
}