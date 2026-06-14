import enums.ArtifactType;
import enums.Difficulty;
import events.AudioManager;
import events.EventBus;
import events.GameEvent;
import gameHistory.VictoryHistoryManager;
import gameHistory.VictoryRecord;
import javafx.event.EventHandler;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
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
import ui.render.GameHud;
import ui.render.GamePanel;
import ui.render.LevelIsland;
import ui.render.StartMenuView;

import java.util.Locale;
import java.util.function.Consumer;

public class GameSession {
    private static final int PLAYER_MAX_HEALTH = 3;
    private static final String ICON_PATH = "/icons/";
    private static final Duration RADAR_REVEAL_DURATION = Duration.seconds(10);
    private static final Duration RADAR_WARNING_START_DELAY = Duration.seconds(7);
    private static final Duration RADAR_BLINK_INTERVAL = Duration.seconds(0.35);
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
        audioManager.playLevelMusic(Difficulty.current);

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

        GameHud gameHud = new GameHud(scene, Difficulty.current, player);

        LevelIsland levelIsland = new LevelIsland(getLevelTitle(Difficulty.current));
        levelIsland.setTimerText("00:00");
        StackPane.setAlignment(levelIsland.getView(), Pos.TOP_CENTER);
        StackPane.setMargin(levelIsland.getView(), new Insets(24, 0, 0, 0));
        HBox timerButton = levelIsland.getTimerView();
        timerButton.setMaxWidth(Region.USE_PREF_SIZE);
        timerButton.setMaxHeight(Region.USE_PREF_SIZE);
        timerButton.setPickOnBounds(true);
        StackPane.setAlignment(timerButton, Pos.TOP_LEFT);
        StackPane.setMargin(timerButton, new Insets(24, 0, 0, 24));
        miniGames.MiniGameManager miniGameManager = new miniGames.MiniGameManager(gameState, player, levelIsland);
        EventHandler<KeyEvent> miniGamePromptKeyHandler = event -> {
            if (miniGameManager.handlePromptKey(event.getCode())) {
                event.consume();
            }
        };
        scene.addEventFilter(KeyEvent.KEY_PRESSED, miniGamePromptKeyHandler);

        PauseTransition radarMistRestoreTimer = new PauseTransition(RADAR_REVEAL_DURATION);
        PauseTransition radarBlinkStartTimer = new PauseTransition(RADAR_WARNING_START_DELAY);
        Timeline radarBlinkTimeline = new Timeline(new KeyFrame(RADAR_BLINK_INTERVAL, event -> gameHud.toggleRadarWarning()));
        radarBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
        radarBlinkStartTimer.setOnFinished(event -> {
            gameHud.setRadarWarning(true);
            radarBlinkTimeline.playFromStart();
        });
        radarMistRestoreTimer.setOnFinished(event -> {
            gamePanel.setMistEnabled(mistEnabled);
            gamePanel.setRadarActive(false);
            radarBlinkStartTimer.stop();
            radarBlinkTimeline.stop();
            gameHud.setRadarActive(false);
            gameHud.setRadarWarning(false);
            levelIsland.hideTinyMessage();
        });

        boolean[] stealthHintVisible = {false};
        VBox bottomHudStack = gameHud.getView();
        StackPane.setAlignment(bottomHudStack, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bottomHudStack, new Insets(0, 0, 24, 0));

        StackPane winLoseOverlay = new StackPane();
        winLoseOverlay.getStyleClass().add("victory-overlay");
        winLoseOverlay.setVisible(false);
        StackPane.setAlignment(winLoseOverlay, Pos.CENTER);

        Label endBadgeLabel = new Label(levelIsland.getTitle());
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

        Label pauseOverlayTitle = new Label(levelIsland.getTitle());
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
                        0,
                        30,
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

        Consumer<GameEvent> exitBlockedListener = event -> Platform.runLater(() ->
                levelIsland.showArtifactMessage(ArtifactType.KEY, "Key required"));
        EventBus.getInstance().subscribe(GameEvent.Type.EXIT_BLOCKED, exitBlockedListener);

        Consumer<GameEvent> playerDamagedListener = event -> {
            Platform.runLater(() -> {
                gameHud.sync(player, levelIsland);
            });
        };
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DAMAGED, playerDamagedListener);

        Consumer<GameEvent> artifactCollectedListener = event -> Platform.runLater(() -> {
            gameHud.sync(player, levelIsland);
            gameHud.refreshShield(player);
            gamePanel.redraw(grid, player, enemies);
        });
        EventBus.getInstance().subscribe(GameEvent.Type.ARTIFACT_COLLECTED, artifactCollectedListener);

        Consumer<GameEvent> shieldActivatedListener = event -> Platform.runLater(() ->
                levelIsland.showArtifactMessage(ArtifactType.SHIELD, "Shield equipped"));
        EventBus.getInstance().subscribe(GameEvent.Type.SHIELD_ACTIVATED, shieldActivatedListener);

        Consumer<GameEvent> beaconActivatedListener = event -> Platform.runLater(() ->
                levelIsland.showArtifactMessage(ArtifactType.BEACON, "Beacon placed"));
        EventBus.getInstance().subscribe(GameEvent.Type.BEACON_ACTIVATED, beaconActivatedListener);

        Timeline stealthHintTimer = new Timeline(
                new KeyFrame(STEALTH_HINT_UPDATE_INTERVAL, event ->
                        updateStealthHint(levelIsland, player.isSemiInvisible(), stealthHintVisible))
        );
        stealthHintTimer.setCycleCount(Timeline.INDEFINITE);
        stealthHintTimer.play();

        Runnable[] showDefeatOverlayRef = new Runnable[1];

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
                        gameHud.setRadarActive(true);
                        gameHud.setRadarWarning(false);
                        radarBlinkStartTimer.playFromStart();
                        radarMistRestoreTimer.stop();
                        radarMistRestoreTimer.playFromStart();
                        levelIsland.showTinyCountdown(RADAR_REVEAL_DURATION);
                    }
                }
                case SHIELD -> shieldSystem.activateShield(gameState);
                case BEACON -> beaconSystem.placeBeacon(gameState);
                case ELIXIR -> {
                    int elixirsBeforeUse = player.getElixirCount();
                    int healthBeforeUse = player.getHealth();
                    useElixir(player);
                    if (player.getElixirCount() < elixirsBeforeUse && player.getHealth() > healthBeforeUse) {
                        levelIsland.showArtifactMessage(ArtifactType.ELIXIR, "Elixir used");
                    }
                }
                default -> {
                }
            }

            artifactSystem.processArtifacts(gameState);

            Platform.runLater(() -> {
                gameHud.sync(player, levelIsland);
                gameHud.refreshShield(player);

                if (gameState.isGameOver() || player.getHealth() <= 0) {
                    if (showDefeatOverlayRef[0] != null) {
                        showDefeatOverlayRef[0].run();
                    }
                } else if (gameState.isLevelComplete()) {
                    pauseController.resume();
                    pauseOverlay.setVisible(false);
                    winLoseOverlay.getStyleClass().remove("victory-defeat");
                    winLoseOverlay.getStyleClass().add("victory-complete");
                    endBadgeLabel.setText(levelIsland.getTitle());
                    successIcon.setVisible(true);
                    successIcon.setManaged(true);
                    defeatCrossLabel.setVisible(false);
                    defeatCrossLabel.setManaged(false);
                    endGroupLabel.setText(getCompletionTitle(Difficulty.current));

                    long totalSeconds = gameState.getTotalPlayTimeSeconds();

                    java.util.Map<String, Integer> artifactsMap = new java.util.HashMap<>();
                    artifactsMap.put("Crystals ◆", player.getCrystals());
                    artifactsMap.put("Shields 🛡", player.getShieldCount());
                    artifactsMap.put("Radars ⏱", player.getRadarCharges());
                    artifactsMap.put("Beacons ⌖", player.getBeaconCount());
                    artifactsMap.put("Elixirs 🧪", player.getElixirCount());

                    String currentDate = java.time.LocalDate.now().toString();
                    int currentLevelNum = (Difficulty.current == Difficulty.EASY) ? 1 : ((Difficulty.current == Difficulty.MEDIUM) ? 2 : 3);
                    int finalHealth = player.getHealth();

                    VictoryRecord newRecord = new VictoryRecord(currentLevelNum, totalSeconds, artifactsMap, currentDate, finalHealth);
                    VictoryHistoryManager.saveRecord(newRecord);
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    String timeText = String.format("%02d:%02d", minutes, seconds);

                    String originalSubtitle = getCompletionSubtitle(Difficulty.current);
                    endSubtitleLabel.setText(originalSubtitle + "\nClear Time: " + timeText);
                    retryLevelBtn.setManaged(false);
                    retryLevelBtn.setVisible(false);
                    returnToMenuBtn.setText("Exit");
                    winLoseOverlay.setVisible(true);
                }
            });
        };

        gameHud.attachArtifactActions(actionHandler);

        InputHandler inputHandler = new InputHandler(actionHandler);
        inputHandler.attachTo(scene);

        Timeline enemyTimer = enemyTurnScheduler.createTimer(gameState, grid, player);

        Timeline gameStopwatch = new Timeline(new KeyFrame(Duration.seconds(1), event -> {

            if (!gameState.isPaused() && !gameState.isGameOver() && !gameState.isLevelComplete()) {
                gameState.incrementPlayTime();
                levelIsland.setTimerText(formatTime(gameState.getTotalPlayTimeSeconds()));
            }
        }));
        gameStopwatch.setCycleCount(Timeline.INDEFINITE);
        gameStopwatch.play();

        enemyTimer.play();

        Runnable showDefeatOverlay = () -> {
            gameState.setGameOver(true);
            pauseController.resume();
            pauseOverlay.setVisible(false);
            enemyTimer.stop();
            winLoseOverlay.getStyleClass().remove("victory-complete");
            winLoseOverlay.getStyleClass().add("victory-defeat");
            endBadgeLabel.setText(levelIsland.getTitle());
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
        };
        showDefeatOverlayRef[0] = showDefeatOverlay;

        Consumer<GameEvent> playerDiedListener = event -> Platform.runLater(showDefeatOverlay);
        EventBus.getInstance().subscribe(GameEvent.Type.PLAYER_DIED, playerDiedListener);

        Runnable syncPauseUi = () -> {
            boolean paused = pauseController.isPaused();
            pauseOverlay.setVisible(paused);
            pauseOverlayTitle.setText(levelIsland.getTitle());
        };

        timerButton.setOnMouseClicked(event -> {
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
            gamePanel.dispose();
            gameHud.dispose();
            levelIsland.dispose();
            miniGameManager.dispose();
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, miniGamePromptKeyHandler);
            EventBus.getInstance().unsubscribe(GameEvent.Type.EXIT_BLOCKED, exitBlockedListener);
            EventBus.getInstance().unsubscribe(GameEvent.Type.PLAYER_DAMAGED, playerDamagedListener);
            EventBus.getInstance().unsubscribe(GameEvent.Type.ARTIFACT_COLLECTED, artifactCollectedListener);
            EventBus.getInstance().unsubscribe(GameEvent.Type.SHIELD_ACTIVATED, shieldActivatedListener);
            EventBus.getInstance().unsubscribe(GameEvent.Type.BEACON_ACTIVATED, beaconActivatedListener);
            EventBus.getInstance().unsubscribe(GameEvent.Type.PLAYER_DIED, playerDiedListener);
        };

        exitButton.setOnAction(event -> {
            closeSession.run();
            gameStopwatch.stop();
            exitToMenu.run();
        });
        retryLevelBtn.setOnAction(event -> {
            closeSession.run();
            start(settings);
        });
        returnToMenuBtn.setOnAction(event -> {
            closeSession.run();
            gameStopwatch.stop();
            exitToMenu.run();
        });

        root.getChildren().setAll(gamePanel, levelIsland.getView(), timerButton, bottomHudStack, pauseOverlay,
                winLoseOverlay);
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

    private static void useElixir(Player player) {
        if (player.getElixirCount() <= 0 || player.getHealth() >= PLAYER_MAX_HEALTH) {
            return;
        }

        player.useElixir();
        player.heal(1);
    }

    private static void updateStealthHint(LevelIsland levelIsland, boolean stealthActive, boolean[] stealthHintVisible) {
        if (stealthHintVisible[0] == stealthActive) {
            return;
        }

        stealthHintVisible[0] = stealthActive;
        if (stealthActive) {
            levelIsland.showPersistentTextMessage("STEALTH MODE ACTIVE");
            return;
        }

        levelIsland.hideTextMessage();
    }

    private String getGameStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/game-stone.css";
            case HARD -> "/styles/game-inferno.css";
            default -> "/styles/game-cryo.css";
        };
    }

    private static VBox createPauseSliderSetting(String labelText, double initialValue, java.util.function.DoubleConsumer onValueChanged) {
        return createPauseSliderSetting(labelText, initialValue, 0, 100, onValueChanged, GameSession::formatSliderValue);
    }

    private static VBox createPauseSliderSetting(
            String labelText,
            double initialValue,
            double minValue,
            double maxValue,
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

        Slider slider = new Slider(minValue, maxValue, initialValue);
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

    private static VBox createPauseSliderSetting(
            String labelText,
            double initialValue,
            java.util.function.DoubleConsumer onValueChanged,
            java.util.function.DoubleFunction<String> valueFormatter) {
        return createPauseSliderSetting(labelText, initialValue, 0, 100, onValueChanged, valueFormatter);
    }

    private static String formatSliderValue(double value) {
        return String.format(Locale.US, "%.0f%%", value);
    }

    private static String formatTime(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
