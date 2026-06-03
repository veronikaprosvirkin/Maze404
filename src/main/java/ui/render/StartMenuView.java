package ui.render;

import enums.Difficulty;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import logic.MazeGenerator;
import model.Grid;

import java.io.InputStream;

public class StartMenuView extends StackPane {
    private static final String ICON_PATH = "/styles/icons/";
    private static final Color VIOLET_GLOW = Color.web("#C9A7FF");
    private static final int TILE_SIZE = 32;
    private static final double BASE_WIDTH = 1024.0;
    private static final double BASE_HEIGHT = 576.0;

    private final Canvas background = new Canvas();
    private final Grid menuMaze = new MazeGenerator().generate(19, 31);
    private final GridRenderer gridRenderer = new GridRenderer(new SpriteSheet(Difficulty.MEDIUM));

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

        Ellipse centerAura = new Ellipse();
        centerAura.getStyleClass().add("menu-center-aura");
        centerAura.radiusXProperty().bind(widthProperty().multiply(0.36));
        centerAura.radiusYProperty().bind(heightProperty().multiply(0.29));

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

        overlays.getChildren().addAll(mazeTint, centerAura, topShade, bottomShade, vignette);
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

        double mazeWidth = menuMaze.getWidth() * TILE_SIZE;
        double mazeHeight = menuMaze.getHeight() * TILE_SIZE;
        double scale = Math.max(width / mazeWidth, height / mazeHeight) * 1.06;
        double drawWidth = mazeWidth * scale;
        double drawHeight = mazeHeight * scale;
        double offsetX = (width - drawWidth) * 0.5;
        double offsetY = (height - drawHeight) * 0.5;

        gc.save();
        gc.translate(offsetX, offsetY);
        gc.scale(scale, scale);
        gridRenderer.draw(gc, menuMaze);
        gc.restore();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
