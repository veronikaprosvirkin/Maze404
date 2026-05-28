package model;

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
}