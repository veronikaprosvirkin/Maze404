import javafx.application.Application;
import logic.generation.MazeGenerator;
import model.Grid;

public class Main {
    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }

    // Для тестування генерації лабіринту
    /*public static void main(String[] args) {
        MazeGenerator gen = new MazeGenerator();
        Grid grid = gen.generate(11, 11);

        for (int r = 0; r < grid.getHeight(); r++) {
            for (int c = 0; c < grid.getWidth(); c++) {
                switch (grid.getCell(r, c).getType()) {
                    case WALL  -> System.out.print("██");
                    case FLOOR -> System.out.print("  ");
                    case EXIT  -> System.out.print("EX");
                    case TRAP  -> System.out.print("TT");
                }
            }
            System.out.println();
        }
    }*/
}

