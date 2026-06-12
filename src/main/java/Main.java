import javafx.application.Application;
import Utilities.AppLog;

public class Main {
    public static void main(String[] args) {
        AppLog.initialize();
        Application.launch(MainApp.class, args);
    }
}
