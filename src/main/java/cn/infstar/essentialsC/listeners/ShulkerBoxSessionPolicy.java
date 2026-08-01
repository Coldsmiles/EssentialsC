package cn.infstar.essentialsC.listeners;

import java.util.Set;

final class ShulkerBoxSessionPolicy {

    private ShulkerBoxSessionPolicy() {
    }

    static boolean isTopSlot(int rawSlot, int topInventorySize) {
        return rawSlot >= 0 && rawSlot < topInventorySize;
    }

    static boolean touchesTopInventory(Set<Integer> rawSlots, int topInventorySize) {
        return rawSlots.stream().anyMatch(rawSlot -> isTopSlot(rawSlot, topInventorySize));
    }
}
