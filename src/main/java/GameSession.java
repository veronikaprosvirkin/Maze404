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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
import ui.input.InputHandler;
import ui.render.ArtifactVisuals;
import ui.render.GameAlerts;
import ui.render.GamePanel;
import ui.render.StartMenuView;

public class GameSession {
    private static final int PLAYER_MAX_HEALTH = 3;
    private static final Duration RADAR_REVEAL_DURATION = Duration.seconds(10);
    private static final Duration RADAR_WARNING_START_DELAY = Duration.seconds(7);
    private static final Duration RADAR_BLINK_INTERVAL = Duration.seconds(0.35);
    private static final Duration HUD_DAMAGE_HIGHLIGHT_DURATION = Duration.seconds(1);

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
        audioManager.setMasterVolume(settings.gameVolume());

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
        gamePanel.setGameVolume(settings.gameVolume());

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
        inventoryHud.setMouseTransparent(true);
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
        hudRow.setMouseTransparent(true);
        hudRow.setMaxWidth(Region.USE_PREF_SIZE);
        hudRow.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane.setAlignment(hudRow, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hudRow, new Insets(0, 0, 24, 0));

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
        winLoseOverlay.setVisible(false);
        winLoseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        StackPane.setAlignment(winLoseOverlay, Pos.CENTER);

        Label endGroupLabel = new Label();
        endGroupLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 36px; -fx-font-weight: bold;");

        Button returnToMenuBtn = new Button("Main Menu");
        returnToMenuBtn.getStyleClass().addAll("hud-card", "pause-menu-button", "pause-menu-secondary");
        attachPauseButtonHover(returnToMenuBtn, Color.rgb(255, 96, 96, 0.92));

        VBox endScreenBox = new VBox(24, endGroupLabel, returnToMenuBtn);
        endScreenBox.setAlignment(Pos.CENTER);

        winLoseOverlay.getChildren().add(endScreenBox);

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

        VBox pauseMenu = new VBox(14, pauseOverlayTitle, pausedLabel, resumeButton, exitButton);
        pauseMenu.getStyleClass().addAll("game-hud", "pause-overlay-panel");
        pauseMenu.setAlignment(Pos.CENTER);
        pauseMenu.setMaxWidth(Region.USE_PREF_SIZE);
        pauseMenu.setMaxHeight(Region.USE_PREF_SIZE);
        pauseMenu.setPadding(new Insets(24, 28, 24, 28));

        StackPane pauseOverlay = new StackPane(pauseMenu);
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

        InputHandler inputHandler = new InputHandler(action -> {
            if (action == ui.input.GameAction.TOGGLE_PAUSE) {
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
                    endGroupLabel.setText("GAME OVER");
                    endGroupLabel.setStyle("-fx-text-fill: #FF3333; -fx-font-size: 42px; -fx-font-weight: bold;");
                    winLoseOverlay.setVisible(true);
                } else if (gameState.isLevelComplete()) {
                    pauseController.resume();
                    pauseOverlay.setVisible(false);
                    endGroupLabel.setText("VICTORY!");
                    endGroupLabel.setStyle("-fx-text-fill: #33FF33; -fx-font-size: 42px; -fx-font-weight: bold;");
                    winLoseOverlay.setVisible(true);
                }
            });
        });
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
        exitButton.setOnAction(event -> {
            pauseController.resume();
            enemyTimer.stop();
            miniGameManager.dispose();
            exitToMenu.run();
        });
        returnToMenuBtn.setOnAction(event -> {
            pauseController.resume();
            enemyTimer.stop();
            miniGameManager.dispose();
            exitToMenu.run();
        });

        root.getChildren().setAll(gamePanel, levelTitleBox, pauseButtonWrapper, hudRow, pauseOverlay, winLoseOverlay);
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

    private String getGameStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/game-stone.css";
            case HARD -> "/styles/game-inferno.css";
            default -> "/styles/game-cryo.css";
        };
    }

}
