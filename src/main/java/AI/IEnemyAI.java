package AI;

import model.Enemy;
import model.GameState;
import model.Position;

public interface IEnemyAI {
    /**
     * Обчислює наступну позицію дрона.
     * @param state  поточний стан гри (тільки читання)
     * @param enemy  дрон, для якого рахуємо хід
     * @return нова Position дрона, або поточна якщо рух неможливий
     */
    Position computeNextMove(GameState state, Enemy enemy);
}