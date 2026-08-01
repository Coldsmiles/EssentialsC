package cn.infstar.essentialsC.listeners;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShulkerBoxSessionPolicyTest {

    @Test
    void identifiesOnlySlotsInsideTopInventory() {
        assertFalse(ShulkerBoxSessionPolicy.isTopSlot(-999, 27));
        assertTrue(ShulkerBoxSessionPolicy.isTopSlot(0, 27));
        assertTrue(ShulkerBoxSessionPolicy.isTopSlot(26, 27));
        assertFalse(ShulkerBoxSessionPolicy.isTopSlot(27, 27));
    }

    @Test
    void detectsDragsThatTouchTopInventory() {
        assertTrue(ShulkerBoxSessionPolicy.touchesTopInventory(Set.of(5, 30), 27));
        assertFalse(ShulkerBoxSessionPolicy.touchesTopInventory(Set.of(27, 35), 27));
    }
}
