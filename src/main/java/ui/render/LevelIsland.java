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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class LevelIsland {
    private static final String MESSAGE_BASE_CLASS = "level-island-message";
    private static final String MESSAGE_RADAR_CLASS = "level-island-message-radar";
    private static final String MESSAGE_SHIELD_CLASS = "level-island-message-shield";
    private static final String MESSAGE_BEACON_CLASS = "level-island-message-beacon";
    private static final String MESSAGE_ELIXIR_CLASS = "level-island-message-elixir";
    private static final String MESSAGE_KEY_CLASS = "level-island-message-key";
    private static final String MESSAGE_MINI_GAME_CLASS = "level-island-message-mini-game";
    private static final String MESSAGE_CHOICE_CLASS = "level-island-message-choice";
    private static final String MESSAGE_DEFAULT_CLASS = "level-island-message-default";
    private static final Duration ANIMATION_DURATION = Duration.millis(260);
    private static final Duration MESSAGE_DURATION = Duration.seconds(2.4);
    private static final double TITLE_SLOT_HEIGHT = 54.0;
    private static final double MIN_ISLAND_HEADER_WIDTH = 180.0;
    private static final double ISLAND_MESSAGE_WIDTH = 350.0;
    private static final double ISLAND_CHOICE_WIDTH = 430.0;
    private static final double ISLAND_HORIZONTAL_PADDING = 36.0;
    private static final double TITLE_HORIZONTAL_PADDING = 28.0;
    private static final double TIMER_GAP = 10.0;
    private static final double TIMER_CAPSULE_WIDTH = 132.0;
    private static final double TIMER_ICON_SIZE = 34.0;
    private static final Font TITLE_MEASURE_FONT = Font.font("System", FontWeight.SEMI_BOLD, 16.0);
    private static final double MESSAGE_START_OFFSET = -8.0;
    private static final double MESSAGE_BOX_HEIGHT = 42.0;
    private static final double MESSAGE_BOTTOM_PADDING = 15.0;
    private static final double MESSAGE_SLOT_HEIGHT = MESSAGE_BOX_HEIGHT + MESSAGE_BOTTOM_PADDING;
    private static final double CHOICE_MESSAGE_BOX_HEIGHT = 72.0;
    private static final double CHOICE_MESSAGE_SLOT_HEIGHT = CHOICE_MESSAGE_BOX_HEIGHT + MESSAGE_BOTTOM_PADDING;
    private static final double ACTION_SLOT_HEIGHT = 50.0;
    private static final double ACTION_START_OFFSET = -8.0;
    private static final double TINY_MESSAGE_SIZE = TITLE_SLOT_HEIGHT;
    private static final double TINY_MESSAGE_START_OFFSET = -18.0;

    private final String title;
    private final HBox view;
    private final HBox timerCapsule;
    private final VBox islandBase;
    private final StackPane titleSlot;
    private final HBox messageBox;
    private final VBox messageContent;
    private Node messageIcon;
    private final Label messageLabel;
    private final HBox crystalCountBadge;
    private final Label crystalCountLabel;
    private final StackPane messageSlot;
    private final StackPane actionSlot;
    private final HBox actionBox;
    private final Button secondaryActionButton;
    private final Button primaryActionButton;
    private final StackPane tinyMessageCircle;
    private final Label tinyMessageLabel;
    private final Label timerCapsuleValueLabel;
    private final PauseTransition hideTimer = new PauseTransition(MESSAGE_DURATION);
    private Timeline widthTransition;
    private Timeline messageTransition;
    private Timeline actionTransition;
    private Timeline tinyMessageTransition;
    private Timeline tinyCountdownTimeline;
    private Timeline damagePulseTransition;
    private final double headerWidth;
    private boolean choiceActive = false;

    public LevelIsland(String title) {
        this.title = title;
        this.headerWidth = computeHeaderWidth(title);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("pause-title-label");

        timerCapsuleValueLabel = new Label("00:00");
        timerCapsuleValueLabel.getStyleClass().add("level-island-header-timer-value");

        Label timerKickerLabel = new Label("RUN TIME");
        timerKickerLabel.getStyleClass().add("level-island-header-timer-kicker");

        Label timerIconLabel = new Label("⏱");
        timerIconLabel.getStyleClass().add("level-island-header-timer-icon");

        StackPane timerIconShell = new StackPane(timerIconLabel);
        timerIconShell.getStyleClass().add("level-island-header-timer-icon-shell");
        timerIconShell.setMinSize(TIMER_ICON_SIZE, TIMER_ICON_SIZE);
        timerIconShell.setPrefSize(TIMER_ICON_SIZE, TIMER_ICON_SIZE);
        timerIconShell.setMaxSize(TIMER_ICON_SIZE, TIMER_ICON_SIZE);

        VBox timerCopyBlock = new VBox(1, timerKickerLabel, timerCapsuleValueLabel);
        timerCopyBlock.setAlignment(Pos.CENTER_LEFT);

        timerCapsule = new HBox(8, timerIconShell, timerCopyBlock);
        timerCapsule.getStyleClass().addAll("game-hud", "level-island-header-timer");
        timerCapsule.setAlignment(Pos.CENTER_LEFT);
        timerCapsule.setPadding(new Insets(10, 16, 10, 12));
        timerCapsule.setMinHeight(TITLE_SLOT_HEIGHT);
        timerCapsule.setPrefHeight(TITLE_SLOT_HEIGHT);
        timerCapsule.setMaxHeight(TITLE_SLOT_HEIGHT);
        timerCapsule.setMinWidth(TIMER_CAPSULE_WIDTH);
        timerCapsule.setPrefWidth(TIMER_CAPSULE_WIDTH);
        timerCapsule.setMaxWidth(TIMER_CAPSULE_WIDTH);

        titleSlot = new StackPane(titleLabel);
        titleSlot.setAlignment(Pos.CENTER);
        titleSlot.setMinHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setPrefHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setMaxHeight(TITLE_SLOT_HEIGHT);
        titleSlot.setPickOnBounds(false);

        messageLabel = new Label();
        messageLabel.getStyleClass().add("level-island-message-text");
        messageLabel.setWrapText(true);

        crystalCountLabel = new Label();
        crystalCountLabel.getStyleClass().add("level-island-crystal-count-text");
        crystalCountBadge = new HBox(5, ArtifactVisuals.createHudIcon(ArtifactType.CRYSTAL, 15), crystalCountLabel);
        crystalCountBadge.getStyleClass().add("level-island-crystal-count");
        crystalCountBadge.setAlignment(Pos.CENTER);
        crystalCountBadge.setVisible(false);
        crystalCountBadge.setManaged(false);

        messageContent = new VBox(3, messageLabel, crystalCountBadge);
        messageContent.getStyleClass().add("level-island-choice-content");
        messageContent.setAlignment(Pos.CENTER);

        messageIcon = ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 18);
        messageBox = new HBox(8, messageIcon, messageContent);
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

        secondaryActionButton = new Button();
        secondaryActionButton.getStyleClass().addAll("level-island-action-button", "level-island-action-secondary");
        secondaryActionButton.setFocusTraversable(false);

        primaryActionButton = new Button();
        primaryActionButton.getStyleClass().addAll("level-island-action-button", "level-island-action-primary");
        primaryActionButton.setFocusTraversable(false);

        actionBox = new HBox(10, secondaryActionButton, primaryActionButton);
        actionBox.getStyleClass().add("level-island-actions");
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setOpacity(0);
        actionBox.setTranslateY(ACTION_START_OFFSET);

        actionSlot = new StackPane(actionBox);
        actionSlot.setAlignment(Pos.TOP_CENTER);
        actionSlot.setVisible(false);
        actionSlot.setManaged(false);
        setActionSlotHeight(0);
        actionSlot.setPickOnBounds(false);
        Rectangle actionSlotClip = new Rectangle();
        actionSlotClip.widthProperty().bind(actionSlot.widthProperty());
        actionSlotClip.heightProperty().bind(actionSlot.heightProperty());
        actionSlot.setClip(actionSlotClip);

        islandBase = new VBox(0, titleSlot, messageSlot, actionSlot);
        islandBase.getStyleClass().addAll("game-hud", "level-island");
        islandBase.setAlignment(Pos.TOP_CENTER);
        islandBase.setPadding(new Insets(0, 18, 0, 18));
        islandBase.setPickOnBounds(false);
        islandBase.setMaxWidth(Region.USE_PREF_SIZE);
        islandBase.setMaxHeight(Region.USE_PREF_SIZE);
        setIslandWidth(headerWidth);

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

        view = new HBox(TIMER_GAP, timerCapsule, islandBase, tinyMessageCircle);
        view.setAlignment(Pos.TOP_CENTER);
        view.setFillHeight(false);
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

    public void setTimerText(String timerText) {
        timerCapsuleValueLabel.setText(timerText);
    }

    public void showArtifactMessage(ArtifactType artifactType, String message) {
        if (choiceActive) {
            return;
        }
        hideActions();
        hideCrystalCount();
        setArtifactMessageStyle(artifactType);
        messageIcon = ArtifactVisuals.createHudIcon(artifactType, 18);
        messageBox.getChildren().set(0, messageIcon);
        messageIcon.setVisible(true);
        messageIcon.setManaged(true);
        showMessage(message, true, MESSAGE_BOX_HEIGHT, MESSAGE_SLOT_HEIGHT, ISLAND_MESSAGE_WIDTH);
    }

    public void showTextMessage(String message) {
        if (choiceActive) {
            return;
        }
        hideActions();
        hideCrystalCount();
        setDefaultMessageStyle();
        messageIcon.setVisible(false);
        messageIcon.setManaged(false);
        showMessage(message, true, MESSAGE_BOX_HEIGHT, MESSAGE_SLOT_HEIGHT, ISLAND_MESSAGE_WIDTH);
    }

    public void showPersistentTextMessage(String message) {
        if (choiceActive) {
            return;
        }
        hideActions();
        hideCrystalCount();
        setDefaultMessageStyle();
        messageIcon.setVisible(false);
        messageIcon.setManaged(false);
        showMessage(message, false, MESSAGE_BOX_HEIGHT, MESSAGE_SLOT_HEIGHT, ISLAND_MESSAGE_WIDTH);
    }

    public void hideTextMessage() {
        if (choiceActive) {
            return;
        }
        hideTimer.stop();
        hideActions();
        hideMessage();
    }

    public void hideChoiceMessage() {
        if (!choiceActive) {
            return;
        }
        closeChoiceMessage();
    }

    public void showChoiceMessage(
            ArtifactType artifactType,
            String message,
            String secondaryText,
            Runnable secondaryAction,
            String primaryText,
            Runnable primaryAction,
            int crystalCount
    ) {
        choiceActive = true;
        setArtifactMessageStyle(artifactType);
        messageBox.getStyleClass().add(MESSAGE_CHOICE_CLASS);
        messageIcon = ArtifactVisuals.createHudIcon(artifactType, 18);
        messageBox.getChildren().set(0, messageIcon);
        messageIcon.setVisible(true);
        messageIcon.setManaged(true);

        secondaryActionButton.setText(secondaryText);
        secondaryActionButton.setOnAction(event -> {
            closeChoiceMessage();
            secondaryAction.run();
        });
        primaryActionButton.setText(primaryText);
        primaryActionButton.setGraphic(ArtifactVisuals.createHudIcon(ArtifactType.CRYSTAL, 15));
        primaryActionButton.setGraphicTextGap(6);
        primaryActionButton.setOnAction(event -> {
            closeChoiceMessage();
            primaryAction.run();
        });
        crystalCountLabel.setText(String.valueOf(crystalCount));
        crystalCountBadge.setVisible(true);
        crystalCountBadge.setManaged(true);

        showMessage(message, false, CHOICE_MESSAGE_BOX_HEIGHT, CHOICE_MESSAGE_SLOT_HEIGHT, ISLAND_CHOICE_WIDTH);
        showActions();
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

    public void playDamagePulse() {
        stopDamagePulseTransition();

        DropShadow damageShadow = new DropShadow();
        damageShadow.setColor(Color.rgb(255, 86, 86, 0.0));
        damageShadow.setRadius(18);
        damageShadow.setSpread(0.08);
        damageShadow.setOffsetX(0);
        damageShadow.setOffsetY(0);
        islandBase.setEffect(damageShadow);

        damagePulseTransition = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(damageShadow.radiusProperty(), 18, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.spreadProperty(), 0.08, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.colorProperty(), Color.rgb(255, 86, 86, 0.0), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(220),
                        new KeyValue(damageShadow.radiusProperty(), 38, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.spreadProperty(), 0.26, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.colorProperty(), Color.rgb(255, 86, 86, 0.58), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(620),
                        new KeyValue(damageShadow.radiusProperty(), 22, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.spreadProperty(), 0.10, Interpolator.EASE_BOTH),
                        new KeyValue(damageShadow.colorProperty(), Color.rgb(255, 86, 86, 0.0), Interpolator.EASE_BOTH))
        );
        damagePulseTransition.setOnFinished(event -> {
            damagePulseTransition = null;
            islandBase.setEffect(null);
        });
        damagePulseTransition.playFromStart();
    }

    public void dispose() {
        hideTimer.stop();
        stopWidthTransition();
        stopMessageTransition();
        stopActionTransition();
        stopTinyCountdown();
        stopTinyMessageTransition();
        stopDamagePulseTransition();
    }

    private void showMessage(
            String message,
            boolean autoHide,
            double messageBoxHeight,
            double messageSlotHeight,
            double islandWidth
    ) {
        hideTimer.stop();
        stopMessageTransition();

        messageLabel.setText(message);
        setMessageBoxHeight(messageBoxHeight);
        animateIslandWidth(islandWidth);
        messageSlot.setVisible(true);
        messageSlot.setManaged(true);
        messageBox.setVisible(true);
        messageBox.applyCss();

        messageTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(messageSlot.minHeightProperty(), messageSlotHeight, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.prefHeightProperty(), messageSlotHeight, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.maxHeightProperty(), messageSlotHeight, Interpolator.EASE_BOTH),
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

    private void showActions() {
        stopActionTransition();

        actionSlot.setVisible(true);
        actionSlot.setManaged(true);
        animateIslandWidth(ISLAND_CHOICE_WIDTH);

        actionTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(actionSlot.minHeightProperty(), ACTION_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.prefHeightProperty(), ACTION_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.maxHeightProperty(), ACTION_SLOT_HEIGHT, Interpolator.EASE_BOTH),
                        new KeyValue(actionBox.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                        new KeyValue(actionBox.translateYProperty(), 0.0, Interpolator.EASE_BOTH))
        );
        actionTransition.setOnFinished(event -> actionTransition = null);
        actionTransition.playFromStart();
    }

    private void closeChoiceMessage() {
        choiceActive = false;
        messageBox.getStyleClass().remove(MESSAGE_CHOICE_CLASS);
        primaryActionButton.setGraphic(null);
        secondaryActionButton.setGraphic(null);
        hideCrystalCount();
        hideTimer.stop();
        hideActions();
        hideMessage();
    }

    private void setArtifactMessageStyle(ArtifactType artifactType) {
        clearMessageVariantStyles();
        messageBox.getStyleClass().add(switch (artifactType) {
            case RADAR -> MESSAGE_RADAR_CLASS;
            case SHIELD -> MESSAGE_SHIELD_CLASS;
            case BEACON -> MESSAGE_BEACON_CLASS;
            case ELIXIR -> MESSAGE_ELIXIR_CLASS;
            case KEY -> MESSAGE_KEY_CLASS;
            case MINI_GAME -> MESSAGE_MINI_GAME_CLASS;
            default -> MESSAGE_DEFAULT_CLASS;
        });
    }

    private void setDefaultMessageStyle() {
        clearMessageVariantStyles();
        messageBox.getStyleClass().add(MESSAGE_DEFAULT_CLASS);
    }

    private void hideCrystalCount() {
        crystalCountBadge.setVisible(false);
        crystalCountBadge.setManaged(false);
    }

    private void clearMessageVariantStyles() {
        messageBox.getStyleClass().removeAll(
                MESSAGE_RADAR_CLASS,
                MESSAGE_SHIELD_CLASS,
                MESSAGE_BEACON_CLASS,
                MESSAGE_ELIXIR_CLASS,
                MESSAGE_KEY_CLASS,
                MESSAGE_MINI_GAME_CLASS,
                MESSAGE_CHOICE_CLASS,
                MESSAGE_DEFAULT_CLASS
        );
    }

    private void hideActions() {
        if (!actionSlot.isVisible() && !actionSlot.isManaged()) {
            return;
        }

        stopActionTransition();
        actionTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(actionSlot.minHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.prefHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.maxHeightProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(actionBox.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(actionBox.translateYProperty(), ACTION_START_OFFSET, Interpolator.EASE_BOTH))
        );
        actionTransition.setOnFinished(event -> {
            actionTransition = null;
            actionSlot.setVisible(false);
            actionSlot.setManaged(false);
        });
        actionTransition.playFromStart();
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
            animateIslandWidth(headerWidth);
        });
        messageTransition.playFromStart();
    }

    private void animateIslandWidth(double width) {
        stopWidthTransition();

        width = getExpandedWidth(width);
        double contentWidth = getContentWidth(width);
        widthTransition = new Timeline(
                new KeyFrame(ANIMATION_DURATION,
                        new KeyValue(islandBase.minWidthProperty(), width, Interpolator.EASE_BOTH),
                        new KeyValue(islandBase.prefWidthProperty(), width, Interpolator.EASE_BOTH),
                        new KeyValue(islandBase.maxWidthProperty(), width, Interpolator.EASE_BOTH),
                        new KeyValue(titleSlot.minWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(titleSlot.prefWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(titleSlot.maxWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.minWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.prefWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageSlot.maxWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.minWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.prefWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(messageBox.maxWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.minWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.prefWidthProperty(), contentWidth, Interpolator.EASE_BOTH),
                        new KeyValue(actionSlot.maxWidthProperty(), contentWidth, Interpolator.EASE_BOTH))
        );
        widthTransition.setOnFinished(event -> widthTransition = null);
        widthTransition.playFromStart();
    }

    private void setIslandWidth(double width) {
        double contentWidth = getContentWidth(width);
        islandBase.setMinWidth(width);
        islandBase.setPrefWidth(width);
        islandBase.setMaxWidth(width);
        titleSlot.setMinWidth(contentWidth);
        titleSlot.setPrefWidth(contentWidth);
        titleSlot.setMaxWidth(contentWidth);
        messageSlot.setMinWidth(contentWidth);
        messageSlot.setPrefWidth(contentWidth);
        messageSlot.setMaxWidth(contentWidth);
        messageBox.setMinWidth(contentWidth);
        messageBox.setPrefWidth(contentWidth);
        messageBox.setMaxWidth(contentWidth);
        actionSlot.setMinWidth(contentWidth);
        actionSlot.setPrefWidth(contentWidth);
        actionSlot.setMaxWidth(contentWidth);
    }

    private double getContentWidth(double islandWidth) {
        return Math.max(0.0, islandWidth - ISLAND_HORIZONTAL_PADDING);
    }

    private double getExpandedWidth(double width) {
        return Math.max(headerWidth, width);
    }

    private static double computeHeaderWidth(String title) {
        Text titleText = new Text(title == null ? "" : title);
        titleText.setFont(TITLE_MEASURE_FONT);
        return Math.ceil(Math.max(
                MIN_ISLAND_HEADER_WIDTH,
                titleText.getLayoutBounds().getWidth() + ISLAND_HORIZONTAL_PADDING + TITLE_HORIZONTAL_PADDING
        ));
    }

    private void setMessageSlotHeight(double height) {
        messageSlot.setMinHeight(height);
        messageSlot.setPrefHeight(height);
        messageSlot.setMaxHeight(height);
    }

    private void setMessageBoxHeight(double height) {
        messageBox.setMinHeight(height);
        messageBox.setPrefHeight(height);
        messageBox.setMaxHeight(height);
    }

    private void setActionSlotHeight(double height) {
        actionSlot.setMinHeight(height);
        actionSlot.setPrefHeight(height);
        actionSlot.setMaxHeight(height);
    }

    private void stopMessageTransition() {
        if (messageTransition != null) {
            messageTransition.stop();
            messageTransition = null;
        }
    }

    private void stopWidthTransition() {
        if (widthTransition != null) {
            widthTransition.stop();
            widthTransition = null;
        }
    }

    private void stopActionTransition() {
        if (actionTransition != null) {
            actionTransition.stop();
            actionTransition = null;
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

    private void stopDamagePulseTransition() {
        if (damagePulseTransition != null) {
            damagePulseTransition.stop();
            damagePulseTransition = null;
        }
        islandBase.setEffect(null);
    }
}
