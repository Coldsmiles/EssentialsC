package cn.infstar.essentialsC.teleport;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportRequestQueuePolicyTest {

    @Test
    void keepsNewestRequestsWithinConfiguredLimit() {
        Deque<String> requests = new ArrayDeque<>();

        TeleportRequestQueuePolicy.addFirstBounded(requests, "first", 2);
        TeleportRequestQueuePolicy.addFirstBounded(requests, "second", 2);
        TeleportRequestQueuePolicy.addFirstBounded(requests, "third", 2);

        assertEquals(java.util.List.of("third", "second"), java.util.List.copyOf(requests));
    }

    @Test
    void retainsRecentlyExpiredRequestsForUserFeedback() {
        long now = 120_000L;

        assertFalse(TeleportRequestQueuePolicy.shouldPrune(90_000L, now, 60_000L));
        assertTrue(TeleportRequestQueuePolicy.shouldPrune(60_000L, now, 60_000L));
    }
}
