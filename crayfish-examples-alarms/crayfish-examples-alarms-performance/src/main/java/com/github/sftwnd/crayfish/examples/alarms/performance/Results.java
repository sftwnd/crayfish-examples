package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The results of the runs: by scenario, case and version, a map of the values of every round. */
final class Results {

    private final Map<String, Map<String, Map<String, List<Map<String, String>>>>> scenarios = new LinkedHashMap<>();

    void add(String scenario, String key, String version, Map<String, String> values) {
        scenarios.computeIfAbsent(scenario, s -> new LinkedHashMap<>())
                .computeIfAbsent(key, k -> new LinkedHashMap<>())
                .computeIfAbsent(version, v -> new ArrayList<>())
                .add(values);
    }

    boolean has(String scenario) {
        return scenarios.containsKey(scenario);
    }

    List<String> keys(String scenario) {
        return new ArrayList<>(scenarios.getOrDefault(scenario, Collections.emptyMap()).keySet());
    }

    List<Map<String, String>> runs(String scenario, String key, String version) {
        return scenarios.getOrDefault(scenario, Collections.emptyMap())
                .getOrDefault(key, Collections.emptyMap())
                .getOrDefault(version, Collections.emptyList());
    }

}
