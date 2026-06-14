package gameHistory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gameHistory.VictoryRecord;


import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class VictoryHistoryManager {
    private static final Path HISTORY_FILE_PATH = Path.of("src", "main", "resources", "history", "victory_history.json");
    private static final Gson gson = new Gson();

    public static void saveRecord(VictoryRecord record) {
        List<VictoryRecord> records = loadRecords();
        records.add(record);

        try {
            Files.createDirectories(HISTORY_FILE_PATH.getParent());
        } catch (IOException e) {
            System.err.println("[HistoryManager] Failed to create history directory: " + e.getMessage());
            return;
        }

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(HISTORY_FILE_PATH.toFile()), StandardCharsets.UTF_8)) {
            gson.toJson(records, writer);
        } catch (IOException e) {
            System.err.println("[HistoryManager] Failed to save record: " + e.getMessage());
        }
    }

    public static List<VictoryRecord> loadRecords() {
        File file = HISTORY_FILE_PATH.toFile();
        if (!file.exists()) return new ArrayList<>();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, new TypeToken<List<VictoryRecord>>(){}.getType());
        } catch (IOException e) {
            System.err.println("[HistoryManager] Failed to load records: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
