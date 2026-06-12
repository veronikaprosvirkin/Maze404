import enums.ArtifactType;
import enums.Difficulty;
import events.AudioManager;
import events.EventBus;
import events.GameEvent;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import logic.ArtifactSystem;
import logic.PauseController;
import logic.system.BeaconSystem;
import logic.system.MovementSystem;
import logic.system.RadarSystem;
import logic.system.ShieldSystem;
import model.Artifact;
import model.GameState;
import model.Grid;
import model.Player;
import ui.input.GameAction;
import ui.input.InputHandler;
import ui.render.ArtifactVisuals;
import ui.render.GameAlerts;
import ui.render.GamePanel;
import ui.render.StartMenuView;

import java.util.Locale;

public class GameSession {
    private static final int PLAYER_MAX_HEALTH = 3;
    private static final String ICON_PATH = "/icons/";
    private static final Duration RADAR_REVEAL_DURATION = Duration.seconds(10);
    private static final Duration RADAR_WARNING_START_DELAY = Duration.seconds(7);
    private static final Duration RADAR_BLINK_INTERVAL = Duration.seconds(0.35);
    private static final Duration HUD_DAMAGE_HIGHLIGHT_DURATION = Duration.seconds(1);
    private static final Duration STEALTH_HINT_UPDATE_INTERVAL = Duration.millis(100);

    private final StackPane root;
    private final Scene scene;
    private final Runnable exitToMenu;
    private final AudioManager audioManager;
    private final MovementSystem movementSystem = new MovementSystem();
    private final GameLevelFactory gameLevelFactory = new GameLevelFactory();
    private final EnemyTurnScheduler enemyTurnScheduler = new EnemyTurnScheduler();

    public GameSession(StackPane root, Scene scene, Runnable exitToMenu, AudioManager audioManager) {
        this.root = root;
        this.scene = scene;
        this.exitToMenu = exitToMenu;
        this.audioManager = audioManager;
    }

    public void start(StartMenuView.MenuSettings settings) {
        Difficulty.current = settings.difficulty() != null ? settings.difficulty() : Difficulty.EASY;
        audioManager.setMusicVolume(settings.musicVolume());
        audioManager.setEffectsVolume(settings.soundEffectsVolume());
        audioManager.playLevelMusic();

        root.setStyle("-fx-background-color: " + getBackgroundColor(Difficulty.current) + ";");
        scene.getStylesheets().setAll(getClass().getResource(getGameStylesheet()).toExternalForm());

        GameLevelFactory.LevelContext level = gameLevelFactory.create(Difficulty.current, settings.playerSkin());
        Grid grid = level.grid();
        Player player = level.player();
        java.util.List<Artifact> artifacts = level.artifacts();
        java.util.List<model.Enemy> enemies = level.enemies();
        GameState gameState = level.gameState();
        PauseController pauseController = new PauseController(gameState);
        ArtifactSystem artifactSystem = new ArtifactSystem();
        miniGames.MiniGameManager miniGameManager = new miniGames.MiniGameManager(gameState, player);
        RadarSystem radarSystem = new RadarSystem();
        ShieldSystem shieldSystem = new ShieldSystem();
        BeaconSystem beaconSystem = new BeaconSystem();

        GamePanel gamePanel = new GamePanel(grid, player, artifacts, Difficulty.current, settings.mistSampleStep(), enemies);
        boolean mistEnabled = true;
        double mistAnimationTime = 2;
        double mistDensity = 1;
        gamePanel.setMistEnabled(mistEnabled);
        gamePanel.setMistAnimationTimeScale(mistAnimationTime);
        gamePanel.setMistDensity(mistDensity);
        gamePanel.setGameVolume(settings.soundEffectsVolume());

        Label hpValueLabel = createHudValueLabel();
        Label crystalsValueLabel = createHudValueLabel();
        Label radarValueLabel = createHudValueLabel();
        Label shieldValueLabel = createHudValueLabel();
        Label beaconValueLabel = createHudValueLabel();
        Label elixirsValueLabel = createHudValueLabel();
        Label keyValueLabel = createHudValueLabel();

        updateHudValues(
                player,
                hpValueLabel,
                crystalsValueLabel,
                radarValueLabel,
                shieldValueLabel,
                beaconValueLabel,
                elixirsValueLabel,
                keyValueLabel
        );

        HBox healthHud = new HBox();
        healthHud.getStyleClass().add("game-hud");
        healthHud.setPickOnBounds(false);
        healthHud.setMouseTransparent(true);
        healthHud.setMaxWidth(Region.USE_PREF_SIZE);
        healthHud.setMaxHeight(Region.USE_PREF_SIZE);
        healthHud.setPrefHeight(Region.USE_COMPUTED_SIZE);
        healthHud.setPadding(new Insets(14, 18, 14, 18));
        healthHud.setSpacing(10);
        VBox healthCard = createHudCard("Health", createHealthIcon(), hpValueLabel, "health");
        healthHud.getChildren().addAll(
                healthCard,
                createHudCard("Crystals", ArtifactVisuals.createHudIcon(ArtifactType.CRYSTAL, 24), crystalsValueLabel, "crystals")
        );

        HBox inventoryHud = new HBox(10);
        inventoryHud.getStyleClass().add("game-hud");
        inventoryHud.setPickOnBounds(false);
        inventoryHud.setMouseTransparent(false);
        inventoryHud.setMaxWidth(Region.USE_PREF_SIZE);
        inventoryHud.setMaxHeight(Region.USE_PREF_SIZE);
        inventoryHud.setPrefHeight(Region.USE_COMPUTED_SIZE);
        inventoryHud.setPadding(new Insets(14, 18, 14, 18));

        VBox radarCard = createHudCard("Radar", ArtifactVisuals.createHudIcon(ArtifactType.RADAR, 24), radarValueLabel, "radar", "1");
        VBox shieldCard = createHudCard("Shield", ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 24), shieldValueLabel, "shield", "2");
        VBox beaconCard = createHudCard("Beacon", ArtifactVisuals.createHudIcon(ArtifactType.BEACON, 24), beaconValueLabel, "beacon", "3");
        VBox elixirCard = createHudCard("Elixir", ArtifactVisuals.createHudIcon(ArtifactType.ELIXIR, 24), elixirsValueLabel, "elixir", "4");
        inventoryHud.getChildren().addAll(radarCard, shieldCard, beaconCard, elixirCard);
        updateShieldHudState(shieldCard, player);

        PauseTransition radarMistRestoreTimer = new PauseTransition(RADAR_REVEAL_DURATION);
        PauseTransition radarBlinkStartTimer = new PauseTransition(RADAR_WARNING_START_DELAY);
        Timeline radarBlinkTimeline = new Timeline(
                new KeyFrame(RADAR_BLINK_INTERVAL, event -> toggleHudCardState(radarCard, "hud-card-warning"))
        );
        radarBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
        radarBlinkStartTimer.setOnFinished(event -> {
            setHudCardState(radarCard, "hud-card-warning", true);
            radarBlinkTimeline.playFromStart();
        });
        radarMistRestoreTimer.setOnFinished(event -> {
            gamePanel.setMistEnabled(mistEnabled);
            gamePanel.setRadarActive(false);
            radarBlinkStartTimer.stop();
            radarBlinkTimeline.stop();
            setHudCardState(radarCard, "hud-card-active", false);
            setHudCardState(radarCard, "hud-card-warning", false);
        });

        PauseTransition healthDamageHighlightTimer = new PauseTransition(HUD_DAMAGE_HIGHLIGHT_DURATION);
        healthDamageHighlightTimer.setOnFinished(event -> setHudCardState(healthCard, "hud-card-active", false));
        int[] lastRenderedHealth = {player.getHealth()};
        boolean[] stealthHintVisible = {false};

        HBox hudRow = new HBox(12, healthHud, inventoryHud);
        if (Difficulty.current != Difficulty.EASY) {
            HBox keyHud = new HBox();
            keyHud.getStyleClass().add("game-hud");
            keyHud.setPickOnBounds(false);
            keyHud.setMouseTransparent(true);
            keyHud.setMaxWidth(Region.USE_PREF_SIZE);
            keyHud.setMaxHeight(Region.USE_PREF_SIZE);
            keyHud.setPrefHeight(Region.USE_COMPUTED_SIZE);
            keyHud.setPadding(new Insets(14, 18, 14, 18));
            keyHud.getChildren().add(createHudCard("Key", ArtifactVisuals.createHudIcon(ArtifactType.KEY, 24), keyValueLabel, "key"));
            hudRow.getChildren().add(keyHud);
        }

        hudRow.setPickOnBounds(false);
        hudRow.setMouseTransparent(false);
        hudRow.setMaxWidth(Region.USE_PREF_SIZE);
        hudRow.setMaxHeight(Region.USE_PREF_SIZE);

        Label levelTitle = new Label(getLevelTitle(Difficulty.current));
        levelTitle.getStyleClass().add("pause-title-label");
        HBox levelTitleBox = new HBox(levelTitle);
        levelTitleBox.getStyleClass().add("game-hud");
        levelTitleBox.setAlignment(Pos.CENTER);
        levelTitleBox.setPadding(new Insets(14, 22, 14, 22));
        levelTitleBox.setPickOnBounds(false);
        levelTitleBox.setMaxWidth(Region.USE_PREF_SIZE);
        levelTitleBox.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(levelTitleBox, Pos.TOP_CENTER);
        StackPane.setMargin(levelTitleBox, new Insets(24, 0, 0, 0));

        Label stealthHintLabel = new Label("STEALTH MODE ACTIVE");
        stealthHintLabel.getStyleClass().add("stealth-hint-text");
        stealthHintLabel.setManaged(true);
        stealthHintLabel.setVisible(true);
        stealthHintLabel.setMouseTransparent(true);
        HBox stealthHintBox = new HBox(stealthHintLabel);
        stealthHintBox.getStyleClass().addAll("game-hud", "stealth-hint");
        stealthHintBox.setManaged(false);
        stealthHintBox.setVisible(false);
        stealthHintBox.setMouseTransparent(true);
        stealthHintBox.setOpacity(1.0);
        stealthHintBox.setTranslateY(10.0);
        stealthHintBox.setAlignment(Pos.CENTER);
        stealthHintBox.setMaxWidth(Region.USE_PREF_SIZE);
        stealthHintBox.setMaxHeight(Region.USE_PREF_SIZE);

        VBox bottomHudStack = new VBox(10, stealthHintBox, hudRow);
        bottomHudStack.setPickOnBounds(false);
        bottomHudStack.setMouseTransparent(false);
        bottomHudStack.setAlignment(Pos.BOTTOM_CENTER);
        bottomHudStack.setMaxWidth(Region.USE_PREF_SIZE);
        bottomHudStack.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(bottomHudStack, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bottomHudStack, new Insets(0, 0, 24, 0));

        Button pauseButton = new Button("Pause");
        pauseButton.getStyleClass().addAll("hud-card", "pause-hud-button");
        pauseButton.setFocusTraversable(false);

        Label pauseHint = new Label("Esc");
        pauseHint.getStyleClass().add("hud-hotkey-badge");
        pauseHint.getStyleClass().add("pause-hotkey-badge");

        StackPane pauseButtonWrapper = new StackPane(pauseButton, pauseHint);
        pauseButtonWrapper.getStyleClass().addAll("game-hud", "pause-button-wrapper");
        pauseButtonWrapper.setPadding(new Insets(8, 8, 8, 8));
        pauseButtonWrapper.setMaxWidth(Region.USE_PREF_SIZE);
        pauseButtonWrapper.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(pauseButton, Pos.CENTER_LEFT);
        StackPane.setAlignment(pauseHint, Pos.BOTTOM_CENTER);
        pauseHint.setTranslateX(40);
        pauseHint.setTranslateY(-3);
        StackPane.setAlignment(pauseButtonWrapper, Pos.TOP_LEFT);
        StackPane.setMargin(pauseButtonWrapper, new Insets(24, 0, 0, 24));

        StackPane winLoseOverlay = new StackPane();
        winLoseOverlay.getStyleClass().add("victory-overlay");
        winLoseOverlay.setVisible(false);
        StackPane.setAlignment(winLoseOverlay, Pos.CENTER);

        Label endBadgeLabel = new Label(levelTitle.getText());
        endBadgeLabel.getStyleClass().add("victory-badge");

        ImageView successIcon = createMenuIcon("success.png", 42);
        Label defeatCrossLabel = new Label("×");
        defeatCrossLabel.getStyleClass().add("victory-icon-cross");
        defeatCrossLabel.setVisible(false);
        defeatCrossLabel.setManaged(false);
        StackPane endIconShell = new StackPane(successIcon, defeatCrossLabel);
        endIconShell.getStyleClass().add("victory-icon-shell");

        Label endGroupLabel = new Label("LEVEL COMPLETE");
        endGroupLabel.getStyleClass().add("victory-title");

        Label endSubtitleLabel = new Label("You found the exit and cleared the maze.");
        endSubtitleLabel.getStyleClass().add("victory-subtitle");
        endSubtitleLabel.setWrapText(true);
        endSubtitleLabel.setMaxWidth(320);
        endSubtitleLabel.setAlignment(Pos.CENTER);

        Button retryLevelBtn = new Button("Restart");
        retryLevelBtn.getStyleClass().addAll("hud-card", "pause-menu-button", "victory-retry-button");
        retryLevelBtn.setVisible(false);
        retryLevelBtn.setManaged(false);
        attachPauseButtonHover(retryLevelBtn, getPausePrimaryGlowColor(Difficulty.current));

        Button returnToMenuBtn = new Button("Exit");
        returnToMenuBtn.getStyleClass().addAll("hud-card", "pause-menu-button", "victory-exit-button");
        attachPauseButtonHover(returnToMenuBtn, getPauseSecondaryGlowColor(Difficulty.current));

        VBox endMenuButtons = new VBox(12, retryLevelBtn, returnToMenuBtn);
        endMenuButtons.setAlignment(Pos.CENTER);

        VBox endScreenBox = new VBox(18, endBadgeLabel, endIconShell, endGroupLabel, endSubtitleLabel, endMenuButtons);
        endScreenBox.getStyleClass().addAll("game-hud", "victory-panel");
        endScreenBox.setAlignment(Pos.CENTER);
        endScreenBox.setMaxWidth(Region.USE_PREF_SIZE);
        endScreenBox.setMaxHeight(Region.USE_PREF_SIZE);
        endScreenBox.setPadding(new Insets(26, 30, 26, 30));

        winLoseOverlay.getChildren().add(endScreenBox);

        int[] currentMistSampleStep = {settings.mistSampleStep()};

        Label pauseOverlayTitle = new Label(levelTitle.getText());
        pauseOverlayTitle.getStyleClass().add("pause-title-label");
        Label pausedLabel = new Label("Paused");
        pausedLabel.getStyleClass().add("pause-overlay-heading");

        Button resumeButton = new Button("Resume");
        resumeButton.getStyleClass().addAll("hud-card", "pause-menu-button", "pause-menu-primary");
        Button exitButton = new Button("Exit");
        exitButton.getStyleClass().addAll("hud-card", "pause-menu-button", "pause-menu-secondary");
        attachPauseButtonHover(resumeButton, getPausePrimaryGlowColor(Difficulty.current));
        attachPauseButtonHover(exitButton, Color.rgb(255, 96, 96, 0.92));

        Label settingsHeading = new Label("Settings");
        settingsHeading.getStyleClass().addAll("pause-overlay-heading", "pause-settings-heading");

        VBox settingsControls = new VBox(12,
                createPauseSliderSetting(
                        "Background music",
                        audioManager.getMusicVolume() * 100.0,
                        value -> {
                            audioManager.setMusicVolume(value / 100.0);
                        }),
                createPauseSliderSetting(
                        "Sound effects",
                        audioManager.getEffectsVolume() * 100.0,
                        value -> {
                            double normalizedValue = value / 100.0;
                            audioManager.setEffectsVolume(normalizedValue);
                            gamePanel.setGameVolume(normalizedValue);
                        }),
                createPauseSliderSetting(
                        "Mist quality",
                        30 - currentMistSampleStep[0],
                        value -> {
                            int mistSampleStep = Math.max(1, (int) Math.round(30 - value));
                            currentMistSampleStep[0] = mistSampleStep;
                            gamePanel.setMistSampleStep(mistSampleStep);
                        },
                        value -> String.format(Locale.US, "%.0f", value))
        );
        settingsControls.setAlignment(Pos.CENTER);

        VBox settingsFrame = new VBox(10, settingsHeading, settingsControls);
        settingsFrame.getStyleClass().add("pause-settings-frame");
        settingsFrame.setAlignment(Pos.CENTER);
        Rectangle settingsFrameClip = new Rectangle();
        settingsFrameClip.arcWidthProperty().set(20);
        settingsFrameClip.arcHeightProperty().set(20);
        settingsFrameClip.widthProperty().bind(settingsFrame.widthProperty());
        settingsFrameClip.heightProperty().bind(settingsFrame.heightProperty());
        settingsFrame.setClip(settingsFrameClip);

        VBox pausePanel = new VBox(14, pauseOverlayTitle, pausedLabel, resumeButton, exitButton, settingsFrame);
        pausePanel.getStyleClass().addAll("game-hud", "pause-overlay-panel");
        pausePanel.setAlignment(Pos.CENTER);
        pausePanel.setMaxWidth(Region.USE_PREF_SIZE);
        pausePanel.setMaxHeight(Region.USE_PREF_SIZE);
        pausePanel.setPadding(new Insets(24, 28, 24, 28));

        StackPane pauseOverlay = new StackPane(pausePanel);
        pauseOverlay.getStyleClass().add("pause-overlay");
        pauseOverlay.setVisible(false);

        EventBus.getInstance().subscribe(GameEvent.Type.EXIT_BLOCKED, event -> {
            Platform.runLater(GameAlerts::showExitBlockedAlert);
        });

        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DAMAGED, event -> {
            Platform.runLater(() -> {
                syncHudValues(
                        player,
                        hpValueLabel,
                        crystalsValueLabel,
                        radarValueLabel,
                        shieldValueLabel,
                        beaconValueLabel,
                        elixirsValueLabel,
                        keyValueLabel,
                        healthCard,
                        healthDamageHighlightTimer,
                        lastRenderedHealth
                );
            });
        });

        Timeline stealthHintTimer = new Timeline(
                new KeyFrame(STEALTH_HINT_UPDATE_INTERVAL, event ->
                        updateStealthHint(stealthHintBox, player.isSemiInvisible(), stealthHintVisible))
        );
        stealthHintTimer.setCycleCount(Timeline.INDEFINITE);
        stealthHintTimer.play();

        java.util.function.Consumer<GameAction> actionHandler = action -> {
            if (action == GameAction.TOGGLE_PAUSE) {
                pauseController.toggle();
                pauseOverlay.setVisible(pauseController.isPaused());
                return;
            }

            if (gameState.isGameOver() || gameState.isLevelComplete() || gameState.isPaused()) return;

            switch (action) {
                case MOVE_UP -> movementSystem.movePlayer(gameState, -1, 0);
                case MOVE_DOWN -> movementSystem.movePlayer(gameState, 1, 0);
                case MOVE_LEFT -> movementSystem.movePlayer(gameState, 0, -1);
                case MOVE_RIGHT -> movementSystem.movePlayer(gameState, 0, 1);
                case RADAR -> {
                    int radarChargesBeforeUse = player.getRadarCharges();
                    radarSystem.activateRadar(gameState);
                    if (player.getRadarCharges() < radarChargesBeforeUse) {
                        gamePanel.setMistEnabled(false);
                        gamePanel.setRadarActive(true);
                        radarBlinkStartTimer.stop();
                        radarBlinkTimeline.stop();
                        setHudCardState(radarCard, "hud-card-active", true);
                        setHudCardState(radarCard, "hud-card-warning", false);
                        radarBlinkStartTimer.playFromStart();
                        radarMistRestoreTimer.stop();
                        radarMistRestoreTimer.playFromStart();
                    }
                }
                case SHIELD -> shieldSystem.activateShield(gameState);
                case BEACON -> beaconSystem.placeBeacon(gameState);
                case ELIXIR -> useElixir(player);
                default -> {
                }
            }

            artifactSystem.processArtifacts(gameState);

            Platform.runLater(() -> {
                syncHudValues(
                        player,
                        hpValueLabel,
                        crystalsValueLabel,
                        radarValueLabel,
                        shieldValueLabel,
                        beaconValueLabel,
                        elixirsValueLabel,
                        keyValueLabel,
                        healthCard,
                        healthDamageHighlightTimer,
                        lastRenderedHealth
                );
                updateShieldHudState(shieldCard, player);

                if (gameState.isGameOver() || player.getHealth() <= 0) {
                    pauseController.resume();
                    pauseOverlay.setVisible(false);
                    winLoseOverlay.getStyleClass().remove("victory-complete");
                    winLoseOverlay.getStyleClass().add("victory-defeat");
                    endBadgeLabel.setText(levelTitle.getText());
                    successIcon.setVisible(false);
                    successIcon.setManaged(false);
                    defeatCrossLabel.setVisible(true);
                    defeatCrossLabel.setManaged(true);
                    endGroupLabel.setText("GAME OVER");
                    endSubtitleLabel.setText(getDefeatSubtitle(Difficulty.current));
                    retryLevelBtn.setManaged(true);
                    retryLevelBtn.setVisible(true);
                    returnToMenuBtn.setText("Exit");
                    winLoseOverlay.setVisible(true);
                } else if (gameState.isLevelComplete()) {
                    pauseController.resume();
                    pauseOverlay.setVisible(false);
                    winLoseOverlay.getStyleClass().remove("victory-defeat");
                    winLoseOverlay.getStyleClass().add("victory-complete");
                    endBadgeLabel.setText(levelTitle.getText());
                    successIcon.setVisible(true);
                    successIcon.setManaged(true);
                    defeatCrossLabel.setVisible(false);
                    defeatCrossLabel.setManaged(false);
                    endGroupLabel.setText(getCompletionTitle(Difficulty.current));
                    endSubtitleLabel.setText(getCompletionSubtitle(Difficulty.current));
                    retryLevelBtn.setManaged(false);
                    retryLevelBtn.setVisible(false);
                    returnToMenuBtn.setText("Exit");
                    winLoseOverlay.setVisible(true);
                }
            });
        };

        attachArtifactHudAction(radarCard, GameAction.RADAR, actionHandler, scene);
        attachArtifactHudAction(shieldCard, GameAction.SHIELD, actionHandler, scene);
        attachArtifactHudAction(beaconCard, GameAction.BEACON, actionHandler, scene);
        attachArtifactHudAction(elixirCard, GameAction.ELIXIR, actionHandler, scene);

        InputHandler inputHandler = new InputHandler(actionHandler);
        inputHandler.attachTo(scene);

        Timeline enemyTimer = enemyTurnScheduler.createTimer(gameState, grid, player);
        enemyTimer.play();

        Runnable syncPauseUi = () -> {
            boolean paused = pauseController.isPaused();
            pauseOverlay.setVisible(paused);
            pauseOverlayTitle.setText(levelTitle.getText());
        };

        pauseButton.setOnAction(event -> {
            pauseController.pause();
            syncPauseUi.run();
            scene.getRoot().requestFocus();
        });
        resumeButton.setOnAction(event -> {
            pauseController.resume();
            syncPauseUi.run();
            scene.getRoot().requestFocus();
        });
        Runnable closeSession = () -> {
            pauseController.resume();
            enemyTimer.stop();
            stealthHintTimer.stop();
            miniGameManager.dispose();
        };

        exitButton.setOnAction(event -> {
            closeSession.run();
            exitToMenu.run();
        });
        retryLevelBtn.setOnAction(event -> {
            closeSession.run();
            start(settings);
        });
        returnToMenuBtn.setOnAction(event -> {
            closeSession.run();
            exitToMenu.run();
        });

        root.getChildren().setAll(gamePanel, levelTitleBox, pauseButtonWrapper, bottomHudStack, pauseOverlay, winLoseOverlay);
        scene.getRoot().requestFocus();
    }

    private static String getBackgroundColor(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "#161210";
            case HARD -> "#150A0C";
            default -> "#111520";
        };
    }

    private static String getLevelTitle(Difficulty difficulty) {
        String levelNumber = switch (difficulty) {
            case MEDIUM -> "2";
            case HARD -> "3";
            default -> "1";
        };

        String difficultyLabel = switch (difficulty) {
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
            default -> "Easy";
        };
        String levelName = switch (difficulty) {
            case MEDIUM -> "Stone Desert";
            case HARD -> "Inferno Hell";
            default -> "Cryo Dungeon";
        };
        return "Level " + levelNumber + " • " + difficultyLabel + " • " + levelName;
    }

    private static void attachPauseButtonHover(Button button, Color glowColor) {
        DropShadow glow = new DropShadow();
        glow.setColor(glowColor);
        glow.setRadius(14);
        glow.setSpread(0.08);
        button.setEffect(glow);

        button.setOnMouseEntered(event -> animatePauseButton(button, glow, 24, 0.24, 1.03));
        button.setOnMouseExited(event -> animatePauseButton(button, glow, 14, 0.08, 1.0));
    }

    private static void animatePauseButton(Button button, DropShadow glow, double radius, double spread, double scale) {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(glow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                        new KeyValue(glow.spreadProperty(), spread, Interpolator.EASE_BOTH),
                        new KeyValue(button.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                        new KeyValue(button.scaleYProperty(), scale, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private static Color getPausePrimaryGlowColor(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> Color.rgb(240, 176, 48, 0.92);
            case HARD -> Color.rgb(240, 144, 64, 0.92);
            default -> Color.rgb(101, 242, 160, 0.92);
        };
    }

    private static Color getPauseSecondaryGlowColor(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> Color.rgb(196, 68, 42, 0.92);
            case HARD -> Color.rgb(255, 58, 42, 0.92);
            default -> Color.rgb(125, 228, 255, 0.90);
        };
    }

    private static String getCompletionTitle(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "STONE DESERT CLEARED";
            case HARD -> "INFERNO HELL CONQUERED";
            default -> "CRYO DUNGEON CLEARED";
        };
    }

    private static String getCompletionSubtitle(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "The dunes gave way. Level 2 is complete and the exit stands behind you.";
            case HARD -> "You survived the furnace. Level 3 is complete and the inferno has fallen silent.";
            default -> "The frozen maze is behind you. Level 1 is complete and the path is secure.";
        };
    }

    private static String getDefeatSubtitle(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> "The desert closed in around you. Retry the level or exit to the main menu.";
            case HARD -> "The inferno burned through this attempt. Retry the level or exit to the main menu.";
            default -> "The frozen maze outlasted this run. Retry the level or exit to the main menu.";
        };
    }

    private static ImageView createMenuIcon(String iconName, double iconSize) {
        Image image;
        try (var stream = GameSession.class.getResourceAsStream(ICON_PATH + iconName)) {
            image = stream != null ? new Image(stream) : null;
        } catch (Exception ignored) {
            image = null;
        }

        ImageView icon = new ImageView(image);
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        return icon;
    }

    private static VBox createHudCard(String title, Node iconNode, Label valueLabel, String accentStyleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("hud-card-title");
        Label hotkeyPlaceholder = new Label();
        hotkeyPlaceholder.getStyleClass().add("hud-hotkey-badge");
        hotkeyPlaceholder.setOpacity(0);
        return createHudCard(titleLabel, hotkeyPlaceholder, iconNode, valueLabel, accentStyleClass);
    }

    private static VBox createHudCard(String title, Node iconNode, Label valueLabel, String accentStyleClass, String hotkey) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("hud-card-title");

        Label hotkeyLabel = new Label(hotkey);
        hotkeyLabel.getStyleClass().add("hud-hotkey-badge");

        return createHudCard(titleLabel, hotkeyLabel, iconNode, valueLabel, accentStyleClass);
    }

    private static VBox createHudCard(
            Label titleLabel,
            Label hotkeyLabel,
            Node iconNode,
            Label valueLabel,
            String accentStyleClass
    ) {
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        StackPane titleRow = new StackPane(titleLabel, hotkeyLabel);
        StackPane.setAlignment(titleLabel, Pos.CENTER_LEFT);
        StackPane.setAlignment(hotkeyLabel, Pos.CENTER_RIGHT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        iconNode.getStyleClass().add("hud-card-icon");

        HBox valueRow = new HBox(8, iconNode, valueLabel);
        valueRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(5, titleRow, valueRow);
        card.getStyleClass().addAll("hud-card", "hud-" + accentStyleClass);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setMinWidth(96);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private static Label createHudValueLabel() {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("hud-card-value");
        return valueLabel;
    }

    private static Label createHealthIcon() {
        Label iconLabel = new Label("❤");
        iconLabel.getStyleClass().addAll("hud-card-icon", "hud-icon-health");
        return iconLabel;
    }

    private static void updateHudValues(
            Player player,
            Label hpValueLabel,
            Label crystalsValueLabel,
            Label radarValueLabel,
            Label shieldValueLabel,
            Label beaconValueLabel,
            Label elixirsValueLabel,
            Label keyValueLabel
    ) {
        hpValueLabel.setText(player.getHealth() + " / " + PLAYER_MAX_HEALTH);
        crystalsValueLabel.setText(String.valueOf(player.getCrystals()));
        radarValueLabel.setText(String.valueOf(player.getRadarCharges()));
        shieldValueLabel.setText(String.valueOf(player.getShieldCount()));
        beaconValueLabel.setText(String.valueOf(player.getBeaconCount()));
        elixirsValueLabel.setText(String.valueOf(player.getElixirCount()));
        keyValueLabel.setText(player.hasKey() ? "Found" : "Empty");
    }

    private static void syncHudValues(
            Player player,
            Label hpValueLabel,
            Label crystalsValueLabel,
            Label radarValueLabel,
            Label shieldValueLabel,
            Label beaconValueLabel,
            Label elixirsValueLabel,
            Label keyValueLabel,
            VBox healthCard,
            PauseTransition healthDamageHighlightTimer,
            int[] lastRenderedHealth
    ) {
        int currentHealth = player.getHealth();
        boolean healthChanged = currentHealth != lastRenderedHealth[0];
        lastRenderedHealth[0] = currentHealth;

        updateHudValues(
                player,
                hpValueLabel,
                crystalsValueLabel,
                radarValueLabel,
                shieldValueLabel,
                beaconValueLabel,
                elixirsValueLabel,
                keyValueLabel
        );

        if (healthChanged) {
            setHudCardState(healthCard, "hud-card-active", true);
            healthDamageHighlightTimer.stop();
            healthDamageHighlightTimer.playFromStart();
        }
    }

    private static void useElixir(Player player) {
        if (player.getElixirCount() <= 0 || player.getHealth() >= PLAYER_MAX_HEALTH) {
            return;
        }

        player.useElixir();
        player.heal(1);
    }

    private static void updateShieldHudState(VBox shieldCard, Player player) {
        boolean active = player.hasShield();
        setHudCardState(shieldCard, "hud-card-active", active);
    }

    private static void updateStealthHint(HBox stealthHintBox, boolean stealthActive, boolean[] stealthHintVisible) {
        if (stealthHintVisible[0] == stealthActive) {
            return;
        }

        stealthHintVisible[0] = stealthActive;
        stealthHintBox.setVisible(true);
        stealthHintBox.setManaged(true);

        Timeline transition = new Timeline(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(stealthHintBox.opacityProperty(), stealthActive ? 1.0 : 0.0, Interpolator.EASE_BOTH),
                        new KeyValue(stealthHintBox.translateYProperty(), stealthActive ? 0.0 : 10.0, Interpolator.EASE_BOTH))
        );
        transition.setOnFinished(event -> {
            if (!stealthActive) {
                stealthHintBox.setVisible(false);
                stealthHintBox.setManaged(false);
            }
        });
        transition.play();
    }

    private static void toggleHudCardState(VBox card, String styleClass) {
        setHudCardState(card, styleClass, !card.getStyleClass().contains(styleClass));
    }

    private static void setHudCardState(VBox card, String styleClass, boolean active) {
        if (active) {
            if (!card.getStyleClass().contains(styleClass)) {
                card.getStyleClass().add(styleClass);
            }
            return;
        }
        card.getStyleClass().remove(styleClass);
    }

    private static void attachArtifactHudAction(
            VBox card,
            GameAction action,
            java.util.function.Consumer<GameAction> actionHandler,
            Scene scene
    ) {
        card.setOnMouseClicked(event -> {
            actionHandler.accept(action);
            scene.getRoot().requestFocus();
        });
    }

    private String getGameStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/game-stone.css";
            case HARD -> "/styles/game-inferno.css";
            default -> "/styles/game-cryo.css";
        };
    }

    private static VBox createPauseSliderSetting(String labelText, double initialValue, java.util.function.DoubleConsumer onValueChanged) {
        return createPauseSliderSetting(labelText, initialValue, onValueChanged, GameSession::formatSliderValue);
    }

    private static VBox createPauseSliderSetting(
            String labelText,
            double initialValue,
            java.util.function.DoubleConsumer onValueChanged,
            java.util.function.DoubleFunction<String> valueFormatter) {
        Label label = new Label(labelText);
        label.getStyleClass().add("pause-settings-label");

        Label valueLabel = new Label(valueFormatter.apply(initialValue));
        valueLabel.getStyleClass().add("pause-settings-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(12, label, spacer, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        Slider slider = new Slider(0, 100, initialValue);
        slider.getStyleClass().add("pause-settings-slider");
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            valueLabel.setText(valueFormatter.apply(value));
            onValueChanged.accept(value);
        });

        VBox setting = new VBox(6, header, slider);
        setting.getStyleClass().add("pause-settings-row");
        setting.setMaxWidth(Double.MAX_VALUE);
        return setting;
    }

    private static String formatSliderValue(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }

}
