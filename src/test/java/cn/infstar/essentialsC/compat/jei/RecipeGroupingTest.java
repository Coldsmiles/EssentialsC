package cn.infstar.essentialsC.compat.jei;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeGroupingTest {

    @Test
    void groupsValuesInOnePassAndPreservesOrder() {
        List<String> recipes = List.of("crafting:first", "smelting:second", "crafting:third");

        Map<String, List<String>> grouped = RecipeGrouping.groupBy(
            recipes, value -> value.substring(0, value.indexOf(':')));

        assertEquals(List.of("crafting", "smelting"), grouped.keySet().stream().toList());
        assertEquals(List.of("crafting:first", "crafting:third"), grouped.get("crafting"));
        assertEquals(List.of("smelting:second"), grouped.get("smelting"));
    }
}
