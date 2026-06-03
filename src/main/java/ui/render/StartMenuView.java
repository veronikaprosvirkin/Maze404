package ui.render;

import enums.Difficulty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import logic.MazeGenerator;
import model.Grid;

import java.io.InputStream;

public class StartMenuView extends StackPane {
    private static final String ICON_PATH = "/styles/icons/";
    private static final Color VIOLET_GLOW = Color.web("#C9A7FF");
    private static final Color VIOLET_TEXT = Color.web("#F1E8FF");
    private static final int TILE_SIZE = 32;

    private final Canvas background = new Canvas();
    private final Grid menuMaze = new MazeGenerator().generate(19, 31);
    private final GridRenderer gridRenderer = new GridRenderer(new SpriteSheet(Difficulty.MEDIUM));

    public StartMenuView(Runnable onPlay, Runnable onSettings, Runnable onExit) {
        getStyleClass().add("start-menu");
        setStyle("-fx-background-color: #08040F;");

        background.widthProperty().bind(widthProperty());
        background.heightProperty().bind(heightProperty());
        getChildren().add(background);

        VBox content = new VBox();
        content.setAlignment(Pos.TOP_CENTER);
        content.setFillWidth(true);
        content.setPadding(new Insets(42, 58, 48, 58));
        content.spacingProperty().bind(heightProperty().multiply(0.09));

        Text title = new Text("MAZE 404");
        title.setFill(VIOLET_TEXT);
        title.setFont(Font.font("Arial", FontWeight.LIGHT, 62));
        title.setEffect(new DropShadow(24, VIOLET_GLOW));
        title.scaleXProperty().bind(widthProperty().divide(1024).multiply(0.25).add(0.75));
        title.scaleYProperty().bind(title.scaleXProperty());

        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER);
        actions.setSpacing(86);
        actions.maxWidthProperty().bind(widthProperty().multiply(0.82));

        actions.getChildren().addAll(
                createMenuAction("Settings", "settings.png", onSettings, false),
                createMenuAction("Play", "play.png", onPlay, true),
                createMenuAction("Exit", "exit.png", onExit, false)
        );

        content.getChildren().addAll(title, actions);
        getChildren().add(content);

        widthProperty().addListener((obs, oldValue, newValue) -> drawBackground());
        heightProperty().addListener((obs, oldValue, newValue) -> drawBackground());
    }

    private VBox createMenuAction(String label, String iconName, Runnable action, boolean primary) {
        double buttonSize = primary ? 178 : 136;
        double iconSize = primary ? 86 : 64;
        double padding = primary ? 27 : 22;
        double labelSize = primary ? 39 : 31;
        double itemWidth = primary ? 196 : 160;

        Button button = new Button();
        button.setCursor(Cursor.HAND);
        button.setMinSize(buttonSize, buttonSize);
        button.setPrefSize(buttonSize, buttonSize);
        button.setMaxSize(buttonSize, buttonSize);
        button.setGraphic(createIcon(iconName, iconSize));
        button.setStyle(String.format("""
                -fx-background-radius: 999;
                -fx-background-color: rgba(34, 16, 58, 0.42);
                -fx-border-color: rgba(201, 167, 255, 0.96);
                -fx-border-width: 4;
                -fx-border-radius: 999;
                -fx-padding: %.0f;
                """, padding));
        button.setEffect(new DropShadow(34, VIOLET_GLOW));
        button.setOnAction(event -> action.run());

        button.setOnMouseEntered(event -> {
            button.setStyle(String.format("""
                    -fx-background-radius: 999;
                    -fx-background-color: rgba(76, 38, 124, 0.58);
                    -fx-border-color: rgba(241, 232, 255, 1);
                    -fx-border-width: 4;
                    -fx-border-radius: 999;
                    -fx-padding: %.0f;
                    """, padding));
            button.setEffect(new Bloom(0.22));
        });
        button.setOnMouseExited(event -> {
            button.setStyle(String.format("""
                    -fx-background-radius: 999;
                    -fx-background-color: rgba(34, 16, 58, 0.42);
                    -fx-border-color: rgba(201, 167, 255, 0.96);
                    -fx-border-width: 4;
                    -fx-border-radius: 999;
                    -fx-padding: %.0f;
                    """, padding));
            button.setEffect(new DropShadow(34, VIOLET_GLOW));
        });

        Text text = new Text(label);
        text.setFill(VIOLET_TEXT);
        text.setFont(Font.font("Arial", FontWeight.NORMAL, labelSize));
        text.setEffect(new DropShadow(7, Color.rgb(0, 0, 0, 0.92)));

        VBox item = new VBox(primary ? 26 : 20, button, text);
        item.setAlignment(Pos.CENTER);
        item.setMinWidth(itemWidth);
        HBox.setHgrow(item, Priority.ALWAYS);
        return item;
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

        gc.setFill(Color.web("#08040F"));
        gc.fillRect(0, 0, width, height);

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

        gc.setFill(Color.rgb(12, 4, 22, 0.34));
        gc.fillRect(0, 0, width, height);

        gc.setStroke(Color.rgb(201, 167, 255, 0.10));
        gc.setLineWidth(1.0);
        for (int row = 0; row < menuMaze.getHeight(); row++) {
            for (int col = 0; col < menuMaze.getWidth(); col++) {
                if ((row + col) % 5 == 0) {
                    double x = offsetX + col * TILE_SIZE * scale;
                    double y = offsetY + row * TILE_SIZE * scale;
                    gc.strokeRect(x, y, TILE_SIZE * scale, TILE_SIZE * scale);
                }
            }
        }

        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(0, 0, width, height * 0.16);
        gc.fillRect(0, height * 0.86, width, height * 0.14);

        gc.setFill(Color.rgb(143, 85, 255, 0.11));
        gc.fillOval(width * 0.14, height * 0.25, width * 0.72, height * 0.58);

        gc.setFill(Color.rgb(8, 3, 14, 0.28));
        gc.fillRect(0, 0, width, height);
    }
}
