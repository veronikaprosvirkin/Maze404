package logic.persistance;

import com.google.gson.Gson;
import model.SaveData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SaveManager {
    private static final String SAVE_FILE = System.getProperty("user.home") + "/mazegame_save.json";
    private final Gson gson = new Gson();

    public void save(SaveData data) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[SaveManager] Failed to save: " + e.getMessage());
        }
    }

    public SaveData load() {
        Path path = Paths.get(SAVE_FILE);
        if (!Files.exists(path)) return new SaveData();
        try (Reader reader = new FileReader(SAVE_FILE)) {
            SaveData data = gson.fromJson(reader, SaveData.class);
            return data != null ? data : new SaveData();
        } catch (Exception e) {
            System.err.println("[SaveManager] Failed to load: " + e.getMessage());
            return new SaveData();
        }
    }
}
