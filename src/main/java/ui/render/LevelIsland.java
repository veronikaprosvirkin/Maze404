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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class LevelIsland {
    private static final Duration ANIMATION_DURATION = Duration.millis(260);
    private static final Duration MESSAGE_DURATION = Duration.seconds(2.4);
    private static final double COLLAPSED_SIZE = 0.0;
    private static final double MESSAGE_START_OFFSET = -8.0;
    private static final double MIN_MESSAGE_HEIGHT = 34.0;
    private static final double TINY_MESSAGE_SIZE = 42.0;

    private final String title;
    private final HBox view;
    private final VBox islandBase;
    private final HBox messageBox;
    private final Label messageLabel;
    private final StackPane tinyMessageCircle;
    private final Label tinyMessageLabel;
    private final PauseTransition hideTimer = new PauseTransition(MESSAGE_DURATION);
    private Timeline messageTransition;
    private Timeline tinyMessageTransition;
    private Timeline tinyCountdownTimeline;

    public LevelIsland(String title) {
        this.title = title;

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pause-title-label");

        messageLabel = new Label();
        messageLabel.getStyleClass().add("level-island-message-text");

        Node shieldIcon = ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 18);
        messageBox = new HBox(8, shieldIcon, messageLabel);
        messageBox.getStyleClass().add("level-island-message");
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setVisible(false);
        messageBox.setManaged(false);
        messageBox.setOpacity(0);
        messageBox.setTranslateY(MESSAGE_START_OFFSET);
        setMessageHeight(COLLAPSED_SIZE);

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(24);
        clip.arcHeightProperty().set(24);
        clip.widthProperty().bind(messageBox.widthProperty());
        clip.heightProperty().bind(messageBox.heightProperty());
        messageBox.setClip(clip);

        islandBase = new VBox(8, titleLabel, messageBox);
        islandBase.getStyleClass().addAll("game-hud", "level-island");
        islandBase.setAlignment(Pos.CENTER);
        islandBase.setPadding(new Insets(12, 18, 12, 18));
        islandBase.setPickOnBounds(false);
        islandBase.setMaxWidth(Region.USE_PREF_SIZE);
        islandBase.setMaxHeight(Region.USE_PREF_SIZE);

        tinyMessageLabel = new Label();
        tinyMessageLabel.getStyleClass().add("level-island-tiny-text");
        tinyMessageCircle = new StackPane(tinyMessageLabel);
        tinyMessageCircle.getStyleClass().addAll("game-hud", "level-island-tiny");
        tinyMessageCircle.setAlignment(Pos.CENTER);
        tinyMessageCircle.setVisible(false);
        tinyMessageCircle.setManaged(false);
        tinyMessageCircle.setOpacity(0);
        tinyMessageCircle.setScaleX(0.64);
        tinyMessageCircle.setScaleY(0.64);
        tinyMessageCircle.setMinSize(TINY_MESSAGE_SIZE, TINY_MESSAGE_SIZE);
        tinyMessageCircle.setPrefSize(TINY_MESSAGE_SIZE, TINY_MESSAGE_SIZE);
        tinyMessageCircle.setMaxSize(TINY_MESSAGE_SIZE, TINY_MESSAGE_SIZE);

        view = new HBox(8, islandBase, tinyMessageCircle);
        view.setAlignment(Pos.CENTER);
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

    public void showTinyMessage(String message) {
        stopTinyMessageTransition();

        tinyMessageLabel.setText(message);
        tinyMessageCircle.setVisible(true);
        tinyMessageCircle.setManaged(true);

        tinyMessageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(tinyMessageCircle.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleYProperty(), 1.0, Interpolator.EASE_BOTH))
        );
        tinyMessageTransition.setOnFinished(event -> tinyMessageTransition = null);
        tinyMessageTransition.playFromStart();
    }

    public void showTinyCountdown(Duration duration) {
        stopTinyCountdown();

        int[] secondsRemaining = {(int) Math.ceil(duration.toSeconds())};
        showTinyMessage(String.valueOf(secondsRemaining[0]));

        tinyCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsRemaining[0] = Math.max(0, secondsRemaining[0] - 1);
            if (secondsRemaining[0] > 0) {
                showTinyMessage(String.valueOf(secondsRemaining[0]));
                return;
            }

            stopTinyCountdown();
            hideTinyMessage();
        }));
        tinyCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        tinyCountdownTimeline.playFromStart();
    }

    public void hideTinyMessage() {
        stopTinyCountdown();
        stopTinyMessageTransition();

        tinyMessageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(tinyMessageCircle.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleXProperty(), 0.64, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleYProperty(), 0.64, Interpolator.EASE_BOTH))
        );
        tinyMessageTransition.setOnFinished(event -> {
            tinyMessageTransition = null;
            tinyMessageCircle.setVisible(false);
            tinyMessageCircle.setManaged(false);
        });
        tinyMessageTransition.playFromStart();
    }

    public void dispose() {
        hideTimer.stop();
        stopMessageTransition();
        stopTinyCountdown();
        stopTinyMessageTransition();
    }

    private void showMessage(String message) {
        hideTimer.stop();
        stopMessageTransition();

        messageLabel.setText(message);
        messageBox.setVisible(true);
        messageBox.setManaged(true);
        messageBox.applyCss();

        double targetHeight = Math.max(MIN_MESSAGE_HEIGHT, messageBox.prefHeight(-1));
        messageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageBox.minHeightProperty(), targetHeight, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.prefHeightProperty(), targetHeight, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.maxHeightProperty(), targetHeight, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateYProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        messageTransition.setOnFinished(event -> {
            messageTransition = null;
            hideTimer.playFromStart();
        });
        messageTransition.playFromStart();
    }

    private void hideMessage() {
        stopMessageTransition();

        messageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageBox.minHeightProperty(), COLLAPSED_SIZE, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.prefHeightProperty(), COLLAPSED_SIZE, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.maxHeightProperty(), COLLAPSED_SIZE, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateYProperty(), MESSAGE_START_OFFSET, Interpolator.EASE_BOTH))
        );
        messageTransition.setOnFinished(event -> {
            messageTransition = null;
            messageBox.setVisible(false);
            messageBox.setManaged(false);
        });
        messageTransition.playFromStart();
    }

    private void setMessageHeight(double height) {
        messageBox.setMinHeight(height);
        messageBox.setPrefHeight(height);
        messageBox.setMaxHeight(height);
    }

    private void stopMessageTransition() {
        if (messageTransition != null) {
            messageTransition.stop();
            messageTransition = null;
        }
    }

    private void stopTinyMessageTransition() {
        if (tinyMessageTransition != null) {
            tinyMessageTransition.stop();
            tinyMessageTransition = null;
        }
    }

    private void stopTinyCountdown() {
        if (tinyCountdownTimeline != null) {
            tinyCountdownTimeline.stop();
            tinyCountdownTimeline = null;
        }
    }
}
