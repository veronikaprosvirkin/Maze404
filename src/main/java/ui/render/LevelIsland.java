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
    private static final String MESSAGE_BASE_CLASS = "level-island-message";
    private static final String MESSAGE_RADAR_CLASS = "level-island-message-radar";
    private static final String MESSAGE_SHIELD_CLASS = "level-island-message-shield";
    private static final String MESSAGE_BEACON_CLASS = "level-island-message-beacon";
    private static final String MESSAGE_ELIXIR_CLASS = "level-island-message-elixir";
    private static final String MESSAGE_DEFAULT_CLASS = "level-island-message-default";
    private static final Duration ANIMATION_DURATION = Duration.millis(260);
    private static final Duration MESSAGE_DURATION = Duration.seconds(2.4);
    private static final double TITLE_SLOT_HEIGHT = 54.0;
    private static final double MESSAGE_START_OFFSET = -8.0;
    private static final double MESSAGE_BOX_HEIGHT = 34.0;
    private static final double MESSAGE_BOTTOM_PADDING = 8.0;
    private static final double MESSAGE_SLOT_HEIGHT = MESSAGE_BOX_HEIGHT + MESSAGE_BOTTOM_PADDING;
    private static final double TINY_MESSAGE_SIZE = 42.0;
    private static final double TINY_MESSAGE_START_OFFSET = -18.0;

    private final String title;
    private final HBox view;
    private final VBox islandBase;
    private final StackPane messageSlot;
    private final HBox messageBox;
    private Node messageIcon;
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

        StackPane titleSlot = new StackPane(titleLabel);
        titleSlot.setAlignment(Pos.CENTER);
        titleSlot.setMinHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setPrefHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setMaxHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setPickOnBounds(false);

        messageLabel = new Label();
        messageLabel.getStyleClass().add("level-island-message-text");

        messageIcon = ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 18);
        messageBox = new HBox(8, messageIcon, messageLabel);
        messageBox.getStyleClass().addAll(MESSAGE_BASE_CLASS, MESSAGE_DEFAULT_CLASS);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setVisible(false);
        messageBox.setOpacity(0);
        messageBox.setTranslateY(MESSAGE_START_OFFSET);
        messageBox.setMinHeight(MESSAGE_BOX_HEIGHT);
        messageBox.setPrefHeight(MESSAGE_BOX_HEIGHT);
        messageBox.setMaxHeight(MESSAGE_BOX_HEIGHT);

        Rectangle clip = new Rectangle();
        clip.arcWidthProperty().set(24);
        clip.arcHeightProperty().set(24);
        clip.widthProperty().bind(messageBox.widthProperty());
        clip.heightProperty().bind(messageBox.heightProperty());
        messageBox.setClip(clip);

        messageSlot = new StackPane(messageBox);
        messageSlot.setAlignment(Pos.TOP_CENTER);
        messageSlot.setPadding(new Insets(0, 0, MESSAGE_BOTTOM_PADDING, 0));
        messageSlot.setVisible(false);
        messageSlot.setManaged(false);
        setMessageSlotHeight(0);
        messageSlot.setPickOnBounds(false);
        Rectangle messageSlotClip = new Rectangle();
        messageSlotClip.widthProperty().bind(messageSlot.widthProperty());
        messageSlotClip.heightProperty().bind(messageSlot.heightProperty());
        messageSlot.setClip(messageSlotClip);

        islandBase = new VBox(0, titleSlot, messageSlot);
        islandBase.getStyleClass().addAll("game-hud", "level-island");
        islandBase.setAlignment(Pos.TOP_CENTER);
        islandBase.setPadding(new Insets(0, 18, 0, 18));
        islandBase.setPickOnBounds(false);
        islandBase.setMaxWidth(Region.USE_PREF_SIZE);
        islandBase.setMaxHeight(Region.USE_PREF_SIZE);

        tinyMessageLabel = new Label();
        tinyMessageLabel.getStyleClass().add("level-island-tiny-text");
        tinyMessageCircle = new StackPane(tinyMessageLabel);
        tinyMessageCircle.getStyleClass().addAll("game-hud", "level-island-tiny");
        tinyMessageCircle.setAlignment(Pos.CENTER);
        tinyMessageCircle.setVisible(false);
        tinyMessageCircle.setOpacity(0);
        tinyMessageCircle.setTranslateX(TINY_MESSAGE_START_OFFSET);
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
        setArtifactMessageStyle(artifactType);
        messageIcon = ArtifactVisuals.createHudIcon(artifactType, 18);
        messageBox.getChildren().set(0, messageIcon);
        messageIcon.setVisible(true);
        messageIcon.setManaged(true);
        showMessage(message, true);
    }

    public void showTextMessage(String message) {
        setDefaultMessageStyle();
        messageIcon.setVisible(false);
        messageIcon.setManaged(false);
        showMessage(message, true);
    }

    public void showPersistentTextMessage(String message) {
        setDefaultMessageStyle();
        messageIcon.setVisible(false);
        messageIcon.setManaged(false);
        showMessage(message, false);
    }

    public void hideTextMessage() {
        hideTimer.stop();
        hideMessage();
    }

    public void showTinyMessage(String message) {
        stopTinyMessageTransition();

        tinyMessageLabel.setText(message);
        tinyMessageCircle.setVisible(true);

        tinyMessageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(tinyMessageCircle.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.translateXProperty(), 0.0, Interpolator.EASE_BOTH),
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
                        new KeyValue(tinyMessageCircle.translateXProperty(), TINY_MESSAGE_START_OFFSET, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleXProperty(), 0.64, Interpolator.EASE_BOTH),
                        new KeyValue(tinyMessageCircle.scaleYProperty(), 0.64, Interpolator.EASE_BOTH))
        );
        tinyMessageTransition.setOnFinished(event -> {
            tinyMessageTransition = null;
            tinyMessageCircle.setVisible(false);
        });
        tinyMessageTransition.playFromStart();
    }

    public void dispose() {
        hideTimer.stop();
        stopMessageTransition();
        stopTinyCountdown();
        stopTinyMessageTransition();
    }

    private void showMessage(String message, boolean autoHide) {
        hideTimer.stop();
        stopMessageTransition();

        messageLabel.setText(message);
        messageSlot.setVisible(true);
        messageSlot.setManaged(true);
        messageBox.setVisible(true);
        messageBox.applyCss();

        messageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageSlot.minHeightProperty(), MESSAGE_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.prefHeightProperty(), MESSAGE_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.maxHeightProperty(), MESSAGE_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateYProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        messageTransition.setOnFinished(event -> {
            messageTransition = null;
            if (autoHide) {
                hideTimer.playFromStart();
            }
        });
        messageTransition.playFromStart();
    }

    private void setArtifactMessageStyle(ArtifactType artifactType) {
        clearMessageVariantStyles();
        messageBox.getStyleClass().add(switch (artifactType) {
            case RADAR -> MESSAGE_RADAR_CLASS;
            case SHIELD -> MESSAGE_SHIELD_CLASS;
            case BEACON -> MESSAGE_BEACON_CLASS;
            case ELIXIR -> MESSAGE_ELIXIR_CLASS;
            default -> MESSAGE_DEFAULT_CLASS;
        });
    }

    private void setDefaultMessageStyle() {
        clearMessageVariantStyles();
        messageBox.getStyleClass().add(MESSAGE_DEFAULT_CLASS);
    }

    private void clearMessageVariantStyles() {
        messageBox.getStyleClass().removeAll(
                MESSAGE_RADAR_CLASS,
                MESSAGE_SHIELD_CLASS,
                MESSAGE_BEACON_CLASS,
                MESSAGE_ELIXIR_CLASS,
                MESSAGE_DEFAULT_CLASS
        );
    }

    private void hideMessage() {
        stopMessageTransition();

        messageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageSlot.minHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.prefHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.maxHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.translateYProperty(), MESSAGE_START_OFFSET, Interpolator.EASE_BOTH))
        );
        messageTransition.setOnFinished(event -> {
            messageTransition = null;
            messageBox.setVisible(false);
            messageSlot.setVisible(false);
            messageSlot.setManaged(false);
        });
        messageTransition.playFromStart();
    }

    private void setMessageSlotHeight(double height) {
        messageSlot.setMinHeight(height);
        messageSlot.setPrefHeight(height);
        messageSlot.setMaxHeight(height);
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
