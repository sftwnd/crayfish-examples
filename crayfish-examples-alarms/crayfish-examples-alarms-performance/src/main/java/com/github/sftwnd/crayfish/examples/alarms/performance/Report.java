package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The report of a comparison in Markdown: the medians of the rounds of every version. */
final class Report {

    private static final String NONE = "—";

    // the case, the value, the measure, and whether a higher value is better
    private static final String[][] COSTS = {
            {"add-1s", "nsPerAlarm", "addElements, chunks of 1 s: ns per alarm", "lower"},
            {"add-60s", "nsPerAlarm", "addElements, one chunk of 60 s: ns per alarm", "lower"},
            {"extract-1s-10000", "usPerExtraction", "An extraction of 1 alarm, 10,000 waiting, chunks of 1 s: µs", "lower"},
            {"extract-1s-100000", "usPerExtraction", "An extraction of 1 alarm, 100,000 waiting, chunks of 1 s: µs", "lower"},
            {"extract-60s-10000", "usPerExtraction", "An extraction of 1 alarm, 10,000 waiting, one chunk of 60 s: µs", "lower"},
            {"extract-60s-100000", "usPerExtraction", "An extraction of 1 alarm, 100,000 waiting, one chunk of 60 s: µs", "lower"},
            {"drain-1s", "alarmsPerSecond", "Extractions every 10 ms, chunks of 1 s: alarms per second of work", "higher"},
            {"calls", "perAdded", "Calls of the expectation per alarm added", "lower"},
            {"calls", "perExtraction", "Calls of the expectation per extraction", "lower"},
    };

    private static final String TIME_RANGE = "One thread registers the alarms in calls of `addElements` of 100, extracts "
            + "the fired ones and parks until the next call or alarm, as a process does, without a service. The range "
            + "has chunks of 1 s; an alarm is due 0.5 to 1.5 s after its registration.";
    private static final String SERVICE = "Four threads register the alarms in calls of `addElements` of 100, the "
            + "process of `AlarmTimeRangeService` with the default minimalWait fires them. The range has chunks of 1 s; "
            + "an alarm is due 0.5 to 1.5 s after its registration.";
    private static final String SPARSE = "One thread registers 50 alarms per second one by one for 20 s, each due 10 "
            + "to 100 ms later. The default minimalWait of 1.0.4 is 75 ms, a busy wait; that of 1.0.5 is none.";
    private static final String KEPT_UP = "A version keeps up with a rate if it fires at least 95% of it, the 99th "
            + "percentile of the delays is at most 100 ms and every alarm is fired; its steps stop at the first rate it "
            + "does not keep up with.";
    private static final String COLUMNS = "Alarms/s fired: in the window in which the alarms are due at the full "
            + "rate. Delay: from the moment an alarm is due to the moment the consumer gets it. Backlog: the most alarms "
            + "due and not fired. Drain: from the last due moment to the last alarm fired. CPU: of the thread that "
            + "fires the alarms, in percent of a core. JVM: the CPU time of the JVM. B/alarm: the bytes that the "
            + "thread of the alarms allocates per alarm. Not fired: 10 s after the last alarm is due. Out of order: "
            + "the alarms fired after an alarm due later.";
    private static final String REGISTRATION = "Registration p99: the time from a call of `addElements` to the "
            + "completion of its stage.";

    private final List<String> versions;
    private final int rounds;
    private final Results results;
    private boolean columnsDescribed;

    Report(List<String> versions, int rounds, Results results) {
        this.versions = versions;
        this.rounds = rounds;
        this.results = results;
    }

    String markdown(String environment) {
        StringBuilder out = new StringBuilder();
        out.append("# crayfish-alarms ").append(String.join(" and ", versions)).append("\n\n").append(environment);
        if (results.has("timerange") || results.has("service")) {
            keptUp(out);
        }
        if (results.has("cost")) {
            costs(out);
        }
        if (results.has("timerange")) {
            ladder(out, "timerange", "A time range in one thread", TIME_RANGE);
        }
        if (results.has("service")) {
            ladder(out, "service", "AlarmTimeRangeService", SERVICE);
        }
        if (results.has("sparse")) {
            sparse(out);
        }
        return out.toString();
    }

    /** The values of a result for the log. */
    static String summary(Map<String, String> values) {
        if (values.containsKey("throughput")) {
            return String.format(Locale.ROOT, "%s alarms/s fired, delay p50 %s ms, p99 %s ms, max %s ms, backlog %s, "
                            + "CPU %s%%, not fired %s, kept up %s",
                    values.get("throughput"), values.get("p50"), values.get("p99"), values.get("max"),
                    values.get("backlog"), values.get("cpu"), values.get("notFired"), values.get("sustained"));
        }
        StringBuilder summary = new StringBuilder();
        values.forEach((name, value) -> {
            if (!"scenario".equals(name) && !"case".equals(name)) {
                summary.append(summary.length() == 0 ? "" : ", ").append(name).append(' ').append(value);
            }
        });
        return summary.toString();
    }

    private void keptUp(StringBuilder out) {
        out.append("\n## The highest rate kept up with\n\n")
                .append("Alarms per second: the highest rate of the steps that a version kept up with in every round; ")
                .append("≥ if it kept up with the highest one. ").append(KEPT_UP).append("\n\n")
                .append(header("Scenario", versions));
        for (String[] scenario : new String[][] {{"timerange", "A time range in one thread"}, {"service", "AlarmTimeRangeService"}}) {
            if (!results.has(scenario[0])) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            cells.add(scenario[1]);
            for (String version : versions) {
                long kept = -1L;
                long highest = -1L;
                for (String key : results.keys(scenario[0])) {
                    if (!results.runs(scenario[0], key, version).isEmpty()) {
                        highest = Math.max(highest, Long.parseLong(key));
                        if (keptUp(scenario[0], key, version)) {
                            kept = Math.max(kept, Long.parseLong(key));
                        }
                    }
                }
                cells.add(kept < 0 ? "none" : (kept == highest ? "≥ " : "") + integer(kept));
            }
            out.append(row(cells));
        }
    }

    private void costs(StringBuilder out) {
        boolean compare = versions.size() > 1;
        String first = versions.get(0);
        String last = versions.get(versions.size() - 1);
        List<String> titles = new ArrayList<>(versions);
        if (compare) {
            titles.add(last + " vs " + first);
        }
        out.append("\n## The costs of a time range in one thread\n\n")
                .append("A range of a minute with alarms at random moments, and extractions at given moments: ")
                .append("no waits. The last column tells how many times the value of ").append(last)
                .append(" is better than that of ").append(first).append(".\n\n")
                .append(header("Measure", titles));
        for (String[] cost : COSTS) {
            List<String> cells = new ArrayList<>();
            cells.add(cost[2]);
            for (String version : versions) {
                double value = median("cost", cost[0], version, cost[1]);
                cells.add(cost[1].equals("usPerExtraction") ? millis(value) : integer(value));
            }
            if (compare) {
                double from = median("cost", cost[0], first, cost[1]);
                double to = median("cost", cost[0], last, cost[1]);
                cells.add(times("higher".equals(cost[3]) ? to / from : from / to));
            }
            out.append(row(cells));
        }
    }

    private void ladder(StringBuilder out, String scenario, String title, String text) {
        boolean service = "service".equals(scenario);
        List<String> titles = new ArrayList<>(List.of("Version", "Kept up", "Alarms/s fired", "p50, ms", "p99, ms",
                "Max, ms", "Backlog", "Drain, ms", "CPU, %", "JVM, cores", "B/alarm", "GC, ms", "Not fired",
                "Out of order, %"));
        if (service) {
            titles.add("Registration p99, ms");
        }
        out.append("\n## ").append(title).append("\n\n").append(text);
        if (!columnsDescribed) {
            out.append(' ').append(COLUMNS);
            columnsDescribed = true;
        } else {
            out.append(" The columns are those of the table above.");
        }
        if (service) {
            out.append(' ').append(REGISTRATION);
        }
        out.append("\n\n").append(header("Alarms/s registered", titles));
        List<String> keys = results.keys(scenario);
        keys.sort(Comparator.comparingLong(Long::parseLong));
        for (String key : keys) {
            for (String version : versions) {
                List<String> cells = new ArrayList<>();
                cells.add(integer(Long.parseLong(key)));
                cells.add(version);
                if (results.runs(scenario, key, version).isEmpty()) {
                    cells.add("not run");
                    while (cells.size() < titles.size() + 1) {
                        cells.add(NONE);
                    }
                    out.append(row(cells));
                    continue;
                }
                cells.add(keptUpText(scenario, key, version));
                cells.add(integer(median(scenario, key, version, "throughput")));
                cells.add(millis(median(scenario, key, version, "p50")));
                cells.add(millis(median(scenario, key, version, "p99")));
                cells.add(millis(median(scenario, key, version, "max")));
                cells.add(integer(median(scenario, key, version, "backlog")));
                cells.add(millis(median(scenario, key, version, "drain")));
                cells.add(decimal(median(scenario, key, version, "cpu"), 1));
                cells.add(decimal(median(scenario, key, version, "cores"), 2));
                cells.add(integer(median(scenario, key, version, "alloc")));
                cells.add(integer(median(scenario, key, version, "gc")));
                cells.add(integer(median(scenario, key, version, "notFired")));
                cells.add(decimal(median(scenario, key, version, "outOfOrder"), 2));
                if (service) {
                    cells.add(millis(median(scenario, key, version, "regP99")));
                }
                out.append(row(cells));
            }
        }
    }

    private void sparse(StringBuilder out) {
        out.append("\n## Rare alarms of AlarmTimeRangeService\n\n").append(SPARSE)
                .append(" CPU: of the thread of the process, in percent of a core.\n\n")
                .append(header("minimalWait", List.of("Version", "p50, ms", "p99, ms", "p99.9, ms", "Max, ms",
                        "Later than 50 ms", "CPU, %", "JVM, cores", "Not fired", "Out of order, %")));
        for (String key : results.keys("sparse")) {
            for (String version : versions) {
                List<String> cells = new ArrayList<>();
                cells.add("zero".equals(key) ? "0" : "the default");
                cells.add(version);
                cells.add(millis(median("sparse", key, version, "p50")));
                cells.add(millis(median("sparse", key, version, "p99")));
                cells.add(millis(median("sparse", key, version, "p999")));
                cells.add(millis(median("sparse", key, version, "max")));
                cells.add(integer(median("sparse", key, version, "late50")));
                cells.add(decimal(median("sparse", key, version, "cpu"), 1));
                cells.add(decimal(median("sparse", key, version, "cores"), 2));
                cells.add(integer(median("sparse", key, version, "notFired")));
                cells.add(decimal(median("sparse", key, version, "outOfOrder"), 2));
                out.append(row(cells));
            }
        }
    }

    // kept up with the rate in every round
    private boolean keptUp(String scenario, String key, String version) {
        List<Map<String, String>> runs = results.runs(scenario, key, version);
        return runs.size() == rounds && runs.stream().allMatch(run -> "true".equals(run.get("sustained")));
    }

    private String keptUpText(String scenario, String key, String version) {
        List<Map<String, String>> runs = results.runs(scenario, key, version);
        long kept = runs.stream().filter(run -> "true".equals(run.get("sustained"))).count();
        if (kept == runs.size() && runs.size() == rounds) {
            return "yes";
        }
        return kept == 0 ? "no" : kept + " of " + runs.size();
    }

    // the median of the rounds; a negative value is none
    private double median(String scenario, String key, String version, String name) {
        List<Double> values = new ArrayList<>();
        for (Map<String, String> run : results.runs(scenario, key, version)) {
            double value = number(run.get(name));
            if (value >= 0) {
                values.add(value);
            }
        }
        if (values.isEmpty()) {
            return Double.NaN;
        }
        Collections.sort(values);
        int middle = values.size() / 2;
        return values.size() % 2 == 1 ? values.get(middle) : (values.get(middle - 1) + values.get(middle)) / 2.0D;
    }

    private static double number(String text) {
        if (text == null || !text.matches("-?\\d+(\\.\\d+)?([eE]-?\\d+)?")) {
            return Double.NaN;
        }
        return Double.parseDouble(text);
    }

    private static String header(String first, List<String> titles) {
        List<String> cells = new ArrayList<>();
        cells.add(first);
        cells.addAll(titles);
        StringBuilder line = new StringBuilder(row(cells)).append('|');
        for (int i = 0; i < cells.size(); i++) {
            line.append("---|");
        }
        return line.append('\n').toString();
    }

    private static String row(List<String> cells) {
        return "| " + String.join(" | ", cells) + " |\n";
    }

    private static String integer(double value) {
        return Double.isNaN(value) ? NONE : String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    private static String decimal(double value, int digits) {
        return Double.isNaN(value) ? NONE : String.format(Locale.ROOT, "%." + digits + "f", value);
    }

    private static String millis(double value) {
        if (Double.isNaN(value)) {
            return NONE;
        }
        if (value < 1.0D) {
            return String.format(Locale.ROOT, "%.3f", value);
        }
        if (value < 10.0D) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return value < 100.0D ? String.format(Locale.ROOT, "%.1f", value) : integer(value);
    }

    private static String times(double ratio) {
        if (Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return NONE;
        }
        return String.format(Locale.ROOT, ratio >= 10.0D ? "×%.0f" : "×%.2f", ratio);
    }

}
