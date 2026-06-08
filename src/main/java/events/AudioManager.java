package events;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

public class AudioManager {
    private final boolean enabled;
    private Clip backgroundClip;

    public AudioManager(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) return;

        // Існуючі підписки на події
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_MOVED, e -> play("move.wav"));
        EventBus.getInstance().subscribe(GameEvent.Type.TRAP_TRIGGERED, e -> play("trap.wav"));
        EventBus.getInstance().subscribe(GameEvent.Type.SCAN_ACTIVATED, e -> play("scan.wav"));
        EventBus.getInstance().subscribe(GameEvent.Type.RADAR_ACTIVATED, e -> play("radar.wav"));
        EventBus.getInstance().subscribe(GameEvent.Type.SHIELD_BROKEN, e -> play("shield.wav"));
        EventBus.getInstance().subscribe(GameEvent.Type.LEVEL_COMPLETE, e -> {
            stopBackground(); // Зупиняємо ембієнт під час перемоги
            play("win.wav");
        });
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DIED, e -> {
            stopBackground(); // Зупиняємо ембієнт під час програшу
            play("lose.wav");
        });

        EventBus.getInstance().subscribe(GameEvent.Type.EXIT_BLOCKED, e -> play("error.wav"));
        playBackground("bg.wav");
    }

    // Метод для одноразових звукових ефектів
    private void play(String filename) {
        try {
            URL url = getClass().getResource("/sounds/" + filename);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (Exception e) {

        }
    }
    private void playBackground(String filename) {
        try {
            URL url = getClass().getResource("/sounds/" + filename);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(ais);

            // Зациклюємо відтворення нескінченно
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        } catch (Exception e) {

        }
    }

    public void stopBackground() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
        }
    }
}
