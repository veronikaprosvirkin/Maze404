package events;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class AudioManager {
    private static final double BACKGROUND_VOLUME_SCALE = 0.5;
    private static final double EFFECT_VOLUME_SCALE = 0.7;

    private final boolean enabled;
    private MediaPlayer backgroundPlayer;
    private boolean isMuted = false;
    private double masterVolume = 1.0;

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
            //stopBackground();
            play("win.wav");
        });
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DIED, e -> {
            //stopBackground();
            play("lose.wav");
        });

        EventBus.getInstance().subscribe(GameEvent.Type.EXIT_BLOCKED, e -> play("error.wav"));

        initBackgroundMusic("bg.wav");
    }


    private void initBackgroundMusic(String filename) {
        URL url = getClass().getResource("/sounds/" + filename);
        if (url == null) return;

        Media media = new Media(url.toExternalForm());
        backgroundPlayer = new MediaPlayer(media);
        backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        applyBackgroundVolume();
        if (!isMuted) {
            backgroundPlayer.play();
        }
    }

    private void play(String filename) {
        if (isMuted) return;

        URL url = getClass().getResource("/sounds/" + filename);
        if (url == null) return;

        // MediaPlayer для ефектів: створюємо новий, щоб звуки могли накладатися
        MediaPlayer effectPlayer = new MediaPlayer(new Media(url.toExternalForm()));
        effectPlayer.setVolume(masterVolume * EFFECT_VOLUME_SCALE);
        effectPlayer.play();
    }

    public void setMasterVolume(double masterVolume) {
        this.masterVolume = clamp(masterVolume, 0.0, 1.0);
        applyBackgroundVolume();
    }

    public double getMasterVolume() {
        return masterVolume;
    }

    private void applyBackgroundVolume() {
        if (backgroundPlayer != null) {
            backgroundPlayer.setVolume(masterVolume * BACKGROUND_VOLUME_SCALE);
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

/*
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
    /*private void playBackground(String filename) {
        try {
            URL url = getClass().getResource("/sounds/" + filename);
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(ais);

            // Зациклюємо відтворення нескінченно
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to play background music: " + e.getMessage());
        }
    }*
     */

    /*private void playBackground(String filename) {
        URL url = getClass().getResource("/sounds/" + filename);
        if (url == null) return;

        backgroundClip = new AudioClip(url.toExternalForm());
        backgroundClip.setCycleCount(AudioClip.INDEFINITE);
        backgroundClip.play();
    }

    public void stopBackground() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
        }
    }*/

    /*private void toggleMute() {
        isMuted = !isMuted;
        toggleBackgroundMusic();
    }

    private void toggleBackgroundMusic() {
        if (backgroundClip != null) {
            if (isMuted) {
                backgroundClip.stop();
            } else {
                // Метод start() продовжує грати з місця зупинки
                backgroundClip.start();
                backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        }
    }*/
}
