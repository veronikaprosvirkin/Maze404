package model;

import AI.IEnemyAI; // Обов'язково додаємо імпорт!
import enums.EnemyMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Enemy {
    private int row;
    private int col;
    private EnemyMode mode;
    private List<Position> patrolPath;
    private IEnemyAI ai;

    public Enemy(int row, int col, EnemyMode mode, IEnemyAI ai) {
        this.row = row;
        this.col = col;
        this.mode = mode;
        this.ai = ai;
    }

    public List<Position> getPatrolRoute() {
        return patrolPath;
    }
}