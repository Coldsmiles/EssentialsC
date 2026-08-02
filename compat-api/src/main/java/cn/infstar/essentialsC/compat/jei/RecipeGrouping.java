package cn.infstar.essentialsC.compat.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class RecipeGrouping {

    private RecipeGrouping() {
    }

    public static <K, V> Map<K, List<V>> groupBy(Iterable<V> values,
                                                  Function<? super V, ? extends K> keyFunction) {
        Map<K, List<V>> grouped = new LinkedHashMap<>();
        for (V value : values) {
            grouped.computeIfAbsent(keyFunction.apply(value), ignored -> new ArrayList<>()).add(value);
        }
        return grouped;
    }
}
