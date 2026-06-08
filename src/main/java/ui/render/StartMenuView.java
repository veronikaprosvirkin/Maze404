package ui.render;

import enums.Difficulty;
import enums.PlayerSkin;
import javafx.application.Platform;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import logic.generation.MazeGenerator;
import model.Grid;

import java.io.InputStream;
import java.util.Locale;
import java.util.function.Consumer;

public class StartMenuView extends StackPane {
    private static final String ICON_PATH = "/icons/";
    private static final String TITLE_FONT_PATH = "/fonts/DoctorGlitch.otf";
    private static final Color DEFAULT_MENU_GLOW = Color.web("#C9A7FF");
    private static final int TILE_SIZE = 32;
    private static final double BASE_WIDTH = 1024.0;
    private static final double BASE_HEIGHT = 576.0;
    private static final int MIST_SAMPLE_STEP = 6;
    private static final int DEFAULT_GAME_VOLUME = 75;
    private static final double SETTINGS_FRAME_WIDTH = 660.0;
    private static final double SETTINGS_CONTROL_WIDTH = 550.0;
    private static final double SETTINGS_SLIDER_WIDTH = 550.0;
    private static final double SKINS_FRAME_WIDTH = 660.0;
    private static final double LEVELS_FRAME_WIDTH = BASE_WIDTH * 0.80;
    private static final double LEVELS_FRAME_HEIGHT = BASE_HEIGHT * 0.80;
    private static final double LEVEL_CARD_WIDTH = LEVELS_FRAME_WIDTH * 0.68;
    private static final double LEVEL_CARD_HEIGHT = LEVELS_FRAME_HEIGHT * 0.70;
    private static final double MIST_ALPHA_CAP = 0.98;
    private static final double TITLE_FONT_SIZE = 62.0;
    private static final double LEVEL_SWITCH_ANIMATION_MS = 240.0;
    private static final double LEVEL_PALETTE_TRANSITION_MS = 420.0;
    private static final double LEVEL_HOVER_ANIMATION_MS = 190.0;
    private static final double SKIN_SWITCH_ANIMATION_MS = 240.0;
    private static final double SKIN_CARD_WIDTH = 150.0;
    private static final double SKIN_CURRENT_CARD_WIDTH = 150.0;
    private static final double SKIN_CARD_HEIGHT = 170.0;
    private static final double SKIN_GALLERY_GAP = 12.0;
    private static final double SKIN_CARD_STEP = (SKIN_CARD_WIDTH + SKIN_CURRENT_CARD_WIDTH) * 0.5 + SKIN_GALLERY_GAP;

    private final Canvas background = new Canvas();
    private final Canvas artifacts = new Canvas();
    private final Canvas mist = new Canvas();
    private final Grid menuMaze = new MazeGenerator().generate(19, 31);
    private GridRenderer gridRenderer = new GridRenderer(new SpriteSheet(Difficulty.MEDIUM));
    private GridRenderer previousGridRenderer = null;
    private final DoubleProperty paletteTransitionProgress = new SimpleDoubleProperty(1.0);
    private Timeline paletteTransition;
    private long mistTimeNanos = 0L;
    private long lastMistFrameNanos = 0L;
    private boolean mistMotionFrozen = false;
    private double mistFocusX = BASE_WIDTH * 0.5;
    private double mistFocusY = BASE_HEIGHT * 0.5;
    private double mistTargetX = BASE_WIDTH * 0.5;
    private double mistTargetY = BASE_HEIGHT * 0.5;
    private double gameVolume = DEFAULT_GAME_VOLUME;
    private double mistSampleStep = MIST_SAMPLE_STEP;
    private int currentSkinIndex = 0;
    private int currentLevelIndex = 0;
    private double backgroundBlend = 1.0;
    private MenuPalette currentMenuPalette = DEFAULT_MENU_PALETTE;
    private MenuPalette transitionStartPalette = DEFAULT_MENU_PALETTE;
    private MenuPalette transitionTargetPalette = DEFAULT_MENU_PALETTE;
    private MistProfile currentMistProfile = DEFAULT_MENU_MIST_PROFILE;
    private MistProfile transitionStartMistProfile = DEFAULT_MENU_MIST_PROFILE;
    private MistProfile transitionTargetMistProfile = DEFAULT_MENU_MIST_PROFILE;

    public record MenuSettings(
            double gameVolume,
            int mistSampleStep,
            PlayerSkin playerSkin,
            Difficulty difficulty) {
    }

    private record MenuArtifact(int row, int col, Color base, Color accent, double phase, boolean diamond) {
    }

    private record LevelChoice(Difficulty difficulty, String levelName, String styleClass) {
    }

    private record MenuPalette(
            Color bg,
            Color tint,
            Color vignette,
            Color text,
            Color textSoft,
            Color textMuted,
            Color accent,
            Color accentSoft,
            Color border,
            Color borderSoft,
            Color panelBg,
            Color cardBg,
            Color cardCurrentBg,
            Color buttonBg,
            Color buttonHoverBg,
            Color actionBg,
            Color actionHoverBg,
            Color glow,
            Difficulty backgroundDifficulty,
            MistProfile mistProfile) {
    }

    private record MazeViewport(double scale, double offsetX, double offsetY) {
    }

    private record MistProfile(
            double colorR,
            double colorG,
            double colorB,
            double accentR,
            double accentG,
            double accentB,
            double baseAlpha,
            double swirlAlpha,
            double driftSpeed,
            double flowSpeedX,
            double flowSpeedY,
            double pulseSpeed,
            double pulseStrength,
            double lateralSwing,
            double verticalSwing,
            double noiseScaleX,
            double noiseScaleY,
            double accentStrength) {
    }

    private static final MistProfile DEFAULT_MENU_MIST_PROFILE = new MistProfile(
            0.08, 0.03, 0.14,
            0.62, 0.36, 0.96,
            0.82, 0.28,
            1.05, 52.0, -14.0,
            1.12, 0.08,
            42.0, 18.0,
            0.016, 0.012,
            0.58);

    private static final MenuPalette DEFAULT_MENU_PALETTE = new MenuPalette(
            Color.web("#10071D"),
            Color.rgb(24, 8, 42, 0.18),
            Color.rgb(8, 3, 14, 0.16),
            Color.web("#F1E8FF"),
            Color.rgb(241, 232, 255, 0.74),
            Color.rgb(241, 232, 255, 0.68),
            Color.web("#C9A7FF"),
            Color.rgb(201, 167, 255, 0.86),
            Color.rgb(201, 167, 255, 0.88),
            Color.rgb(201, 167, 255, 0.28),
            Color.rgb(20, 8, 34, 0.72),
            Color.rgb(26, 10, 42, 0.36),
            Color.rgb(41, 18, 68, 0.52),
            Color.rgb(76, 38, 124, 0.58),
            Color.rgb(113, 62, 176, 0.72),
            Color.rgb(34, 16, 58, 0.42),
            Color.rgb(76, 38, 124, 0.58),
            DEFAULT_MENU_GLOW,
            Difficulty.MEDIUM,
            DEFAULT_MENU_MIST_PROFILE);

    private static final MenuArtifact[] MENU_ARTIFACTS = {
            new MenuArtifact(3, 5, Color.web("#F0D66A"), Color.web("#FFF3A6"), 0.0, true),
            new MenuArtifact(5, 20, Color.web("#7DE4FF"), Color.web("#D7FAFF"), 1.4, false),
            new MenuArtifact(9, 11, Color.web("#C46BFF"), Color.web("#F0C8FF"), 2.3, true),
            new MenuArtifact(12, 25, Color.web("#65F2A0"), Color.web("#D4FFE3"), 3.5, false),
            new MenuArtifact(15, 7, Color.web("#FF73B7"), Color.web("#FFD4EA"), 4.2, true)
    };

    private static final LevelChoice[] LEVEL_CHOICES = {
            new LevelChoice(Difficulty.EASY, "Cryo Dungeon", "cryo"),
            new LevelChoice(Difficulty.MEDIUM, "Stone Desert", "stone"),
            new LevelChoice(Difficulty.HARD, "Inferno Hell", "inferno")
    };

    public StartMenuView(Runnable onPlay, Runnable onSettings, Runnable onExit) {
        this(settings -> onPlay.run(), onExit);
    }

    public StartMenuView(Consumer<MenuSettings> onPlay, Runnable onExit) {
        getStyleClass().add("start-menu");
        var stylesheet = StartMenuView.class.getResource("/styles/start-menu.css");
        if (stylesheet != null) {
            getStylesheets().add(stylesheet.toExternalForm());
        }
        setStyle(createPaletteStyle(DEFAULT_MENU_PALETTE));
        paletteTransitionProgress.addListener((obs, oldValue, newValue) ->
                updatePaletteTransition(newValue.doubleValue()));

        DoubleBinding uiScale = Bindings.createDoubleBinding(
                () -> clamp(Math.min(getWidth() / BASE_WIDTH, getHeight() / BASE_HEIGHT), 0.38, 1.34),
                widthProperty(),
                heightProperty());

        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        getChildren().add(background);
        getChildren().add(createMenuOverlays());

        artifacts.widthProperty().bind(widthProperty());
        artifacts.heightProperty().bind(heightProperty());
        artifacts.setMouseTransparent(true);
        getChildren().add(artifacts);

        mist.widthProperty().bind(widthProperty());
        mist.heightProperty().bind(heightProperty());
        mist.setMouseTransparent(true);
        getChildren().add(mist);

        setOnMouseMoved(event -> {
            mistTargetX = event.getX();
            mistTargetY = event.getY();
        });
        setOnMouseExited(event -> {
            mistTargetX = getWidth() * 0.5;
            mistTargetY = getHeight() * 0.5;
        });

        StackPane contentFrame = new StackPane();
        contentFrame.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        contentFrame.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        contentFrame.setMaxSize(BASE_WIDTH, BASE_HEIGHT);
        contentFrame.scaleXProperty().bind(uiScale);
        contentFrame.scaleYProperty().bind(uiScale);

        contentFrame.getChildren().setAll(createMainMenuContent(onPlay, onExit, contentFrame));
        getChildren().add(contentFrame);

        widthProperty().addListener((obs, oldValue, newValue) -> drawBackground());
        heightProperty().addListener((obs, oldValue, newValue) -> drawBackground());

        AnimationTimer mistTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateMistFocus(now);
                drawMenuArtifacts(now);
                drawMist();
            }
        };
        mistTimer.start();
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                mistTimer.stop();
            } else {
                lastMistFrameNanos = 0L;
                mistTimer.start();
            }
        });
    }

    private VBox createMainMenuContent(Consumer<MenuSettings> onPlay, Runnable onExit, StackPane contentFrame) {
        VBox content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.setPadding(new Insets(74, 58, 24, 58));
        content.setSpacing(52);

        Text title = new Text("MAZE 404");
        title.getStyleClass().add("start-menu-title");
        Font titleFont = loadTitleFont(TITLE_FONT_SIZE);
        if (titleFont != null) {
            title.setFont(titleFont);
        }
        title.setEffect(new DropShadow(24, DEFAULT_MENU_GLOW));

        StackPane menuSlot = new StackPane();
        menuSlot.setMinWidth(760);
        menuSlot.setPrefSize(LEVELS_FRAME_WIDTH, LEVELS_FRAME_HEIGHT);
        menuSlot.setMaxSize(LEVELS_FRAME_WIDTH, LEVELS_FRAME_HEIGHT);
        menuSlot.getChildren().setAll(createMainMenuActions(onPlay, onExit, menuSlot, contentFrame));

        content.getChildren().addAll(title, menuSlot);
        return content;
    }

    private HBox createMainMenuActions(
            Consumer<MenuSettings> onPlay,
            Runnable onExit,
            StackPane menuSlot,
            StackPane contentFrame) {
        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER);
        actions.setSpacing(86);
        actions.setMinWidth(688);
        actions.setPrefWidth(688);
        actions.setMaxWidth(688);

        actions.getChildren().addAll(
                createMenuAction("Settings", "settings.png", () -> menuSlot.getChildren().setAll(
                        createSettingsFrame(() -> menuSlot.getChildren().setAll(
                                createMainMenuActions(onPlay, onExit, menuSlot, contentFrame)))),
                        false),
                createMenuAction("Play", "play.png", () -> contentFrame.getChildren().setAll(
                        createLevelsFrame(
                                () -> onPlay.accept(getMenuSettings()),
                                () -> contentFrame.getChildren().setAll(
                                        createMainMenuContent(onPlay, onExit, contentFrame)))),
                        true),
                createMenuAction("Skins", "skins.png", () -> menuSlot.getChildren().setAll(
                        createSkinsFrame(() -> menuSlot.getChildren().setAll(
                                createMainMenuActions(onPlay, onExit, menuSlot, contentFrame)))),
                        false));
        return actions;
    }

    private StackPane createLevelsFrame(Runnable onSelectLevel, Runnable onBack) {
        applySelectedLevelPalette();

        StackPane levelFrame = new StackPane();
        levelFrame.getStyleClass().add("levels-frame");
        levelFrame.setMinSize(BASE_WIDTH, BASE_HEIGHT);
        levelFrame.setPrefSize(BASE_WIDTH, BASE_HEIGHT);
        levelFrame.setMaxSize(BASE_WIDTH, BASE_HEIGHT);

        HBox levelSwitcher = new HBox(28);
        levelSwitcher.setAlignment(Pos.CENTER);
        levelSwitcher.getStyleClass().add("levels-switcher");

        Button leftButton = createLevelNavButton("left.png", -1, levelSwitcher, onSelectLevel);
        Button rightButton = createLevelNavButton("right.png", 1, levelSwitcher, onSelectLevel);
        leftButton.setViewOrder(-1);
        rightButton.setViewOrder(-1);
        levelSwitcher.getChildren().setAll(
                leftButton,
                createLevelCard(getSelectedLevel(), onSelectLevel),
                rightButton);

        Button backButton = new Button("");
        backButton.getStyleClass().addAll("settings-exit-button", "levels-back-button");
        backButton.setGraphic(createIcon("back.png", 22));
        backButton.setOnAction(event -> {
            applyDefaultMenuPalette();
            onBack.run();
        });

        levelFrame.setFocusTraversable(true);
        levelFrame.setOnMouseClicked(event -> levelFrame.requestFocus());
        levelFrame.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT) {
                animateLevelSwitch(levelSwitcher, -1, onSelectLevel);
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                animateLevelSwitch(levelSwitcher, 1, onSelectLevel);
                event.consume();
            } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                onSelectLevel.run();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.BACK_SPACE) {
                backButton.fire();
                event.consume();
            }
        });
        Platform.runLater(levelFrame::requestFocus);

        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(0, 0, 0, 24));
        levelFrame.getChildren().addAll(levelSwitcher, backButton);
        return levelFrame;
    }

    private VBox createSettingsFrame(Runnable onExitSettings) {
        VBox settingsFrame = new VBox(24);
        settingsFrame.getStyleClass().add("settings-frame");
        settingsFrame.setAlignment(Pos.CENTER);
        settingsFrame.setMinWidth(SETTINGS_FRAME_WIDTH);
        settingsFrame.setPrefWidth(SETTINGS_FRAME_WIDTH);
        settingsFrame.setMaxWidth(SETTINGS_FRAME_WIDTH);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(60);
        clip.setArcHeight(60);
        clip.widthProperty().bind(settingsFrame.widthProperty());
        clip.heightProperty().bind(settingsFrame.heightProperty());
        settingsFrame.setClip(clip);

        Text heading = new Text("SETTINGS");
        heading.getStyleClass().add("settings-heading");
        heading.setEffect(new DropShadow(14, DEFAULT_MENU_GLOW));

        VBox controls = new VBox(18);
        controls.setAlignment(Pos.CENTER);
        controls.setPrefWidth(SETTINGS_CONTROL_WIDTH);
        controls.setMaxWidth(SETTINGS_CONTROL_WIDTH);
        controls.getChildren().addAll(
                createSliderSetting(
                        "Game volume",
                        0,
                        100,
                        gameVolume,
                        value -> Math.round(value) +"",
                        value -> gameVolume = value),
                createSliderSetting(
                        "Mist quality",
                        1,
                        30,
                        30-mistSampleStep,
                        value -> Math.round(value) +"",
                        value -> mistSampleStep = 30-value));

        Button exitButton = new Button("Back");
        exitButton.getStyleClass().add("settings-exit-button");
        exitButton.setGraphic(createIcon("back.png", 22));
        exitButton.setOnAction(event -> onExitSettings.run());

        settingsFrame.getChildren().addAll(heading, controls, exitButton);
        return settingsFrame;
    }

    private VBox createSkinsFrame(Runnable onBack) {
        VBox skinsFrame = new VBox(18);
        skinsFrame.getStyleClass().add("settings-frame");
        skinsFrame.getStyleClass().add("skins-frame");
        skinsFrame.setAlignment(Pos.CENTER);
        skinsFrame.setMinWidth(SKINS_FRAME_WIDTH);
        skinsFrame.setPrefWidth(SKINS_FRAME_WIDTH);
        skinsFrame.setMaxWidth(SKINS_FRAME_WIDTH);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(60);
        clip.setArcHeight(60);
        clip.widthProperty().bind(skinsFrame.widthProperty());
        clip.heightProperty().bind(skinsFrame.heightProperty());
        skinsFrame.setClip(clip);

        Text heading = new Text("SKINS");
        heading.getStyleClass().add("settings-heading");
        heading.setEffect(new DropShadow(14, DEFAULT_MENU_GLOW));

        Text helper = new Text("Center skin is active");
        helper.getStyleClass().add("skins-helper");

        HBox gallery = new HBox(12);
        gallery.getStyleClass().add("skins-gallery");
        gallery.setAlignment(Pos.CENTER);

        Button leftButton = createGalleryNavButton("left.png", -1, gallery);
        Button rightButton = createGalleryNavButton("right.png", 1, gallery);
        gallery.getChildren().setAll(
                leftButton,
                createSkinPreviewCard(getRelativeSkin(-1), false),
                createSkinPreviewCard(getSelectedSkin(), true),
                createSkinPreviewCard(getRelativeSkin(1), false),
                rightButton
        );

        Button backButton = new Button("Select");
        backButton.getStyleClass().add("settings-exit-button");
        backButton.setGraphic(createIcon("select.png", 22));
        backButton.setOnAction(event -> onBack.run());

        skinsFrame.getChildren().addAll(heading, helper, gallery, backButton);
        return skinsFrame;
    }

    private VBox createSliderSetting(
            String name,
            double min,
            double max,
            double initialValue,
            java.util.function.DoubleFunction<String> valueFormatter,
            java.util.function.DoubleConsumer onValueChanged) {
        VBox setting = new VBox(8);
        setting.getStyleClass().add("settings-row");
        setting.setAlignment(Pos.TOP_CENTER);
        setting.setMaxWidth(SETTINGS_CONTROL_WIDTH);

        HBox labels = new HBox();
        labels.setAlignment(Pos.CENTER_LEFT);
        labels.setPrefWidth(SETTINGS_CONTROL_WIDTH);
        labels.setMaxWidth(SETTINGS_CONTROL_WIDTH);

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("settings-label");

        Label valueLabel = new Label(valueFormatter.apply(initialValue));
        valueLabel.getStyleClass().add("settings-value");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        labels.getChildren().addAll(nameLabel, spacer, valueLabel);

        Slider slider = new Slider(min, max, initialValue);
        slider.getStyleClass().add("settings-slider");
        slider.setPrefWidth(SETTINGS_SLIDER_WIDTH);
        slider.setMaxWidth(SETTINGS_SLIDER_WIDTH);
        slider.setMinWidth(SETTINGS_SLIDER_WIDTH);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setSnapToTicks(false);
        if (max - min <= 10 && Math.rint(min) == min && Math.rint(max) == max) {
            slider.setMajorTickUnit(1);
            slider.setMinorTickCount(0);
            slider.setBlockIncrement(1);
            slider.setSnapToTicks(true);
        }
        slider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            onValueChanged.accept(value);
            valueLabel.setText(valueFormatter.apply(value));
        });

        setting.getChildren().addAll(labels, slider);
        return setting;
    }

    private Button createGalleryNavButton(String iconName, int direction, HBox gallery) {
        Button button = new Button();
        button.getStyleClass().add("skins-nav-button");
        button.setGraphic(createIcon(iconName, 28));
        button.setOnAction(event -> animateSkinGallerySwitch(gallery, direction));
        return button;
    }

    private void animateSkinGallerySwitch(HBox gallery, int direction) {
        if (Boolean.TRUE.equals(gallery.getProperties().get("skinSwitching"))
                || gallery.getChildren().size() < 5) {
            return;
        }

        gallery.getProperties().put("skinSwitching", true);
        Node leftCard = gallery.getChildren().get(1);
        Node currentCard = gallery.getChildren().get(2);
        Node rightCard = gallery.getChildren().get(3);

        Node incomingCard = direction > 0 ? rightCard : leftCard;
        Node disappearingCard = direction > 0 ? leftCard : rightCard;
        double sideShift = direction > 0 ? -SKIN_CARD_STEP : SKIN_CARD_STEP;
        double disappearingShift = direction > 0 ? -SKIN_CARD_STEP * 0.62 : SKIN_CARD_STEP * 0.62;
        double centerToSideScale = SKIN_CARD_WIDTH / SKIN_CURRENT_CARD_WIDTH;
        double sideToCenterScale = SKIN_CURRENT_CARD_WIDTH / SKIN_CARD_WIDTH;

        Timeline animation = new Timeline(new KeyFrame(
                Duration.millis(SKIN_SWITCH_ANIMATION_MS),
                new KeyValue(incomingCard.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                new KeyValue(incomingCard.translateXProperty(), sideShift, Interpolator.EASE_BOTH),
                new KeyValue(incomingCard.scaleXProperty(), sideToCenterScale, Interpolator.EASE_BOTH),
                new KeyValue(incomingCard.scaleYProperty(), sideToCenterScale, Interpolator.EASE_BOTH),
                new KeyValue(currentCard.opacityProperty(), 0.92, Interpolator.EASE_BOTH),
                new KeyValue(currentCard.translateXProperty(), sideShift, Interpolator.EASE_BOTH),
                new KeyValue(currentCard.scaleXProperty(), centerToSideScale, Interpolator.EASE_BOTH),
                new KeyValue(currentCard.scaleYProperty(), centerToSideScale, Interpolator.EASE_BOTH),
                new KeyValue(disappearingCard.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(disappearingCard.translateXProperty(), disappearingShift, Interpolator.EASE_BOTH)));

        animation.setOnFinished(event -> {
            currentSkinIndex = wrapSkinIndex(currentSkinIndex + direction);
            gallery.getChildren().set(1, createSkinPreviewCard(getRelativeSkin(-1), false));
            gallery.getChildren().set(2, createSkinPreviewCard(getSelectedSkin(), true));
            gallery.getChildren().set(3, createSkinPreviewCard(getRelativeSkin(1), false));
            gallery.getProperties().remove("skinSwitching");
        });

        animation.play();
    }

    private Button createLevelNavButton(
            String iconName,
            int direction,
            HBox levelSwitcher,
            Runnable onSelectLevel) {
        Button button = new Button();
        button.getStyleClass().add("levels-nav-button");
        button.setGraphic(createIcon(iconName, 34));
        DropShadow buttonShadow = new DropShadow(24, currentMenuPalette.glow());
        button.setEffect(buttonShadow);
        button.setOnAction(event -> animateLevelSwitch(levelSwitcher, direction, onSelectLevel));
        button.setOnMouseEntered(event -> animateLevelHover(button, buttonShadow, 1.06, 32, 0.22, null, 1.0));
        button.setOnMouseExited(event -> animateLevelHover(button, buttonShadow, 1.0, 24, 0.10, null, 1.0));
        return button;
    }

    private void animateLevelSwitch(HBox levelSwitcher, int direction, Runnable onSelectLevel) {
        if (Boolean.TRUE.equals(levelSwitcher.getProperties().get("levelSwitching"))
                || levelSwitcher.getChildren().size() < 3) {
            return;
        }

        levelSwitcher.getProperties().put("levelSwitching", true);
        Node currentCard = levelSwitcher.getChildren().get(1);
        double travel = 74.0;
        double outgoingEnd = direction > 0 ? -travel : travel;
        double incomingStart = direction > 0 ? travel : -travel;

        Timeline slideOut = new Timeline(new KeyFrame(
                Duration.millis(LEVEL_SWITCH_ANIMATION_MS * 0.45),
                new KeyValue(currentCard.opacityProperty(), 0.0, Interpolator.EASE_BOTH),
                new KeyValue(currentCard.translateXProperty(), outgoingEnd, Interpolator.EASE_BOTH)));

        slideOut.setOnFinished(event -> {
            currentLevelIndex = wrapLevelIndex(currentLevelIndex + direction);
            applySelectedLevelPalette();
            StackPane nextCard = createLevelCard(getSelectedLevel(), onSelectLevel);
            nextCard.setOpacity(0.0);
            nextCard.setTranslateX(incomingStart);
            levelSwitcher.getChildren().set(1, nextCard);

            Timeline slideIn = new Timeline(new KeyFrame(
                    Duration.millis(LEVEL_SWITCH_ANIMATION_MS * 0.55),
                    new KeyValue(nextCard.opacityProperty(), 1.0, Interpolator.EASE_BOTH),
                    new KeyValue(nextCard.translateXProperty(), 0.0, Interpolator.EASE_BOTH)));
            slideIn.setOnFinished(done -> levelSwitcher.getProperties().remove("levelSwitching"));
            slideIn.play();
        });

        slideOut.play();
    }

    private StackPane createLevelCard(LevelChoice level, Runnable onSelectLevel) {
        StackPane card = new StackPane();
        card.getStyleClass().addAll("level-card", "level-card-" + level.styleClass());
        card.setMinSize(LEVEL_CARD_WIDTH, LEVEL_CARD_HEIGHT);
        card.setPrefSize(LEVEL_CARD_WIDTH, LEVEL_CARD_HEIGHT);
        card.setMaxSize(LEVEL_CARD_WIDTH, LEVEL_CARD_HEIGHT);

        Region frameGlow = new Region();
        frameGlow.getStyleClass().add("level-card-glow");
        frameGlow.setOpacity(0.72);

        DropShadow cardShadow = new DropShadow(28, currentMenuPalette.glow());
        cardShadow.setSpread(0.12);
        card.setEffect(cardShadow);

        Region outerFrame = new Region();
        outerFrame.getStyleClass().add("level-card-frame");

        Region leftBracket = new Region();
        leftBracket.getStyleClass().addAll("level-card-side-bracket", "left");
        StackPane.setAlignment(leftBracket, Pos.CENTER_LEFT);

        Region rightBracket = new Region();
        rightBracket.getStyleClass().addAll("level-card-side-bracket", "right");
        StackPane.setAlignment(rightBracket, Pos.CENTER_RIGHT);

        StackPane difficultyBadge = new StackPane();
        difficultyBadge.getStyleClass().add("level-difficulty-badge");
        StackPane.setAlignment(difficultyBadge, Pos.TOP_CENTER);
        StackPane.setMargin(difficultyBadge, new Insets(16, 0, 0, 0));

        Text difficulty = new Text(level.difficulty().name());
        difficulty.getStyleClass().add("level-difficulty");
        difficultyBadge.getChildren().add(difficulty);

        StackPane crystalPanel = new StackPane();
        crystalPanel.getStyleClass().add("level-card-panel");
        crystalPanel.setCursor(Cursor.HAND);
        crystalPanel.setOnMouseClicked(event -> onSelectLevel.run());
        crystalPanel.setOnMouseEntered(event -> animateLevelHover(card, cardShadow, 1.02, 40, 0.20, frameGlow, 1.0));
        crystalPanel.setOnMouseExited(event -> animateLevelHover(card, cardShadow, 1.0, 28, 0.12, frameGlow, 0.72));

        VBox copy = new VBox(12);
        copy.setAlignment(Pos.CENTER);
        copy.setMouseTransparent(true);

        Text name = new Text(level.levelName());
        name.getStyleClass().add("level-name");

        Text prompt = new Text("Tap to enter");
        prompt.getStyleClass().add("level-card-prompt");

        copy.getChildren().addAll(name, prompt);
        crystalPanel.getChildren().add(copy);

        Region bottomPrism = new Region();
        bottomPrism.getStyleClass().add("level-card-footer-prism");
        StackPane.setAlignment(bottomPrism, Pos.BOTTOM_CENTER);
        StackPane.setMargin(bottomPrism, new Insets(0, 0, 16, 0));

        card.getChildren().addAll(
                frameGlow,
                outerFrame,
                leftBracket,
                rightBracket,
                crystalPanel,
                difficultyBadge,
                bottomPrism);
        return card;
    }

    private void animateLevelHover(
            Node node,
            DropShadow shadow,
            double scale,
            double radius,
            double spread,
            Node glowNode,
            double glowOpacity) {
        Object existingAnimation = node.getProperties().get("levelHoverAnimation");
        if (existingAnimation instanceof Timeline timeline) {
            timeline.stop();
        }

        Timeline animation = new Timeline(new KeyFrame(
                Duration.millis(LEVEL_HOVER_ANIMATION_MS),
                new KeyValue(node.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(node.scaleYProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                new KeyValue(shadow.spreadProperty(), spread, Interpolator.EASE_BOTH)));
        if (glowNode != null) {
            animation.getKeyFrames().setAll(new KeyFrame(
                    Duration.millis(LEVEL_HOVER_ANIMATION_MS),
                    new KeyValue(node.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                    new KeyValue(node.scaleYProperty(), scale, Interpolator.EASE_BOTH),
                    new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                    new KeyValue(shadow.spreadProperty(), spread, Interpolator.EASE_BOTH),
                    new KeyValue(glowNode.opacityProperty(), glowOpacity, Interpolator.EASE_BOTH)));
        }
        node.getProperties().put("levelHoverAnimation", animation);
        animation.play();
    }

    private VBox createSkinPreviewCard(PlayerSkin skin, boolean current) {
        VBox card = new VBox(10);
        card.getStyleClass().add("skin-card");
        if (current) {
            card.getStyleClass().add("current");
        } else {
            card.getStyleClass().add("neighbor");
        }
        card.setAlignment(Pos.CENTER);
        card.setMinSize(SKIN_CARD_WIDTH, SKIN_CARD_HEIGHT);
        card.setPrefSize(SKIN_CARD_WIDTH, SKIN_CARD_HEIGHT);
        card.setMaxSize(SKIN_CARD_WIDTH, SKIN_CARD_HEIGHT);

        double canvasSize = 112.0;
        double radius = current ? 28.0 : 22.0;
        double opacity = current ? 1.0 : 0.62;

        Canvas canvas = new Canvas(canvasSize, canvasSize);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvasSize, canvasSize);
        PlayerRenderer.drawMenuPreview(gc, skin, canvasSize * 0.5, canvasSize * 0.5, radius, opacity);

        Text name = new Text(skin.getDisplayName());
        name.getStyleClass().add(current ? "skin-card-title-current" : "skin-card-title");

        card.getChildren().addAll(canvas, name);
        return card;
    }

    private PlayerSkin getSelectedSkin() {
        return PlayerSkin.values()[wrapSkinIndex(currentSkinIndex)];
    }

    private PlayerSkin getRelativeSkin(int offset) {
        return PlayerSkin.values()[wrapSkinIndex(currentSkinIndex + offset)];
    }

    private LevelChoice getSelectedLevel() {
        return LEVEL_CHOICES[wrapLevelIndex(currentLevelIndex)];
    }

    private int wrapSkinIndex(int index) {
        int total = PlayerSkin.values().length;
        int wrapped = index % total;
        return wrapped < 0 ? wrapped + total : wrapped;
    }

    private int wrapLevelIndex(int index) {
        int total = LEVEL_CHOICES.length;
        int wrapped = index % total;
        return wrapped < 0 ? wrapped + total : wrapped;
    }

    private MenuSettings getMenuSettings() {
        return new MenuSettings(
                clamp(gameVolume / 100.0, 0.0, 1.0),
                (int) Math.round(mistSampleStep),
                getSelectedSkin(),
                getSelectedLevel().difficulty()
        );
    }

    private void applySelectedLevelPalette() {
        animateMenuPaletteTo(paletteFor(getSelectedLevel().difficulty()));
    }

    private void applyDefaultMenuPalette() {
        animateMenuPaletteTo(DEFAULT_MENU_PALETTE);
    }

    private MenuPalette paletteFor(Difficulty difficulty) {
        return switch (difficulty) {
            case MEDIUM -> new MenuPalette(
                    Color.web("#161210"),
                    Color.rgb(46, 38, 28, 0.22),
                    Color.rgb(10, 8, 6, 0.20),
                    Color.web("#FFF2D8"),
                    Color.rgb(255, 242, 216, 0.74),
                    Color.rgb(255, 242, 216, 0.66),
                    Color.web("#E0C79B"),
                    Color.rgb(224, 199, 155, 0.86),
                    Color.rgb(224, 199, 155, 0.86),
                    Color.rgb(224, 199, 155, 0.30),
                    Color.rgb(26, 22, 18, 0.78),
                    Color.rgb(46, 36, 26, 0.42),
                    Color.rgb(68, 54, 36, 0.56),
                    Color.rgb(96, 74, 42, 0.58),
                    Color.rgb(130, 100, 56, 0.74),
                    Color.rgb(58, 45, 28, 0.46),
                    Color.rgb(98, 76, 44, 0.62),
                    Color.web("#E0C79B"),
                    Difficulty.MEDIUM,
                    new MistProfile(
                            0.20, 0.17, 0.12,
                            0.82, 0.68, 0.42,
                            0.78, 0.24,
                            0.86, 36.0, -10.0,
                            0.82, 0.06,
                            28.0, 12.0,
                            0.014, 0.011,
                            0.46));
            case HARD -> new MenuPalette(
                    Color.web("#150A0C"),
                    Color.rgb(54, 10, 14, 0.24),
                    Color.rgb(12, 2, 4, 0.22),
                    Color.web("#FFF0EA"),
                    Color.rgb(255, 240, 234, 0.74),
                    Color.rgb(255, 240, 234, 0.66),
                    Color.web("#FF745B"),
                    Color.rgb(255, 116, 91, 0.86),
                    Color.rgb(255, 116, 91, 0.88),
                    Color.rgb(255, 116, 91, 0.32),
                    Color.rgb(28, 8, 12, 0.78),
                    Color.rgb(58, 14, 18, 0.42),
                    Color.rgb(82, 20, 24, 0.58),
                    Color.rgb(132, 36, 30, 0.58),
                    Color.rgb(176, 50, 36, 0.74),
                    Color.rgb(72, 18, 20, 0.46),
                    Color.rgb(124, 34, 28, 0.64),
                    Color.web("#FF745B"),
                    Difficulty.HARD,
                    new MistProfile(
                            0.16, 0.03, 0.05,
                            0.92, 0.22, 0.12,
                            0.86, 0.30,
                            1.18, 58.0, -20.0,
                            1.18, 0.09,
                            46.0, 24.0,
                            0.017, 0.013,
                            0.60));
            default -> new MenuPalette(
                    Color.web("#111520"),
                    Color.rgb(10, 24, 38, 0.20),
                    Color.rgb(4, 10, 18, 0.18),
                    Color.web("#F0FBFF"),
                    Color.rgb(240, 251, 255, 0.74),
                    Color.rgb(240, 251, 255, 0.68),
                    Color.web("#7DE4FF"),
                    Color.rgb(125, 228, 255, 0.86),
                    Color.rgb(125, 228, 255, 0.88),
                    Color.rgb(125, 228, 255, 0.30),
                    Color.rgb(12, 18, 30, 0.76),
                    Color.rgb(12, 22, 34, 0.40),
                    Color.rgb(18, 38, 54, 0.56),
                    Color.rgb(25, 72, 96, 0.56),
                    Color.rgb(42, 106, 132, 0.72),
                    Color.rgb(14, 44, 64, 0.44),
                    Color.rgb(35, 88, 116, 0.60),
                    Color.web("#7DE4FF"),
                    Difficulty.EASY,
                    new MistProfile(
                            0.05, 0.08, 0.14,
                            0.32, 0.68, 0.92,
                            0.80, 0.26,
                            0.92, 42.0, -12.0,
                            0.96, 0.07,
                            34.0, 16.0,
                            0.015, 0.012,
                            0.52));
        };
    }

    private void animateMenuPaletteTo(MenuPalette targetPalette) {
        if (paletteTransition != null) {
            paletteTransition.stop();
        }

        transitionStartPalette = currentMenuPalette;
        transitionTargetPalette = targetPalette;
        transitionStartMistProfile = currentMistProfile;
        transitionTargetMistProfile = colorOnlyMistProfile(targetPalette.mistProfile());
        previousGridRenderer = gridRenderer;
        gridRenderer = new GridRenderer(new SpriteSheet(targetPalette.backgroundDifficulty()));
        backgroundBlend = 0.0;
        mistMotionFrozen = true;

        paletteTransitionProgress.set(0.0);
        paletteTransition = new Timeline(new KeyFrame(
                Duration.millis(LEVEL_PALETTE_TRANSITION_MS),
                new KeyValue(paletteTransitionProgress, 1.0, Interpolator.EASE_BOTH)));
        paletteTransition.setOnFinished(event -> {
            currentMenuPalette = targetPalette;
            currentMistProfile = colorOnlyMistProfile(targetPalette.mistProfile());
            previousGridRenderer = null;
            backgroundBlend = 1.0;
            mistMotionFrozen = false;
            setStyle(createPaletteStyle(targetPalette));
            drawBackground();
        });
        paletteTransition.play();
    }

    private void updatePaletteTransition(double progress) {
        double easedProgress = clamp(progress, 0.0, 1.0);
        currentMenuPalette = interpolatePalette(transitionStartPalette, transitionTargetPalette, easedProgress);
        currentMistProfile = interpolateMistProfile(transitionStartMistProfile, transitionTargetMistProfile,
                easedProgress);
        backgroundBlend = easedProgress;
        setStyle(createPaletteStyle(currentMenuPalette));
        drawBackground();
    }

    private MenuPalette interpolatePalette(MenuPalette start, MenuPalette end, double progress) {
        return new MenuPalette(
                interpolateColor(start.bg(), end.bg(), progress),
                interpolateColor(start.tint(), end.tint(), progress),
                interpolateColor(start.vignette(), end.vignette(), progress),
                interpolateColor(start.text(), end.text(), progress),
                interpolateColor(start.textSoft(), end.textSoft(), progress),
                interpolateColor(start.textMuted(), end.textMuted(), progress),
                interpolateColor(start.accent(), end.accent(), progress),
                interpolateColor(start.accentSoft(), end.accentSoft(), progress),
                interpolateColor(start.border(), end.border(), progress),
                interpolateColor(start.borderSoft(), end.borderSoft(), progress),
                interpolateColor(start.panelBg(), end.panelBg(), progress),
                interpolateColor(start.cardBg(), end.cardBg(), progress),
                interpolateColor(start.cardCurrentBg(), end.cardCurrentBg(), progress),
                interpolateColor(start.buttonBg(), end.buttonBg(), progress),
                interpolateColor(start.buttonHoverBg(), end.buttonHoverBg(), progress),
                interpolateColor(start.actionBg(), end.actionBg(), progress),
                interpolateColor(start.actionHoverBg(), end.actionHoverBg(), progress),
                interpolateColor(start.glow(), end.glow(), progress),
                progress < 1.0 ? start.backgroundDifficulty() : end.backgroundDifficulty(),
                interpolateMistProfile(start.mistProfile(), end.mistProfile(), progress));
    }

    private MistProfile interpolateMistProfile(MistProfile start, MistProfile end, double progress) {
        MistProfile motion = DEFAULT_MENU_MIST_PROFILE;
        return new MistProfile(
                lerp(start.colorR(), end.colorR(), progress),
                lerp(start.colorG(), end.colorG(), progress),
                lerp(start.colorB(), end.colorB(), progress),
                lerp(start.accentR(), end.accentR(), progress),
                lerp(start.accentG(), end.accentG(), progress),
                lerp(start.accentB(), end.accentB(), progress),
                motion.baseAlpha(),
                motion.swirlAlpha(),
                motion.driftSpeed(),
                motion.flowSpeedX(),
                motion.flowSpeedY(),
                motion.pulseSpeed(),
                motion.pulseStrength(),
                motion.lateralSwing(),
                motion.verticalSwing(),
                motion.noiseScaleX(),
                motion.noiseScaleY(),
                lerp(start.accentStrength(), end.accentStrength(), progress));
    }

    private MistProfile colorOnlyMistProfile(MistProfile source) {
        return new MistProfile(
                source.colorR(),
                source.colorG(),
                source.colorB(),
                source.accentR(),
                source.accentG(),
                source.accentB(),
                DEFAULT_MENU_MIST_PROFILE.baseAlpha(),
                DEFAULT_MENU_MIST_PROFILE.swirlAlpha(),
                DEFAULT_MENU_MIST_PROFILE.driftSpeed(),
                DEFAULT_MENU_MIST_PROFILE.flowSpeedX(),
                DEFAULT_MENU_MIST_PROFILE.flowSpeedY(),
                DEFAULT_MENU_MIST_PROFILE.pulseSpeed(),
                DEFAULT_MENU_MIST_PROFILE.pulseStrength(),
                DEFAULT_MENU_MIST_PROFILE.lateralSwing(),
                DEFAULT_MENU_MIST_PROFILE.verticalSwing(),
                DEFAULT_MENU_MIST_PROFILE.noiseScaleX(),
                DEFAULT_MENU_MIST_PROFILE.noiseScaleY(),
                source.accentStrength());
    }

    private Color interpolateColor(Color start, Color end, double progress) {
        return Color.color(
                lerp(start.getRed(), end.getRed(), progress),
                lerp(start.getGreen(), end.getGreen(), progress),
                lerp(start.getBlue(), end.getBlue(), progress),
                lerp(start.getOpacity(), end.getOpacity(), progress));
    }

    private String createPaletteStyle(MenuPalette palette) {
        return String.join("",
                cssColor("-menu-bg", palette.bg()),
                cssColor("-menu-tint", palette.tint()),
                cssColor("-menu-vignette", palette.vignette()),
                cssColor("-menu-text", palette.text()),
                cssColor("-menu-text-soft", palette.textSoft()),
                cssColor("-menu-text-muted", palette.textMuted()),
                cssColor("-menu-accent", palette.accent()),
                cssColor("-menu-accent-soft", palette.accentSoft()),
                cssColor("-menu-border", palette.border()),
                cssColor("-menu-border-soft", palette.borderSoft()),
                cssColor("-menu-panel-bg", palette.panelBg()),
                cssColor("-menu-card-bg", palette.cardBg()),
                cssColor("-menu-card-current-bg", palette.cardCurrentBg()),
                cssColor("-menu-button-bg", palette.buttonBg()),
                cssColor("-menu-button-hover-bg", palette.buttonHoverBg()),
                cssColor("-menu-action-bg", palette.actionBg()),
                cssColor("-menu-action-hover-bg", palette.actionHoverBg()),
                "-fx-background-color: -menu-bg;");
    }

    private String cssColor(String name, Color color) {
        return String.format(Locale.US, "%s: rgba(%d, %d, %d, %.3f);",
                name,
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255),
                color.getOpacity());
    }

    private Font loadTitleFont(double size) {
        try (InputStream fontStream = StartMenuView.class.getResourceAsStream(TITLE_FONT_PATH)) {
            if (fontStream != null) {
                return Font.loadFont(fontStream, size);
            }
        } catch (Exception ignored) {
            // Keep the menu usable with fallback fonts if the custom font is unavailable.
        }
        return null;
    }

    private StackPane createMenuOverlays() {
        StackPane overlays = new StackPane();
        overlays.setMouseTransparent(true);
        overlays.prefWidthProperty().bind(widthProperty());
        overlays.prefHeightProperty().bind(heightProperty());

        Rectangle mazeTint = new Rectangle();
        mazeTint.getStyleClass().add("menu-maze-tint");
        mazeTint.widthProperty().bind(widthProperty());
        mazeTint.heightProperty().bind(heightProperty());

        Rectangle topShade = new Rectangle();
        topShade.getStyleClass().add("menu-edge-shade");
        topShade.widthProperty().bind(widthProperty());
        topShade.heightProperty().bind(heightProperty().multiply(0.16));
        StackPane.setAlignment(topShade, Pos.TOP_CENTER);

        Rectangle bottomShade = new Rectangle();
        bottomShade.getStyleClass().add("menu-edge-shade");
        bottomShade.widthProperty().bind(widthProperty());
        bottomShade.heightProperty().bind(heightProperty().multiply(0.14));
        StackPane.setAlignment(bottomShade, Pos.BOTTOM_CENTER);

        Rectangle vignette = new Rectangle();
        vignette.getStyleClass().add("menu-vignette");
        vignette.widthProperty().bind(widthProperty());
        vignette.heightProperty().bind(heightProperty());

        overlays.getChildren().addAll(mazeTint, topShade, bottomShade, vignette);
        return overlays;
    }

    private VBox createMenuAction(
            String label,
            String iconName,
            Runnable action,
            boolean primary) {
        double buttonSize = primary ? 178 : 136;
        double iconSize = primary ? 86 : 64;
        double itemWidth = primary ? 196 : 160;

        Button button = new Button();
        button.getStyleClass().addAll("menu-action-button", primary ? "primary" : "secondary");
        button.setMinSize(buttonSize, buttonSize);
        button.setPrefSize(buttonSize, buttonSize);
        button.setMaxSize(buttonSize, buttonSize);
        button.setGraphic(createIcon(iconName, iconSize));
        DropShadow buttonShadow = new DropShadow(34, DEFAULT_MENU_GLOW);
        button.setEffect(buttonShadow);
        button.setOnAction(event -> action.run());

        button.setOnMouseEntered(event -> {
            animateHover(button, buttonShadow, primary ? 1.07 : 1.09, 46, 0.24);
        });
        button.setOnMouseExited(event -> {
            animateHover(button, buttonShadow, 1.0, 34, 0.0);
        });

        Text text = new Text(label);
        text.getStyleClass().addAll("menu-action-label", primary ? "primary" : "secondary");
        text.setEffect(new DropShadow(7, Color.rgb(0, 0, 0, 0.92)));

        VBox item = new VBox(primary ? 26 : 20);
        item.setAlignment(Pos.CENTER);
        item.setMinWidth(itemWidth);
        item.setPrefWidth(itemWidth);
        item.setMaxWidth(itemWidth);
        item.getChildren().addAll(button, text);
        return item;
    }

    private void animateHover(Button button, DropShadow shadow, double scale, double radius, double spread) {
        Object existingAnimation = button.getProperties().get("hoverAnimation");
        if (existingAnimation instanceof Timeline timeline) {
            timeline.stop();
        }

        Timeline animation = new Timeline(new KeyFrame(
                Duration.millis(170),
                new KeyValue(button.scaleXProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(button.scaleYProperty(), scale, Interpolator.EASE_BOTH),
                new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                new KeyValue(shadow.spreadProperty(), spread, Interpolator.EASE_BOTH)));
        button.getProperties().put("hoverAnimation", animation);
        animation.play();
    }

    private ImageView createIcon(String iconName, double iconSize) {
        InputStream stream = StartMenuView.class.getResourceAsStream(ICON_PATH + iconName);
        ImageView icon = new ImageView(stream == null ? null : new Image(stream));
        icon.setFitWidth(iconSize);
        icon.setFitHeight(iconSize);
        icon.setPreserveRatio(true);
        icon.setSmooth(true);
        icon.setEffect(new DropShadow(2, Color.BLACK));
        return icon;
    }

    private void drawBackground() {
        double width = background.getWidth();
        double height = background.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = background.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        MazeViewport viewport = getMazeViewport(width, height);

        gc.save();
        gc.translate(viewport.offsetX(), viewport.offsetY());
        gc.scale(viewport.scale(), viewport.scale());
        if (previousGridRenderer != null && backgroundBlend < 1.0) {
            gc.setGlobalAlpha(1.0 - backgroundBlend);
            previousGridRenderer.draw(gc, menuMaze);
            gc.setGlobalAlpha(backgroundBlend);
        }
        gridRenderer.draw(gc, menuMaze);
        gc.restore();
    }

    private void drawMenuArtifacts(long now) {
        double width = artifacts.getWidth();
        double height = artifacts.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = artifacts.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        MazeViewport viewport = getMazeViewport(width, height);
        double tile = TILE_SIZE * viewport.scale();
        double timeSeconds = now / 1_000_000_000.0;

        for (MenuArtifact artifact : MENU_ARTIFACTS) {
            double pulse = 0.5 + 0.5 * Math.sin(timeSeconds * 1.8 + artifact.phase());
            double centerX = viewport.offsetX() + (artifact.col() + 0.5) * tile;
            double centerY = viewport.offsetY() + (artifact.row() + 0.5) * tile;
            double radius = tile * (0.16 + pulse * 0.045);

            gc.save();
            gc.setGlobalAlpha(0.78 + pulse * 0.20);
            gc.setEffect(new DropShadow(tile * (0.55 + pulse * 0.25), artifact.accent()));
            gc.setFill(artifact.base());
            if (artifact.diamond()) {
                gc.fillPolygon(
                        new double[] { centerX, centerX + radius, centerX, centerX - radius },
                        new double[] { centerY - radius, centerY, centerY + radius, centerY },
                        4);
            } else {
                gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
            gc.restore();

            gc.setStroke(Color.color(
                    artifact.accent().getRed(),
                    artifact.accent().getGreen(),
                    artifact.accent().getBlue(),
                    0.50 + pulse * 0.30));
            gc.setLineWidth(Math.max(1.2, tile * 0.035));
            gc.strokeOval(centerX - radius * 1.9, centerY - radius * 1.9, radius * 3.8, radius * 3.8);
        }
    }

    private void updateMistFocus(long now) {
        double width = mist.getWidth();
        double height = mist.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (lastMistFrameNanos == 0L) {
            mistFocusX = width * 0.5;
            mistFocusY = height * 0.5;
            mistTargetX = mistFocusX;
            mistTargetY = mistFocusY;
            lastMistFrameNanos = now;
        }

        double deltaSeconds = (now - lastMistFrameNanos) / 1_000_000_000.0;
        lastMistFrameNanos = now;
        if (mistMotionFrozen) {
            return;
        }

        mistTimeNanos += (long) (deltaSeconds * 1_000_000_000.0);

        double follow = 1.0 - Math.pow(0.001, Math.min(deltaSeconds, 0.05));
        mistFocusX = lerp(mistFocusX, mistTargetX, follow);
        mistFocusY = lerp(mistFocusY, mistTargetY, follow);
    }

    private void drawMist() {
        double width = mist.getWidth();
        double height = mist.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = mist.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);

        MistProfile profile = getMenuMistProfile();
        double clearRadius = Math.max(86.0, Math.min(width, height) * 0.20);
        double edgeFadeBand = clearRadius * 0.82;
        double timeSeconds = mistTimeNanos / 1_000_000_000.0;
        int sampleStep = Math.max(1, (int) Math.round(mistSampleStep));

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                double sampleX = x + sampleStep * 0.5;
                double sampleY = y + sampleStep * 0.5;
                double dx = sampleX - mistFocusX;
                double dy = sampleY - mistFocusY;
                double dist = Math.sqrt(dx * dx + dy * dy);

                double distanceOpacity = smoothStep(clearRadius - edgeFadeBand, clearRadius + edgeFadeBand, dist);
                double pulse = 1.0 + Math.sin(timeSeconds * profile.pulseSpeed()) * profile.pulseStrength();

                double flowX = sampleX + timeSeconds * profile.flowSpeedX()
                        + Math.sin(timeSeconds * 0.35) * profile.lateralSwing();
                double flowY = sampleY + timeSeconds * profile.flowSpeedY()
                        + Math.cos(timeSeconds * 0.28) * profile.verticalSwing();

                double drift1 = Math.sin(flowX * profile.noiseScaleX() + timeSeconds * profile.driftSpeed())
                        * Math.cos(flowY * profile.noiseScaleY() - timeSeconds * profile.driftSpeed() * 0.85);
                double drift2 = Math
                        .sin(flowX * profile.noiseScaleX() * 0.62 - timeSeconds * profile.driftSpeed() * 0.52)
                        * Math.sin(flowY * profile.noiseScaleY() * 0.88 + timeSeconds * profile.driftSpeed() * 0.68);
                double drift3 = Math.cos(flowX * profile.noiseScaleX() * 0.38 + flowY * profile.noiseScaleY() * 0.56
                        + timeSeconds * profile.driftSpeed() * 0.38);
                double drift4 = Math.sin((flowX + flowY) * profile.noiseScaleX() * 0.32
                        - timeSeconds * profile.driftSpeed() * 0.44);

                double swirl = (drift1 * 0.35 + drift2 * 0.30 + drift3 * 0.20 + drift4 * 0.15) * profile.swirlAlpha();
                double holeNoise = ((drift1 * 0.30) + (drift2 * 0.30) + (drift3 * 0.25) + (drift4 * 0.15) + 1.0) * 0.5;
                double densityMask = smoothStep(0.02, 0.28, holeNoise);
                double accentMask = smoothStep(0.55, 0.95, ((drift2 * 0.55) + (drift4 * 0.45) + 1.0) * 0.5)
                        * profile.accentStrength();

                double alpha = clamp(distanceOpacity * pulse * densityMask * (profile.baseAlpha() + swirl), 0.0,
                        MIST_ALPHA_CAP);
                if (alpha > 0.01) {
                    double colorR = lerp(profile.colorR(), profile.accentR(), accentMask);
                    double colorG = lerp(profile.colorG(), profile.accentG(), accentMask);
                    double colorB = lerp(profile.colorB(), profile.accentB(), accentMask);
                    gc.setFill(Color.color(colorR, colorG, colorB, alpha));
                    gc.fillRect(x, y, sampleStep, sampleStep);
                }
            }
        }
    }

    private MistProfile getMenuMistProfile() {
        return currentMistProfile;
    }

    private MazeViewport getMazeViewport(double width, double height) {
        double mazeWidth = menuMaze.getWidth() * TILE_SIZE;
        double mazeHeight = menuMaze.getHeight() * TILE_SIZE;
        double scale = Math.max(width / mazeWidth, height / mazeHeight) * 1.06;
        double drawWidth = mazeWidth * scale;
        double drawHeight = mazeHeight * scale;
        return new MazeViewport(scale, (width - drawWidth) * 0.5, (height - drawHeight) * 0.5);
    }

    private double smoothStep(double edge0, double edge1, double x) {
        double range = edge1 - edge0;
        if (range <= 0.0) {
            return x >= edge1 ? 1.0 : 0.0;
        }
        double t = clamp((x - edge0) / range, 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}
