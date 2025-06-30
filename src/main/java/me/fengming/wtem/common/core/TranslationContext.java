package me.fengming.wtem.common.core;

import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author FengMing
 */
public class TranslationContext {
    private static final Object2IntMap<String> counts = new Object2IntOpenHashMap<>();
    private static final Map<String, String> languageMap = new HashMap<>();
    private static String key = "no_key";

    public static void clear() {
        counts.clear();
        languageMap.clear();
        key = "no_key";
    }

    public static void setKey(String key) {
        counts.putIfAbsent(key, 0);
        TranslationContext.key = key;
    }

    public static String nextKey() {
        counts.computeInt(key, (k, v) -> v + 1);
        return key + "." + counts.getInt(key);
    }

    public static void append(String key1) {
        setKey(key + "." + key1);
    }

    public static void revertAndAppend(String key1) {
        revert();
        append(key1);
    }

    public static void revert() {
        String[] sp = key.split("\\.");
        setKey(String.join(".", Arrays.copyOfRange(sp, 0, sp.length - 1)));
    }

    public static void addKey(String key, String value) {
        languageMap.putIfAbsent(key, value);
    }

    public static String exportLanguage() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(languageMap);
    }
}
