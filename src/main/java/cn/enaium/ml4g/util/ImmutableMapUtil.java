package cn.enaium.ml4g.util;

import java.util.HashMap;

/**
 * @author Enaium
 */
public class ImmutableMapUtil {
    @SuppressWarnings("unchecked")
    public static <K, V> HashMap<K, V> of(Object... kv) {
        HashMap<K, V> hashMap = new HashMap<>();
        for (int i = 0; i < kv.length; i = i + 2) {

            hashMap.put((K) kv[i], (V) kv[i + 1]);
        }
        return hashMap;
    }
}
