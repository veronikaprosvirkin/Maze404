package ui.render;

import enums.ArtifactType;
import enums.Difficulty;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import model.Player;
import ui.input.GameAction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameHud {
    private static final int PLAYER_MAX_HEALTH = 3;
    private static final Duration DAMAGE_HIGHLIGHT_DURATION = Duration.seconds(1);
    private static final Duration AUTO_MINIMIZE_DELAY = Duration.seconds(3);
    private static final Duration MINIMIZE_ANIMATION_DURATION = Duration.millis(380);
    private static final Duration HOTKEY_REVEAL_DELAY = Duration.seconds(0.8);
    private static final Duration HOTKEY_REVEAL_DURATION = Duration.millis(180);
    private static final double CARD_EXPANDED_MIN_WIDTH = 96;
    private static final double MINIMIZED_HEALTH_CARD_MIN_WIDTH = 90;
    private static final double MINIMIZED_SCALE = 0.76;
    private static final double ANIMATION_CLIP_PADDING = 64;
    private static final double MINIMIZED_WIDTH_RATIO = 0.76;
    private static final double MINIMIZED_EASY_MIN_WIDTH = 470;
    private static final double MINIMIZED_EASY_MAX_WIDTH = 560;
    private static final double MINIMIZED_FULL_MIN_WIDTH = 600;
    private static final double MINIMIZED_FULL_MAX_WIDTH = 700;
    private static final String MINIMIZE_ANIMATION_KEY = "hudMinimizeAnimation";
    private static final String MINIMIZED_STATE_KEY = "hudMinimized";
    private static final String HOTKEY_REVEAL_TIMER_KEY = "hudHotkeyRevealTimer";
    private static final String HOTKEY_REVEAL_ANIMATION_KEY = "hudHotkeyRevealAnimation";
    private static final String ARTIFACT_HOTKEY_CLASS = "hud-artifact-hotkey-badge";

    private final Scene scene;
    private final VBox view;
    private final HBox hudRow;
    private final Label hpValueLabel = createHudValueLabel();
    private final Label crystalsValueLabel = createHudValueLabel();
    private final Label radarValueLabel = createHudValueLabel();
    private final Label shieldValueLabel = createHudValueLabel();
    private final Label beaconValueLabel = createHudValueLabel();
    private final Label elixirsValueLabel = createHudValueLabel();
    private final Label keyValueLabel = createHudValueLabel();
    private final VBox healthCard;
    private final VBox radarCard;
    private final VBox shieldCard;
    private final VBox beaconCard;
    private final VBox elixirCard;
    private final PauseTransition healthDamageHighlightTimer = new PauseTransition(DAMAGE_HIGHLIGHT_DURATION);
    private final PauseTransition autoMinimizeTimer = new PauseTransition(AUTO_MINIMIZE_DELAY);
    private final int[] lastRenderedHealth;
    private EventHandler<KeyEvent> tabPressedHandler;
    private EventHandler<KeyEvent> tabReleasedHandler;

    public GameHud(Scene scene, Difficulty difficulty, Player player) {
        this.scene = scene;
        updateValues(player);

        HBox healthHud = createHudShell(true);
        healthHud.setSpacing(10);
        healthCard = createHudCard("Health", createHealthIcon(), hpValueLabel, "health");
        healthHud.getChildren().addAll(
                healthCard,
                createHudCard("Crystals", ArtifactVisuals.createHudIcon(ArtifactType.CRYSTAL, 24), crystalsValueLabel, "crystals")
        );

        HBox inventoryHud = createHudShell(false);
        inventoryHud.setSpacing(10);
        radarCard = createHudCard("Radar", ArtifactVisuals.createHudIcon(ArtifactType.RADAR, 24), radarValueLabel, "radar", "1");
        shieldCard = createHudCard("Shield", ArtifactVisuals.createHudIcon(ArtifactType.SHIELD, 24), shieldValueLabel, "shield", "2");
        beaconCard = createHudCard("Beacon", ArtifactVisuals.createHudIcon(ArtifactType.BEACON, 24), beaconValueLabel, "beacon", "3");
        elixirCard = createHudCard("Elixir", ArtifactVisuals.createHudIcon(ArtifactType.ELIXIR, 24), elixirsValueLabel, "elixir", "4");
        inventoryHud.getChildren().addAll(radarCard, shieldCard, beaconCard, elixirCard);

        hudRow = new HBox(12, healthHud, inventoryHud);
        if (difficulty != Difficulty.EASY) {
            HBox keyHud = createHudShell(true);
            keyHud.getChildren().add(createHudCard("Key", ArtifactVisuals.createHudIcon(ArtifactType.KEY, 24), keyValueLabel, "key"));
            hudRow.getChildren().add(keyHud);
        }
        hudRow.setPickOnBounds(false);
        hudRow.setMouseTransparent(false);
        hudRow.setAlignment(Pos.CENTER);
        hudRow.setMaxWidth(Region.USE_PREF_SIZE);
        hudRow.setMaxHeight(Region.USE_PREF_SIZE);

        view = new VBox(hudRow);
        view.setPickOnBounds(false);
        view.setMouseTransparent(false);
        view.setAlignment(Pos.BOTTOM_CENTER);
        view.setMaxWidth(Region.USE_PREF_SIZE);
        view.setMaxHeight(Region.USE_PREF_SIZE);

        healthDamageHighlightTimer.setOnFinished(event -> setCardState(healthCard, "hud-card-active", false));
        lastRenderedHealth = new int[]{player.getHealth()};
        refreshShield(player);
        installAutoMinimize();
    }

    public VBox getView() {
        return view;
    }

    public void sync(Player player, LevelIsland levelIsland) {
        int currentHealth = player.getHealth();
        int previousHealth = lastRenderedHealth[0];
        boolean healthChanged = currentHealth != previousHealth;
        boolean damageTaken = currentHealth < previousHealth;
        lastRenderedHealth[0] = currentHealth;

        updateValues(player);

        if (healthChanged) {
            setCardState(healthCard, "hud-card-active", true);
            healthDamageHighlightTimer.stop();
            healthDamageHighlightTimer.playFromStart();
            if (damageTaken) {
                levelIsland.playDamagePulse();
            }
        }
    }

    public void refreshShield(Player player) {
        setCardState(shieldCard, "hud-card-active", player.hasShield());
    }

    public void setRadarActive(boolean active) {
        setCardState(radarCard, "hud-card-active", active);
    }

    public void setRadarWarning(boolean active) {
        setCardState(radarCard, "hud-card-warning", active);
    }

    public void toggleRadarWarning() {
        setCardState(radarCard, "hud-card-warning", !radarCard.getStyleClass().contains("hud-card-warning"));
    }

    public void attachArtifactActions(Consumer<GameAction> actionHandler) {
        attachArtifactAction(radarCard, GameAction.RADAR, actionHandler);
        attachArtifactAction(shieldCard, GameAction.SHIELD, actionHandler);
        attachArtifactAction(beaconCard, GameAction.BEACON, actionHandler);
        attachArtifactAction(elixirCard, GameAction.ELIXIR, actionHandler);
    }

    public void dispose() {
        Object runningAnimation = view.getProperties().remove(MINIMIZE_ANIMATION_KEY);
        if (runningAnimation instanceof Timeline timeline) {
            timeline.stop();
        }
        stopHotkeyReveal();
        healthDamageHighlightTimer.stop();
        autoMinimizeTimer.stop();
        if (tabPressedHandler != null) {
            scene.removeEventFilter(KeyEvent.KEY_PRESSED, tabPressedHandler);
            tabPressedHandler = null;
        }
        if (tabReleasedHandler != null) {
            scene.removeEventFilter(KeyEvent.KEY_RELEASED, tabReleasedHandler);
            tabReleasedHandler = null;
        }
    }

    private HBox createHudShell(boolean mouseTransparent) {
        HBox hud = new HBox();
        hud.getStyleClass().add("game-hud");
        hud.setPickOnBounds(false);
        hud.setMouseTransparent(mouseTransparent);
        hud.setMaxWidth(Region.USE_PREF_SIZE);
        hud.setMaxHeight(Region.USE_PREF_SIZE);
        hud.setPrefHeight(Region.USE_COMPUTED_SIZE);
        hud.setPadding(new Insets(14, 18, 14, 18));
        return hud;
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
        hotkeyLabel.getStyleClass().addAll("hud-hotkey-badge", ARTIFACT_HOTKEY_CLASS);
        hotkeyLabel.setOpacity(0.0);

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
        titleRow.getStyleClass().add("hud-card-title-row");
        StackPane.setAlignment(titleLabel, Pos.CENTER_LEFT);
        StackPane.setAlignment(hotkeyLabel, Pos.CENTER_RIGHT);
        titleRow.setMaxWidth(Double.MAX_VALUE);

        iconNode.getStyleClass().add("hud-card-icon");

        HBox valueRow = new HBox(8, iconNode, valueLabel);
        valueRow.getStyleClass().add("hud-card-value-row");
        valueRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(5, titleRow, valueRow);
        card.getStyleClass().addAll("hud-card", "hud-" + accentStyleClass);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(8, 12, 8, 12));
        card.setMinWidth(CARD_EXPANDED_MIN_WIDTH);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private static Label createHudValueLabel() {
        Label valueLabel = new Label();
        valueLabel.getStyleClass().add("hud-card-value");
        valueLabel.setMinWidth(Region.USE_PREF_SIZE);
        return valueLabel;
    }

    private static Label createHealthIcon() {
        Label iconLabel = new Label("❤");
        iconLabel.getStyleClass().addAll("hud-card-icon", "hud-icon-health");
        return iconLabel;
    }

    private void updateValues(Player player) {
        hpValueLabel.setText(player.getHealth() + " / " + PLAYER_MAX_HEALTH);
        crystalsValueLabel.setText(String.valueOf(player.getCrystals()));
        radarValueLabel.setText(String.valueOf(player.getRadarCharges()));
        shieldValueLabel.setText(String.valueOf(player.getShieldCount()));
        beaconValueLabel.setText(String.valueOf(player.getBeaconCount()));
        elixirsValueLabel.setText(String.valueOf(player.getElixirCount()));
        keyValueLabel.setText(player.hasKey() ? "Found" : "Empty");
    }

    private void attachArtifactAction(VBox card, GameAction action, Consumer<GameAction> actionHandler) {
        card.setOnMouseClicked(event -> {
            actionHandler.accept(action);
            scene.getRoot().requestFocus();
        });
    }

    private void installAutoMinimize() {
        boolean[] canMinimize = {false};
        boolean[] tabExpanding = {false};

        scheduleHotkeyReveal();

        autoMinimizeTimer.setOnFinished(event -> {
            canMinimize[0] = true;
            if (!tabExpanding[0]) {
                setMinimized(true);
            }
        });

        view.setOnMouseEntered(event -> {
            if (canMinimize[0]) {
                setMinimized(false);
            }
        });

        view.setOnMouseExited(event -> {
            if (canMinimize[0]) {
                setMinimized(true);
            }
        });

        tabPressedHandler = event -> {
            if (event.getCode() != KeyCode.TAB) {
                return;
            }

            tabExpanding[0] = true;
            if (canMinimize[0]) {
                setMinimized(false);
            }
            event.consume();
        };

        tabReleasedHandler = event -> {
            if (event.getCode() != KeyCode.TAB) {
                return;
            }

            tabExpanding[0] = false;
            if (canMinimize[0]) {
                setMinimized(true);
            }
            event.consume();
        };

        scene.addEventFilter(KeyEvent.KEY_PRESSED, tabPressedHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, tabReleasedHandler);
        autoMinimizeTimer.playFromStart();
    }

    private void setMinimized(boolean minimized) {
        Object runningAnimation = view.getProperties().get(MINIMIZE_ANIMATION_KEY);
        if (runningAnimation instanceof Timeline timeline) {
            timeline.stop();
        }
        hideArtifactHotkeys();
        view.setClip(null);

        setCardState(hudRow, "hud-row-minimized", false);

        boolean startMinimized = Boolean.TRUE.equals(view.getProperties().get(MINIMIZED_STATE_KEY));
        HudSize startSize = measureCurrentSize();
        HudSize expandedSize = measureSizeForState(false, startMinimized);
        HudSize minimizedSize = measureSizeForState(true, startMinimized);
        HudSize targetSize = minimized
                ? new HudSize(getManualMinimizedWidth(expandedSize.width()), minimizedSize.height())
                : expandedSize;

        setFixedSize(view, startSize.width(), startSize.height());
        List<HudTitleRowAnimation> titleRowAnimations = prepareTitleRowAnimations(minimized);
        prepareCompactLayout(hudRow, minimized);
        view.setClip(createAnimationClip());

        Interpolator hudInterpolator = Interpolator.SPLINE(0.22, 0.0, 0.16, 1.0);
        double targetScale = minimized ? MINIMIZED_SCALE : 1.0;
        List<KeyValue> startValues = new ArrayList<>();
        startValues.add(new KeyValue(view.minWidthProperty(), startSize.width(), hudInterpolator));
        startValues.add(new KeyValue(view.prefWidthProperty(), startSize.width(), hudInterpolator));
        startValues.add(new KeyValue(view.maxWidthProperty(), startSize.width(), hudInterpolator));
        startValues.add(new KeyValue(view.minHeightProperty(), startSize.height(), hudInterpolator));
        startValues.add(new KeyValue(view.prefHeightProperty(), startSize.height(), hudInterpolator));
        startValues.add(new KeyValue(view.maxHeightProperty(), startSize.height(), hudInterpolator));
        startValues.add(new KeyValue(view.scaleXProperty(), view.getScaleX(), hudInterpolator));
        startValues.add(new KeyValue(view.scaleYProperty(), view.getScaleY(), hudInterpolator));
        startValues.add(new KeyValue(view.opacityProperty(), view.getOpacity(), hudInterpolator));
        startValues.add(new KeyValue(view.translateYProperty(), view.getTranslateY(), hudInterpolator));

        List<KeyValue> targetValues = new ArrayList<>();
        targetValues.add(new KeyValue(view.minWidthProperty(), targetSize.width(), hudInterpolator));
        targetValues.add(new KeyValue(view.prefWidthProperty(), targetSize.width(), hudInterpolator));
        targetValues.add(new KeyValue(view.maxWidthProperty(), targetSize.width(), hudInterpolator));
        targetValues.add(new KeyValue(view.minHeightProperty(), targetSize.height(), hudInterpolator));
        targetValues.add(new KeyValue(view.prefHeightProperty(), targetSize.height(), hudInterpolator));
        targetValues.add(new KeyValue(view.maxHeightProperty(), targetSize.height(), hudInterpolator));
        targetValues.add(new KeyValue(view.scaleXProperty(), targetScale, hudInterpolator));
        targetValues.add(new KeyValue(view.scaleYProperty(), targetScale, hudInterpolator));
        targetValues.add(new KeyValue(view.opacityProperty(), minimized ? 0.94 : 1.0, hudInterpolator));
        targetValues.add(new KeyValue(view.translateYProperty(), minimized ? 10.0 : 0.0, hudInterpolator));

        for (HudTitleRowAnimation titleRowAnimation : titleRowAnimations) {
            Region titleRow = titleRowAnimation.titleRow();
            startValues.add(new KeyValue(titleRow.minWidthProperty(), titleRowAnimation.startWidth(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.prefWidthProperty(), titleRowAnimation.startWidth(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.maxWidthProperty(), titleRowAnimation.startWidth(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.minHeightProperty(), titleRowAnimation.startHeight(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.prefHeightProperty(), titleRowAnimation.startHeight(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.maxHeightProperty(), titleRowAnimation.startHeight(), hudInterpolator));
            startValues.add(new KeyValue(titleRow.opacityProperty(), titleRowAnimation.startOpacity(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.minWidthProperty(), titleRowAnimation.targetWidth(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.prefWidthProperty(), titleRowAnimation.targetWidth(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.maxWidthProperty(), titleRowAnimation.targetWidth(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.minHeightProperty(), titleRowAnimation.targetHeight(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.prefHeightProperty(), titleRowAnimation.targetHeight(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.maxHeightProperty(), titleRowAnimation.targetHeight(), hudInterpolator));
            targetValues.add(new KeyValue(titleRow.opacityProperty(), titleRowAnimation.targetOpacity(), hudInterpolator));
        }

        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO, startValues.toArray(new KeyValue[0])),
                new KeyFrame(MINIMIZE_ANIMATION_DURATION, targetValues.toArray(new KeyValue[0]))
        );
        view.getProperties().put(MINIMIZE_ANIMATION_KEY, animation);
        animation.setOnFinished(event -> {
            if (view.getProperties().get(MINIMIZE_ANIMATION_KEY) == animation) {
                applyMinimizedState(minimized);
                view.getProperties().put(MINIMIZED_STATE_KEY, minimized);
                if (minimized) {
                    setFixedSize(view, targetSize.width(), targetSize.height());
                } else {
                    view.setMinWidth(Region.USE_COMPUTED_SIZE);
                    view.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    view.setMaxWidth(Region.USE_PREF_SIZE);
                    view.setMinHeight(Region.USE_COMPUTED_SIZE);
                    view.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    view.setMaxHeight(Region.USE_PREF_SIZE);
                    scheduleHotkeyReveal();
                }
                view.setClip(null);
            }
        });
        animation.play();
    }

    private double getManualMinimizedWidth(double expandedWidth) {
        boolean hasKeyHud = hudRow.getChildren().size() > 2;
        double minWidth = hasKeyHud ? MINIMIZED_FULL_MIN_WIDTH : MINIMIZED_EASY_MIN_WIDTH;
        double maxWidth = hasKeyHud ? MINIMIZED_FULL_MAX_WIDTH : MINIMIZED_EASY_MAX_WIDTH;
        return Math.ceil(Math.max(minWidth, Math.min(maxWidth, expandedWidth * MINIMIZED_WIDTH_RATIO)));
    }

    private Rectangle createAnimationClip() {
        Rectangle clip = new Rectangle();
        clip.setX(-ANIMATION_CLIP_PADDING);
        clip.setY(-ANIMATION_CLIP_PADDING);
        clip.widthProperty().bind(view.widthProperty().add(ANIMATION_CLIP_PADDING * 2));
        clip.heightProperty().bind(view.heightProperty().add(ANIMATION_CLIP_PADDING * 2));
        return clip;
    }

    private HudSize measureCurrentSize() {
        HudSize measuredSize = measureSize();
        double width = view.getWidth() > 0 ? view.getWidth() : measuredSize.width();
        double height = view.getHeight() > 0 ? view.getHeight() : measuredSize.height();
        return new HudSize(Math.ceil(width), Math.ceil(height));
    }

    private HudSize measureSizeForState(boolean minimized, boolean restoreMinimized) {
        applyMinimizedState(minimized);
        HudSize size = measureSize();
        applyMinimizedState(restoreMinimized);
        view.applyCss();
        return size;
    }

    private HudSize measureSize() {
        view.setMinWidth(Region.USE_COMPUTED_SIZE);
        view.setPrefWidth(Region.USE_COMPUTED_SIZE);
        view.setMaxWidth(Region.USE_PREF_SIZE);
        view.setMinHeight(Region.USE_COMPUTED_SIZE);
        view.setPrefHeight(Region.USE_COMPUTED_SIZE);
        view.setMaxHeight(Region.USE_PREF_SIZE);
        view.applyCss();
        return new HudSize(Math.ceil(view.prefWidth(-1)), Math.ceil(view.prefHeight(-1)));
    }

    private static void setFixedSize(Region region, double width, double height) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
        region.setMinHeight(height);
        region.setPrefHeight(height);
        region.setMaxHeight(height);
    }

    private List<HudTitleRowAnimation> prepareTitleRowAnimations(boolean minimized) {
        List<HudTitleRowAnimation> animations = new ArrayList<>();
        for (Region titleRow : findTitleRows(hudRow)) {
            double expandedHeight = measureExpandedHeight(titleRow);
            double expandedWidth = measureExpandedWidth(titleRow);
            double currentHeight = titleRow.isManaged() && titleRow.getHeight() > 0 ? titleRow.getHeight() : 0.0;
            double currentWidth = titleRow.isManaged() && titleRow.getWidth() > 0 ? titleRow.getWidth() : 0.0;
            double startHeight = Math.ceil(currentHeight);
            double startWidth = Math.ceil(currentWidth);
            double targetHeight = minimized ? 0.0 : expandedHeight;
            double targetWidth = minimized ? 0.0 : expandedWidth;
            double startOpacity = titleRow.getOpacity();
            double targetOpacity = minimized ? 0.0 : 1.0;

            titleRow.setManaged(true);
            setFixedSize(titleRow, startWidth, startHeight);
            titleRow.setOpacity(startOpacity);
            animations.add(new HudTitleRowAnimation(
                    titleRow,
                    startWidth,
                    targetWidth,
                    startHeight,
                    targetHeight,
                    startOpacity,
                    targetOpacity
            ));
        }
        return animations;
    }

    private static double measureExpandedHeight(Region titleRow) {
        double minHeight = titleRow.getMinHeight();
        double prefHeight = titleRow.getPrefHeight();
        double maxHeight = titleRow.getMaxHeight();
        titleRow.setMinHeight(Region.USE_COMPUTED_SIZE);
        titleRow.setPrefHeight(Region.USE_COMPUTED_SIZE);
        titleRow.setMaxHeight(Region.USE_COMPUTED_SIZE);
        titleRow.applyCss();
        double measuredHeight = Math.ceil(titleRow.prefHeight(-1));
        titleRow.setMinHeight(minHeight);
        titleRow.setPrefHeight(prefHeight);
        titleRow.setMaxHeight(maxHeight);
        return measuredHeight;
    }

    private static double measureExpandedWidth(Region titleRow) {
        double minWidth = titleRow.getMinWidth();
        double prefWidth = titleRow.getPrefWidth();
        double maxWidth = titleRow.getMaxWidth();
        titleRow.setMinWidth(Region.USE_COMPUTED_SIZE);
        titleRow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        titleRow.setMaxWidth(Region.USE_COMPUTED_SIZE);
        titleRow.applyCss();
        double measuredWidth = Math.ceil(titleRow.prefWidth(-1));
        titleRow.setMinWidth(minWidth);
        titleRow.setPrefWidth(prefWidth);
        titleRow.setMaxWidth(maxWidth);
        return measuredWidth;
    }

    private static List<Region> findTitleRows(Node node) {
        List<Region> titleRows = new ArrayList<>();
        collectTitleRows(node, titleRows);
        return titleRows;
    }

    private static void collectTitleRows(Node node, List<Region> titleRows) {
        if (node instanceof Region region && node.getStyleClass().contains("hud-card-title-row")) {
            titleRows.add(region);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectTitleRows(child, titleRows);
            }
        }
    }

    private void scheduleHotkeyReveal() {
        stopHotkeyReveal();
        setArtifactHotkeyOpacity(0.0);

        PauseTransition revealDelay = new PauseTransition(HOTKEY_REVEAL_DELAY);
        revealDelay.setOnFinished(event -> {
            Timeline revealAnimation = new Timeline(
                    new KeyFrame(
                            HOTKEY_REVEAL_DURATION,
                            getArtifactHotkeys(hudRow).stream()
                                    .map(hotkey -> new KeyValue(hotkey.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
                                    .toArray(KeyValue[]::new)
                    )
            );
            view.getProperties().put(HOTKEY_REVEAL_ANIMATION_KEY, revealAnimation);
            revealAnimation.setOnFinished(done -> {
                if (view.getProperties().get(HOTKEY_REVEAL_ANIMATION_KEY) == revealAnimation) {
                    view.getProperties().remove(HOTKEY_REVEAL_ANIMATION_KEY);
                }
            });
            revealAnimation.playFromStart();
        });
        view.getProperties().put(HOTKEY_REVEAL_TIMER_KEY, revealDelay);
        revealDelay.playFromStart();
    }

    private void hideArtifactHotkeys() {
        stopHotkeyReveal();
        setArtifactHotkeyOpacity(0.0);
    }

    private void stopHotkeyReveal() {
        Object revealDelay = view.getProperties().remove(HOTKEY_REVEAL_TIMER_KEY);
        if (revealDelay instanceof PauseTransition pauseTransition) {
            pauseTransition.stop();
        }

        Object revealAnimation = view.getProperties().remove(HOTKEY_REVEAL_ANIMATION_KEY);
        if (revealAnimation instanceof Timeline timeline) {
            timeline.stop();
        }
    }

    private void setArtifactHotkeyOpacity(double opacity) {
        for (Label hotkey : getArtifactHotkeys(hudRow)) {
            hotkey.setOpacity(opacity);
        }
    }

    private static List<Label> getArtifactHotkeys(Node node) {
        List<Label> hotkeys = new ArrayList<>();
        collectArtifactHotkeys(node, hotkeys);
        return hotkeys;
    }

    private static void collectArtifactHotkeys(Node node, List<Label> hotkeys) {
        if (node instanceof Label label && label.getStyleClass().contains(ARTIFACT_HOTKEY_CLASS)) {
            hotkeys.add(label);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                collectArtifactHotkeys(child, hotkeys);
            }
        }
    }

    private void applyMinimizedState(boolean minimized) {
        applyStyleState(minimized);
        applyTitleRowState(minimized);
    }

    private void applyStyleState(boolean minimized) {
        prepareCompactLayout(hudRow, minimized);
        setCardState(hudRow, "hud-row-minimized", false);
    }

    private void applyTitleRowState(boolean minimized) {
        for (Region titleRow : findTitleRows(hudRow)) {
            titleRow.setManaged(true);
            titleRow.setOpacity(minimized ? 0.0 : 1.0);
            if (minimized) {
                setFixedSize(titleRow, 0.0, 0.0);
            } else {
                titleRow.setMinWidth(Region.USE_COMPUTED_SIZE);
                titleRow.setPrefWidth(Region.USE_COMPUTED_SIZE);
                titleRow.setMaxWidth(Region.USE_COMPUTED_SIZE);
                titleRow.setMinHeight(Region.USE_COMPUTED_SIZE);
                titleRow.setPrefHeight(Region.USE_COMPUTED_SIZE);
                titleRow.setMaxHeight(Region.USE_COMPUTED_SIZE);
            }
        }
    }

    private static void prepareCompactLayout(Node node, boolean minimized) {
        if (node instanceof Region region && region.getStyleClass().contains("hud-card")) {
            if (minimized && region.getStyleClass().contains("hud-health")) {
                region.setMinWidth(MINIMIZED_HEALTH_CARD_MIN_WIDTH);
            } else {
                region.setMinWidth(minimized ? Region.USE_COMPUTED_SIZE : CARD_EXPANDED_MIN_WIDTH);
            }
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                prepareCompactLayout(child, minimized);
            }
        }
    }

    private static void setCardState(Node card, String styleClass, boolean active) {
        if (active) {
            if (!card.getStyleClass().contains(styleClass)) {
                card.getStyleClass().add(styleClass);
            }
            return;
        }
        card.getStyleClass().remove(styleClass);
    }

    private record HudSize(double width, double height) {
    }

    private record HudTitleRowAnimation(
            Region titleRow,
            double startWidth,
            double targetWidth,
            double startHeight,
            double targetHeight,
            double startOpacity,
            double targetOpacity
    ) {
    }
}
