package ui;

import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public final class GameWindows {
    private static Stage primaryStage;

    private GameWindows() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void configureChildStage(Stage stage) {
        Window owner = getOwner();
        if (owner == null || stage.getOwner() != null) {
            return;
        }

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnHidden(event -> owner.requestFocus());
    }

    public static void configureDialog(Dialog<?> dialog) {
        Window owner = getOwner();
        if (owner == null || dialog.getOwner() != null) {
            return;
        }

        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setOnHidden(event -> owner.requestFocus());
    }

    private static Window getOwner() {
        return primaryStage != null && primaryStage.isShowing() ? primaryStage : null;
    }
}
