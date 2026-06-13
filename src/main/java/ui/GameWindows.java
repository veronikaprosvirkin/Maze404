package ui;

import javafx.application.Platform;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class GameWindows {
    private static Stage primaryStage;
    private static boolean shuttingDown;

    private GameWindows() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
        shuttingDown = false;
    }

    public static void beginShutdown() {
        shuttingDown = true;
    }

    public static void configureChildStage(Stage stage) {
        Window owner = getOwner();
        if (owner == null || stage.getOwner() != null) {
            return;
        }

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnHidden(event -> restoreOwnerFocus(owner));
    }

    public static void configureDialog(Dialog<?> dialog) {
        Window owner = getOwner();
        if (owner == null || dialog.getOwner() != null) {
            return;
        }

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnHidden(event -> restoreOwnerFocus(owner));
    }

    private static Window getOwner() {
        return primaryStage != null && primaryStage.isShowing() ? primaryStage : null;
    }

    private static void restoreOwnerFocus(Window owner) {
        if (shuttingDown || owner == null) {
            return;
        }

        Platform.runLater(() -> {
            if (!shuttingDown && owner.isShowing()) {
                owner.requestFocus();
            }
        });
    }
}
