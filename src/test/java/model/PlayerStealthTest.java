package model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStealthTest {

    @Test
    void playerBecomesSemiInvisibleAfterStandingStillFiveSeconds() {
        Player player = new Player(1, 1);
        long nowNanos = System.nanoTime();

        assertFalse(player.isSemiInvisible(nowNanos + 4_000_000_000L));
        assertTrue(player.isSemiInvisible(nowNanos + 5_000_000_000L));
    }

    @Test
    void movingResetsSemiInvisibleTimer() {
        Player player = new Player(1, 1);
        long beforeMove = System.nanoTime();

        assertTrue(player.isSemiInvisible(beforeMove + 5_000_000_000L));

        player.setCol(2);
        long afterMove = System.nanoTime();

        assertFalse(player.isSemiInvisible(afterMove + 4_000_000_000L));
        assertTrue(player.isSemiInvisible(afterMove + 5_000_000_000L));
    }

    @Test
    void semiInvisiblePlayerIsImmuneToAllDamage() {
        Player player = new Player(1, 1);
        long nowNanos = System.nanoTime();
        long invisibleAt = nowNanos + 5_000_000_000L;

        player.takeDamage(1, invisibleAt);
        player.takeEnemyDamage(1, invisibleAt);

        assertEquals(3, player.getHealth());
    }
}
