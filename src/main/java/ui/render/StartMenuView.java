package ui.render;

import enums.Difficulty;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import logic.MazeGenerator;
import model.Grid;

import java.io.InputStream;

public class StartMenuView extends StackPane {
    private static final String ICON_PATH = "/icons/";
    private static final String TITLE_FONT_PATH = "/fonts/DoctorGlitch.otf";
    private static final Color VIOLET_GLOW = Color.web("#C9A7FF");
    private static final int TILE_SIZE = 32;
    private static final double BASE_WIDTH = 1024.0;
    private static final double BASE_HEIGHT = 576.0;
    private static final int MIST_SAMPLE_STEP = 4;
    private static final double MIST_ALPHA_CAP = 0.98;
    private static final double TITLE_FONT_SIZE = 62.0;

    private final Canvas background = new Canvas();
    private final Canvas artifacts = new Canvas();
    private final Canvas mist = new Canvas();
    private final Grid menuMaze = new MazeGenerator().generate(19, 31);
    private final GridRenderer gridRenderer = new GridRenderer(new SpriteSheet(Difficulty.MEDIUM));
    private long mistTimeNanos = 0L;
    private long lastMistFrameNanos = 0L;
    private double mistFocusX = BASE_WIDTH * 0.5;
    private double mistFocusY = BASE_HEIGHT * 0.5;
    private double mistTargetX = BASE_WIDTH * 0.5;
    private double mistTargetY = BASE_HEIGHT * 0.5;

    private record MenuArtifact(int row, int col, Color base, Color accent, double phase, boolean diamond) {
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
            double accentStrength
    ) {
    }

    private static final MenuArtifact[] MENU_ARTIFACTS = {
            new MenuArtifact(3, 5, Color.web("#F0D66A"), Color.web("#FFF3A6"), 0.0, true),
            new MenuArtifact(5, 20, Color.web("#7DE4FF"), Color.web("#D7FAFF"), 1.4, false),
            new MenuArtifact(9, 11, Color.web("#C46BFF"), Color.web("#F0C8FF"), 2.3, true),
            new MenuArtifact(12, 25, Color.web("#65F2A0"), Color.web("#D4FFE3"), 3.5, false),
            new MenuArtifact(15, 7, Color.web("#FF73B7"), Color.web("#FFD4EA"), 4.2, true)
    };

    public StartMenuView(Runnable onPlay, Runnable onSettings, Runnable onExit) {
        getStyleClass().add("start-menu");
        var stylesheet = StartMenuView.class.getResource("/styles/start-menu.css");
        if (stylesheet != null) {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        DoubleBinding uiScale = Bindings.createDoubleBinding(
                () -> clamp(Math.min(getWidth() / BASE_WIDTH, getHeight() / BASE_HEIGHT), 0.38, 1.34),
                widthProperty(),
                heightProperty()
        );

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
        title.setEffect(new DropShadow(24, VIOLET_GLOW));

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER);
        actions.setSpacing(86);
        actions.setMinWidth(688);
        actions.setPrefWidth(688);
        actions.setMaxWidth(688);

        actions.getChildren().addAll(
                createMenuAction("Settings", "settings.png", onSettings, false),
                createMenuAction("Play", "play.png", onPlay, true),
                createMenuAction("Exit", "exit.png", onExit, false)
        );

        content.getChildren().addAll(title, actions);
        contentFrame.getChildren().add(content);
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
            boolean primary
    ) {
        double buttonSize = primary ? 178 : 136;
        double iconSize = primary ? 86 : 64;
        double itemWidth = primary ? 196 : 160;

        Button button = new Button();
        button.getStyleClass().addAll("menu-action-button", primary ? "primary" : "secondary");
        button.setMinSize(buttonSize, buttonSize);
        button.setPrefSize(buttonSize, buttonSize);
        button.setMaxSize(buttonSize, buttonSize);
        button.setGraphic(createIcon(iconName, iconSize));
        DropShadow buttonShadow = new DropShadow(34, VIOLET_GLOW);
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
                new KeyValue(shadow.spreadProperty(), spread, Interpolator.EASE_BOTH)
        ));
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
                        new double[]{centerX, centerX + radius, centerX, centerX - radius},
                        new double[]{centerY - radius, centerY, centerY + radius, centerY},
                        4
                );
            } else {
                gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            }
            gc.restore();

            gc.setStroke(Color.color(
                    artifact.accent().getRed(),
                    artifact.accent().getGreen(),
                    artifact.accent().getBlue(),
                    0.50 + pulse * 0.30
            ));
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
        mistTimeNanos = now;

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

        for (int y = 0; y < height; y += MIST_SAMPLE_STEP) {
            for (int x = 0; x < width; x += MIST_SAMPLE_STEP) {
                double sampleX = x + MIST_SAMPLE_STEP * 0.5;
                double sampleY = y + MIST_SAMPLE_STEP * 0.5;
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
                double drift2 = Math.sin(flowX * profile.noiseScaleX() * 0.62 - timeSeconds * profile.driftSpeed() * 0.52)
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
                    gc.fillRect(x, y, MIST_SAMPLE_STEP, MIST_SAMPLE_STEP);
                }
            }
        }
    }

    private MistProfile getMenuMistProfile() {
        return new MistProfile(
                0.08, 0.03, 0.14,
                0.62, 0.36, 0.96,
                0.82, 0.28,
                1.05, 52.0, -14.0,
                1.12, 0.08,
                42.0, 18.0,
                0.016, 0.012,
                0.58
        );
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
