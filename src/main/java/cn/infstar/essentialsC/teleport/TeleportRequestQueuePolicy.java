package cn.infstar.essentialsC.teleport;

import java.util.Deque;

final class TeleportRequestQueuePolicy {

    private TeleportRequestQueuePolicy() {
    }

    static <T> void addFirstBounded(Deque<T> queue, T value, int maximumSize) {
        while (queue.size() >= maximumSize) {
            queue.removeLast();
        }
        queue.addFirst(value);
    }

    static boolean shouldPrune(long expiresAtMillis, long nowMillis, long retentionMillis) {
        return expiresAtMillis <= nowMillis - retentionMillis;
    }
}
