package ui.render;

import enums.ArtifactType;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LevelIsland {
    private static final Duration ANIMATION_DURATION = Duration.millis(260);
    private static final Duration MESSAGE_DURATION = Duration.seconds(2.4);
    private static final double COLLAPSED_WIDTH = 0.0;
    private static final double ACTION_START_OFFSET = -12.0;
    private static final double MIN_ACTION_WIDTH = 172.0;

    private final String title;
    private final HBox view;
    private final HBox messageBox;
    private final Label messageLabel;
    private final PauseTransition hideTimer = new PauseTransition(MESSAGE_DURATION);
    private Timeline transition;

    public LevelIsland(String title) {
        this.title = title;

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pause-title-label");

        messageLabel = new Label();
        messageLabel.getStyleClass().add("level-island-action-text");

        Node shieldIcon = ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 18);
        messageBox = new HBox(8, shieldIcon, messageLabel);
        messageBox.getStyleClass().add("level-island-action");
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setVisible(false);
        messageBox.setManaged(false);
        messageBox.setOpacity(0);
        messageBox.setTranslateX(ACTION_START_OFFSET);
        setMessageWidth(COLLAPSED_WIDTH);

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(24);
        clip.arcHeightProperty().set(24);
        clip.widthProperty().bind(messageBox.widthProperty());
        clip.heightProperty().bind(messageBox.heightProperty());
        messageBox.setClip(clip);

        view = new HBox(12, titleLabel, messageBox);
        view.getStyleClass().addAll("game-hud", "level-island");
        view.setAlignment(Pos.CENTER);
        view.setPadding(new Insets(12, 18, 12, 18));
        view.setPickOnBounds(false);
        view.setMaxWidth(Region.USE_PREF_SIZE);
        view.setMaxHeight(Region.USE_PREF_SIZE);

        hideTimer.setOnFinished(event -> hideMessage());
    }

    public HBox getView() {
        return view;
    }

    public String getTitle() {
        return title;
    }

    public void showArtifactMessage(ArtifactType artifactType, String message) {
        messageBox.getChildren().set(0, ArtifactVisuals.createHudIcon(artifactType, 18));
        showMessage(message);
    }

    public void dispose() {
        hideTimer.stop();
        stopTransition();
    }

    private void showMessage(String message) {
        hideTimer.stop();
        stopTransition();

        messageLabel.setText(message);
        messageBox.setVisible(true);
        messageBox.setManaged(true);
        messageBox.applyCss();

        double targetWidth = Math.max(MIN_ACTION_WIDTH, messageBox.prefWidth(-1));
        transition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageBox.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateXProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        transition.setOnFinished(event -> {
            transition = null;
            hideTimer.playFromStart();
        });
        transition.playFromStart();
    }

    private void hideMessage() {
        stopTransition();

        transition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageBox.minWidthProperty(), COLLAPSED_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.prefWidthProperty(), COLLAPSED_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.maxWidthProperty(), COLLAPSED_WIDTH, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateXProperty(), ACTION_START_OFFSET, Interpolator.EASE_BOTH))
        );
        transition.setOnFinished(event -> {
            transition = null;
            messageBox.setVisible(false);
            messageBox.setManaged(false);
        });
        transition.playFromStart();
    }

    private void setMessageWidth(double width) {
        messageBox.setMinWidth(width);
        messageBox.setPrefWidth(width);
        messageBox.setMaxWidth(width);
    }

    private void stopTransition() {
        if (transition != null) {
            transition.stop();
            transition = null;
        }
    }
}
