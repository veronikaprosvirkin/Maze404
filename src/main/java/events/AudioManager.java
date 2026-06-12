package events;

import enums.Difficulty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;

public class AudioManager {
    private static final double BACKGROUND_VOLUME_SCALE = 0.5;
    private static final double EFFECT_VOLUME_SCALE = 0.7;

    private final boolean enabled;
    private MediaPlayer backgroundPlayer;
    private String currentBackgroundTrack;
    private boolean isMuted = false;
    private double musicVolume = 1.0;
    private double effectsVolume = 1.0;

    public AudioManager(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) return;

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
    }

    public void playMenuMusic() {
        playBackgroundLoop("menu-screen.wav");
    }

    public void playLevelMusic(Difficulty difficulty) {
        playBackgroundLoop(getLevelTrack(difficulty));
    }

    private void playBackgroundLoop(String filename) {
        if (!enabled) return;
        if (filename.equals(currentBackgroundTrack) && backgroundPlayer != null) {
            if (!isMuted) {
                backgroundPlayer.play();
            }
            return;
        }

        stopBackground();
        URL url = getClass().getResource("/sounds/" + filename);
        if (url == null) return;

        Media media = new Media(url.toExternalForm());
        backgroundPlayer = new MediaPlayer(media);
        currentBackgroundTrack = filename;
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
        effectPlayer.setVolume(effectsVolume * EFFECT_VOLUME_SCALE);
        effectPlayer.play();
    }

    public void setMusicVolume(double musicVolume) {
        this.musicVolume = clamp(musicVolume, 0.0, 1.0);
        applyBackgroundVolume();
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public void setEffectsVolume(double effectsVolume) {
        this.effectsVolume = clamp(effectsVolume, 0.0, 1.0);
    }

    public double getEffectsVolume() {
        return effectsVolume;
    }

    private void applyBackgroundVolume() {
        if (backgroundPlayer != null) {
            backgroundPlayer.setVolume(musicVolume * BACKGROUND_VOLUME_SCALE);
        }
    }

    private void stopBackground() {
        if (backgroundPlayer != null) {
            backgroundPlayer.stop();
            backgroundPlayer.dispose();
            backgroundPlayer = null;
        }
        currentBackgroundTrack = null;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String getLevelTrack(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "stone-desert.wav";
            case HARD -> "flame-hell.wav";
            default -> "cryo-dangeon.wav";
        };
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
