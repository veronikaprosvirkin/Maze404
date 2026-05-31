package miniGames;

import enums.MiniGameResult;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public abstract class MiniGame {
    protected int width = 450;
    protected int height = 450;
    protected MiniGameResult result = MiniGameResult.PENDING;

    protected void setupWindow(Stage stage, Scene scene, String title) {
        scene.getStylesheets().add(
            getClass().getResource("/styles/minigames.css").toExternalForm()
        );
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Injects a full-screen animated overlay on top of the game.
     * Call whenever result changes from PENDING to SUCCESS or FAILURE.
     *
     * @param wrapper  the StackPane that is the scene's root
     * @param isWin    true → win overlay (cyan); false → lose overlay (magenta)
     */
    protected void showEndOverlay(StackPane wrapper, boolean isWin) {
        String accentColor  = isWin ? "#9AB8C8" : "#B03878";
        String glowColor    = isWin ? "rgba(154,184,200,0.6)" : "rgba(176,56,120,0.6)";
        String icon         = isWin ? "✦" : "✖";
        String titleText    = isWin ? "VICTORY" : "SYSTEM FAILURE";
        String subtitleText = isWin ? "Access granted.  Proceed." : "Connection lost.  Try again.";

        // ── icon ────────────────────────────────────────────────────
        Label iconLabel = new Label(icon);
        iconLabel.setId("end-icon-label");
        iconLabel.setStyle(
            "-fx-text-fill: " + accentColor + ";" +
            "-fx-effect: dropshadow(gaussian, " + accentColor + ", 28, 0.7, 0, 0);"
        );

        // ── title ───────────────────────────────────────────────────
        Label titleLabel = new Label(titleText);
        titleLabel.setId("end-title-label");
        titleLabel.setStyle(
            "-fx-text-fill: " + accentColor + ";" +
            "-fx-effect: dropshadow(gaussian, " + glowColor + ", 22, 0.6, 0, 0);"
        );

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
    }

    public MiniGameResult getResult() {
        return this.result;
    }
}
