import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Enemy {
    private int row;
    private int col;
    private EnemyMode mode;

    public enum EnemyMode {
        PATROL,
        CHASE
    }
}