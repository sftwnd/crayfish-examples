package com.github.sftwnd.crayfish.examples.alarms.performance;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Compares the performance of versions of crayfish-alarms, 1.0.4 and 1.0.5 by default. Every scenario of
 * {@link Scenarios} runs in a JVM of its own for each version, with the jars of the version on its class path, in
 * rounds of the order A B, B A, A B...; the report shows the medians of the rounds of each version, and is written
 * to target/alarms-performance.md. The arguments are the scenarios: cost, timerange, service and sparse, all of them
 * by default. {@code -Dalarms.performance.rounds} sets the number of the rounds, 2 by default.
 */
@Slf4j
public class VersionsComparison {

    static final List<String> SCENARIOS = Collections.unmodifiableList(
            Arrays.asList("cost", "timerange", "service", "sparse"));
    static final List<String> JVM_OPTIONS = Collections.unmodifiableList(Arrays.asList("-Xms2g", "-Xmx2g"));
    private static final String RESULT = "RESULT ";

    public static void main(String[] args) throws IOException, InterruptedException {
        Path classes = Paths.get(System.getProperty("alarms.performance.classes", "target/classes"));
        Path directory = Paths.get(System.getProperty("alarms.performance.directory", "target/alarms"));
        Path report = Paths.get(System.getProperty("alarms.performance.report", "target/alarms-performance.md"));
        List<String> versions = Arrays.asList(System.getProperty("alarms.performance.versions", "1.0.4,1.0.5").split(","));
        int rounds = Integer.parseInt(System.getProperty("alarms.performance.rounds", "2"));
        List<String> scenarios = args.length == 0 ? SCENARIOS : Arrays.asList(args);
        for (String scenario : scenarios) {
            if (!SCENARIOS.contains(scenario)) {
                throw new IllegalArgumentException("Unknown scenario " + scenario + ", the scenarios are " + SCENARIOS);
            }
        }
        Map<String, String> classpaths = new LinkedHashMap<>();
        for (String version : versions) {
            classpaths.put(version, classpath(classes, directory.resolve(version)));
        }
        String environment = environment(rounds);
        logger.info("Versions {}, scenarios {}\n{}", versions, scenarios, environment);
        Results results = new Results();
        for (String scenario : scenarios) {
            for (int round = 0; round < rounds; round++) {
                List<String> order = new ArrayList<>(versions);
                if (round % 2 == 1) {
                    Collections.reverse(order);
                }
                for (String version : order) {
                    run(scenario, version, classpaths.get(version), results);
                }
            }
        }
        String markdown = new Report(versions, rounds, results).markdown(environment);
        Files.write(report, markdown.getBytes(StandardCharsets.UTF_8));
        logger.info("The report is written to {}\n{}", report, markdown);
    }

    private static void run(String scenario, String version, String classpath, Results results)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(java());
        command.addAll(JVM_OPTIONS);
        command.addAll(Arrays.asList("-cp", classpath, Scenarios.class.getName(), scenario));
        logger.info("{} {}: started", version, scenario);
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.startsWith(RESULT)) {
                    Map<String, String> values = parse(line.substring(RESULT.length()));
                    results.add(scenario, values.get("case"), version, values);
                    logger.info("{} {} {}: {}", version, scenario, values.get("case"), Report.summary(values));
                } else {
                    logger.info("{} {}: {}", version, scenario, line);
                }
            }
        }
        if (!process.waitFor(1, TimeUnit.MINUTES)) {
            process.destroyForcibly();
            logger.warn("{} {}: the JVM did not exit and is destroyed", version, scenario);
        } else if (process.exitValue() != 0) {
            logger.warn("{} {}: the JVM exited with {}", version, scenario, process.exitValue());
        }
    }

    private static Map<String, String> parse(String line) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : line.trim().split(" +")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                values.put(pair.substring(0, equals), pair.substring(equals + 1));
            }
        }
        return values;
    }

    // the classes of the example and the jars of the version
    private static String classpath(Path classes, Path version) {
        File[] jars = version.toFile().listFiles((directory, name) -> name.endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            throw new IllegalStateException("No jars in " + version + ": build the module with mvn install first");
        }
        Arrays.sort(jars);
        StringBuilder classpath = new StringBuilder(classes.toString());
        for (File jar : jars) {
            classpath.append(File.pathSeparatorChar).append(jar.getPath());
        }
        return classpath.toString();
    }

    private static String java() {
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
        return Paths.get(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java").toString();
    }

    private static String environment(int rounds) {
        double load = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return String.format(Locale.ROOT,
                "- Java %s, %s (%s)%n"
                        + "- %s %s %s, %d CPUs, load average %s at the start%n"
                        + "- every scenario and version in a JVM of its own with %s; %d rounds in the order A B, B A%n"
                        + "- %s%n",
                System.getProperty("java.version"), System.getProperty("java.vm.name"), System.getProperty("java.vendor"),
                System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(), load < 0 ? "unknown" : String.format(Locale.ROOT, "%.2f", load),
                String.join(" ", JVM_OPTIONS), rounds, Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

}
