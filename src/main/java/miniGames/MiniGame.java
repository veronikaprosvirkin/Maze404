package miniGames;

import enums.Difficulty;
import enums.MiniGameResult;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

public abstract class MiniGame {
    protected int width = 450;
    protected int height = 450;
    protected MiniGameResult result = MiniGameResult.PENDING;
    protected String rewardName = "";

    protected void setupWindow(Stage stage, Scene scene, String title) {
        String cssFile = switch (Difficulty.current) {
            case MEDIUM -> "/styles/minigames-stone.css";
            case HARD -> "/styles/minigames-inferno.css";
            default -> "/styles/minigames-cryo.css";
        };
        scene.getStylesheets().add(
                getClass().getResource(cssFile).toExternalForm());
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Injects a full-screen animated overlay on top of the game.
     * Call whenever result changes from PENDING to SUCCESS or FAILURE.
     *
     * @param wrapper the StackPane that is the scene's root
     * @param isWin   true → win overlay; false → lose overlay
     */
    protected void showEndOverlay(StackPane wrapper, boolean isWin) {
        showEndOverlay(wrapper, isWin, null);
    }

    protected void showEndOverlay(StackPane wrapper, boolean isWin, String customSubtitle) {
        String accentColor;
        String glowColor;
        switch (Difficulty.current) {
            case MEDIUM:
                accentColor = isWin ? "#5A8248" : "#C4442A";
                glowColor = isWin ? "rgba(90,130,72,0.6)" : "rgba(196,68,42,0.6)";
                break;
            case HARD:
                accentColor = isWin ? "#6A5028" : "#CC2020";
                glowColor = isWin ? "rgba(106,80,40,0.6)" : "rgba(204,32,32,0.6)";
                break;
            default: // EASY
                accentColor = isWin ? "#9AB8C8" : "#B03878";
                glowColor = isWin ? "rgba(154,184,200,0.6)" : "rgba(176,56,120,0.6)";
                break;
        }

        String icon = isWin ? "✦" : "✖";
        String titleText = isWin ? "VICTORY" : "SYSTEM FAILURE";
        String subtitleText = customSubtitle != null
                ? customSubtitle
                : (isWin ? "Access granted.  Proceed." : "Connection lost.  Try again.");

        if (isWin && rewardName != null && !rewardName.isEmpty()) {
            subtitleText += "\nAcquired: " + rewardName;
        }

        // ── icon ────────────────────────────────────────────────────
        Label iconLabel = new Label(icon);
        iconLabel.setId("end-icon-label");
        iconLabel.setStyle(
                "-fx-text-fill: " + accentColor + ";" +
                        "-fx-effect: dropshadow(gaussian, " + accentColor + ", 28, 0.7, 0, 0);");

        // ── title ───────────────────────────────────────────────────
        Label titleLabel = new Label(titleText);
        titleLabel.setId("end-title-label");
        titleLabel.setStyle(
                "-fx-text-fill: " + accentColor + ";" +
                        "-fx-effect: dropshadow(gaussian, " + glowColor + ", 22, 0.6, 0, 0);");

        // ── subtitle ────────────────────────────────────────────────
        Label subtitleLabel = new Label(subtitleText);
        subtitleLabel.setId("end-subtitle-label");

        // ── separator bar ───────────────────────────────────────────
        Label separator = new Label("─  ─  ─  ─  ─  ─  ─  ─");
        separator.setId("end-separator-label");
        separator.setStyle("-fx-text-fill: " + accentColor + "; -fx-opacity: 0.4;");

        // ── content box ─────────────────────────────────────────────
        VBox box = new VBox(14, iconLabel, titleLabel, separator, subtitleLabel);
        box.setId("end-overlay-box");
        box.setAlignment(Pos.CENTER);

        // ── full-screen overlay ─────────────────────────────────────
        StackPane overlay = new StackPane(box);
        overlay.setId("end-overlay");
        overlay.setOpacity(0);

        wrapper.getChildren().add(overlay);

        // ── fade-in ─────────────────────────────────────────────────
        FadeTransition fade = new FadeTransition(Duration.millis(500), overlay);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        // Auto-close any minigame window shortly after end result is shown.
        PauseTransition closeDelay = new PauseTransition(Duration.seconds(3));
        closeDelay.setOnFinished(e -> {
            Window window = wrapper.getScene() != null ? wrapper.getScene().getWindow() : null;
            if (window instanceof Stage stage) {
                stage.close();
            }
        });
        closeDelay.play();
    }

    public MiniGameResult getResult() {
        return this.result;
    }
}
