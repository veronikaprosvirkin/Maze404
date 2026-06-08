import enums.CellType;
import enums.Difficulty;
import enums.PlayerSkin;
import events.EventBus;
import events.GameEvent;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import logic.ArtifactSpawner;
import logic.ArtifactSystem;
import logic.PauseController;
import logic.generation.MazeGenerator;
import logic.system.BeaconSystem;
import logic.system.MovementSystem;
import logic.system.RadarSystem;
import logic.system.ShieldSystem;
import model.Artifact;
import model.GameState;
import model.Grid;
import model.Player;
import model.Position;
import enums.ArtifactType;
import ui.input.InputHandler;
import ui.render.ArtifactVisuals;
import ui.render.GamePanel;
import ui.render.StartMenuView;

import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Main JavaFX application. Signals when the UI is ready via a latch so
 * background threads can wait for the toolkit to be up and the primary stage shown.
 */
public class MainApp extends Application {
    private static final double MIN_WINDOW_WIDTH = 1024;
    private static final double MIN_WINDOW_HEIGHT = 680;
    private static final int PLAYER_MAX_HEALTH = 3;

    private static final CountDownLatch START_LATCH = new CountDownLatch(1);

    MovementSystem movementSystem = new MovementSystem();


    @SuppressWarnings("unused")
    public static void waitForStart() {
        try {
            START_LATCH.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1024, 680);
        showStartMenu(root, scene);
        primaryStage.setTitle("Maze404");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(MIN_WINDOW_HEIGHT);
        primaryStage.toFront();
        primaryStage.show();
        primaryStage.requestFocus();

        START_LATCH.countDown();
    }

    private void showStartMenu(StackPane root, Scene scene) {
        StartMenuView startMenu = new StartMenuView(
                settings -> startGame(root, scene, settings),
                Platform::exit
        );
        root.getChildren().setAll(startMenu);
        scene.getRoot().requestFocus();
    }

    private void startGame(StackPane root, Scene scene, StartMenuView.MenuSettings settings) {
        Difficulty.current = settings.difficulty() != null ? settings.difficulty() : Difficulty.EASY;

        String bgColor = switch (Difficulty.current) {
            case MEDIUM -> "#161210";
            case HARD -> "#150A0C";
            default -> "#111520";
        };
        root.setStyle("-fx-background-color: " + bgColor + ";");
        scene.getStylesheets().setAll(getClass().getResource(getGameStylesheet()).toExternalForm());

        Grid grid = new MazeGenerator().generate(21, 21);

        Player player = new Player(7, 7);
        player.setSkin(settings.playerSkin() != null ? settings.playerSkin() : PlayerSkin.CIRCLE);

        ArtifactSpawner artifactSpawner = new ArtifactSpawner();
        List<Artifact> artifacts = artifactSpawner.spawnArtifacts(
                grid,
                Difficulty.current,
                new Position(player.getRow(), player.getCol())
        );

        logic.EnemySpawner enemySpawner = new logic.EnemySpawner();
        List<model.Enemy> enemies = enemySpawner.spawnEnemies(grid, Difficulty.current, artifacts);

        GameState gameState = new GameState(grid, player, enemies, artifacts, 1);
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
        healthHud.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        healthHud.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        healthHud.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        healthHud.setPadding(new Insets(14, 18, 14, 18));
        healthHud.setSpacing(10);
        healthHud.getChildren().addAll(
                createHudCard("Health", createHealthIcon(), hpValueLabel, "health"),
                createHudCard("Crystals", ArtifactVisuals.createHudIcon(ArtifactType.CRYSTAL, 24), crystalsValueLabel, "crystals")
        );

        HBox inventoryHud = new HBox(10);
        inventoryHud.getStyleClass().add("game-hud");
        inventoryHud.setPickOnBounds(false);
        inventoryHud.setMouseTransparent(true);
        inventoryHud.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        inventoryHud.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        inventoryHud.setPrefHeight(javafx.scene.layout.Region.USE_COMPUTED_SIZE);
        inventoryHud.setPadding(new Insets(14, 18, 14, 18));

        VBox radarCard = createHudCard("Radar", ArtifactVisuals.createHudIcon(ArtifactType.RADAR, 24), radarValueLabel, "radar", "1");
        VBox shieldCard = createHudCard("Shield", ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 24), shieldValueLabel, "shield", "2");
        VBox beaconCard = createHudCard("Beacon", ArtifactVisuals.createHudIcon(ArtifactType.BEACON, 24), beaconValueLabel, "beacon", "3");
        VBox elixirCard = createHudCard("Elixir", ArtifactVisuals.createHudIcon(ArtifactType.ELIXIR, 24), elixirsValueLabel, "elixir", "4");
        inventoryHud.getChildren().addAll(radarCard, shieldCard, beaconCard, elixirCard);
        updateShieldHudState(shieldCard, player);

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
        hudRow.setMaxWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        hudRow.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
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

        // --- ОВЕРЛЕЙ ПЕРЕМОГИ / ПРОГРАШУ ---
        StackPane winLoseOverlay = new StackPane();
        winLoseOverlay.setVisible(false);
        winLoseOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        StackPane.setAlignment(winLoseOverlay, javafx.geometry.Pos.CENTER);

        javafx.scene.control.Label endGroupLabel = new javafx.scene.control.Label();
        endGroupLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 36px; -fx-font-weight: bold;");
        winLoseOverlay.getChildren().add(endGroupLabel);

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

        // --- ОБРОБНИК ВВОДУ (Рух гравця + оновлення створеного вище UI) ---4

        EventBus.getInstance().subscribe(GameEvent.Type.EXIT_BLOCKED, event -> {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Exit blocked");
                alert.setHeaderText(null);
                alert.setContentText("You need to find key!");
                alert.showAndWait();
            });
        });

        InputHandler inputHandler = new InputHandler(action -> {
            if (action == ui.input.GameAction.TOGGLE_PAUSE) {
                pauseController.toggle();
                pauseOverlay.setVisible(pauseController.isPaused());
                return;
            }

            if (gameState.isGameOver() || gameState.isLevelComplete() || gameState.isPaused()) return;

            /*
            int deltaRow = 0;
            int deltaCol = 0;
            switch (action) {
                case MOVE_UP -> deltaRow = -1;
                case MOVE_DOWN -> deltaRow = 1;
                case MOVE_LEFT -> deltaCol = -1;
                case MOVE_RIGHT -> deltaCol = 1;
                default -> { }
            }

            if (deltaRow != 0 || deltaCol != 0) {
                int targetRow = player.getRow() + deltaRow;
                int targetCol = player.getCol() + deltaCol;
                if (grid.isInBounds(targetRow, targetCol)
                        && grid.getCell(targetRow, targetCol).getType() != CellType.WALL) {

                    player.setRow(targetRow);
                    player.setCol(targetCol);
                    artifactSystem.processArtifacts(gameState);

                }
            }*/
            switch (action) {
                case MOVE_UP    -> movementSystem.movePlayer(gameState, -1,  0);
                case MOVE_DOWN  -> movementSystem.movePlayer(gameState,  1,  0);
                case MOVE_LEFT  -> movementSystem.movePlayer(gameState,  0, -1);
                case MOVE_RIGHT -> movementSystem.movePlayer(gameState,  0,  1);
                case RADAR      -> radarSystem.activateRadar(gameState);
                case SHIELD     -> shieldSystem.activateShield(gameState);
                case BEACON     -> beaconSystem.placeBeacon(gameState);
                case ELIXIR     -> useElixir(player);
                default -> { }
            }

            artifactSystem.processArtifacts(gameState);

            Platform.runLater(() -> {
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

        Timeline enemyTimer = getEnemyTimer(gameState, grid, player);
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
            showStartMenu(root, scene);
        });

        root.getChildren().setAll(gamePanel, levelTitleBox, pauseButtonWrapper, hudRow, pauseOverlay, winLoseOverlay);
        scene.getRoot().requestFocus();
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
        card.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
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

    private static void useElixir(Player player) {
        if (player.getElixirCount() <= 0 || player.getHealth() >= PLAYER_MAX_HEALTH) {
            return;
        }

        player.useElixir();
        player.heal(1);
    }

    private static void updateShieldHudState(VBox shieldCard, Player player) {
        boolean active = player.hasShield();
        if (active) {
            if (!shieldCard.getStyleClass().contains("hud-card-active")) {
                shieldCard.getStyleClass().add("hud-card-active");
            }
        } else {
            shieldCard.getStyleClass().remove("hud-card-active");
        }
    }

    private String getGameStylesheet() {
        return switch (Difficulty.current) {
            case MEDIUM -> "/styles/game-stone.css";
            case HARD -> "/styles/game-inferno.css";
            default -> "/styles/game-cryo.css";
        };
    }

    private static Timeline getEnemyTimer(GameState gameState, Grid grid, Player player) {
        int[] tickCounter = {0};

        Timeline enemyTimer = new Timeline(
                new KeyFrame(Duration.seconds(0.2), e -> {

                    if (gameState.isGameOver() || gameState.isLevelComplete() || gameState.isPaused()) return;

                    tickCounter[0]++;

                    for (model.Enemy enemy : gameState.getEnemies()) {
                        if (enemy.getAi() != null) {

                            int requiredTicks = 4;
                            if (enemy.getMode() == enums.EnemyMode.PATROL) {
                                requiredTicks = 5;
                            } else if (enemy.getMode() == enums.EnemyMode.CHASE) {
                                requiredTicks = switch (Difficulty.current) {
                                    case EASY -> 4;
                                    case MEDIUM -> 3;
                                    case HARD -> 2;
                                    default -> 3;
                                };
                            }

                            if (tickCounter[0] % requiredTicks != 0) {
                                continue;
                            }

                            Position next = enemy.getAi().computeNextMove(enemy, grid, player);
                            if (grid.isInBounds(next.getRow(), next.getCol()) &&
                                    grid.getCell(next.getRow(), next.getCol()).getType() != CellType.WALL) {
                                enemy.setRow(next.getRow());
                                enemy.setCol(next.getCol());
                            }
                        }
                    }
                })
        );
        enemyTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        return enemyTimer;
    }
}
