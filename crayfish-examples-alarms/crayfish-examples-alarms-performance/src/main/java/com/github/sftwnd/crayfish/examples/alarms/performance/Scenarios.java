package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The scenarios of {@link VersionsComparison}. A JVM runs one scenario with one version of crayfish-alarms on its
 * class path and prints the results as lines {@code RESULT scenario=... case=... name=value ...}. The scenarios use
 * the JDK and the API that the versions share, and no other library: the JVM has only the jars of the version.
 * <ul>
 *     <li>cost: the costs of a time range in one thread, see {@link Costs};</li>
 *     <li>timerange: a load of a time range that one thread fills and extracts, see {@link Load#timeRange()};</li>
 *     <li>service: a load of {@code AlarmTimeRangeService}, see {@link Load#service()};</li>
 *     <li>sparse: rare alarms of {@code AlarmTimeRangeService}, see {@link Load#sparse()}.</li>
 * </ul>
 */
public final class Scenarios {

    // 1.0.4 logs the interrupt that stops a process as a warning; the logger is kept, so that its level stays
    private static final Logger ALARMS = Logger.getLogger("com.github.sftwnd.crayfish.alarms");

    private Scenarios() {
    }

    public static void main(String[] args) throws InterruptedException {
        ALARMS.setLevel(Level.OFF);
        String scenario = args.length > 0 ? args[0] : "";
        switch (scenario) {
            case "cost":
                Costs.run();
                break;
            case "timerange":
                Load.timeRange();
                break;
            case "service":
                Load.service();
                break;
            case "sparse":
                Load.sparse();
                break;
            default:
                throw new IllegalArgumentException("Unknown scenario: " + scenario);
        }
        // a process that has not stopped must not keep the JVM
        System.exit(0);
    }

    static void result(String scenario, String key, Object... namesAndValues) {
        StringBuilder line = new StringBuilder("RESULT scenario=").append(scenario).append(" case=").append(key);
        for (int i = 0; i + 1 < namesAndValues.length; i += 2) {
            line.append(' ').append(namesAndValues[i]).append('=').append(namesAndValues[i + 1]);
        }
        System.out.println(line);
        System.out.flush();
    }

}
