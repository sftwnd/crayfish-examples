# Performance of crayfish-alarms 1.0.4 and 1.0.5

`VersionsComparison` compares [crayfish-alarms](https://github.com/sftwnd/crayfish-alarms) 1.0.4 and 1.0.5 under the
same load. Every scenario runs in a JVM of its own for each version, with the jars of the version on its class path:
the build copies them to `target/alarms/<version>`, and the scenarios use the API that both versions share. The rounds
run in the order 1.0.4, 1.0.5, then 1.0.5, 1.0.4; the report shows the medians of the rounds of each version, and is
printed at the end and written to `target/alarms-performance.md`.

```shell
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-performance exec:java
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-performance exec:java -Dexec.args="service sparse"
mvn -pl ./crayfish-examples-alarms/crayfish-examples-alarms-performance exec:java -Dalarms.performance.rounds=4
```

The commands run from the root of the project after `mvn install`, on Java 11 or later; the JVMs of the scenarios run
on the Java of Maven. The arguments are the scenarios to run, all of them by default; all of them take about 10 minutes
with 2 rounds. The numbers depend on the machine and on what else runs on it: compare the versions on the same machine
in one run, and run more rounds when the rounds of a version differ.

## Scenarios

### cost

The costs of a time range in one thread, without waits. The range is a minute of 2030 and the extractions get their
moments, so the clock plays no part.

- `addElements`: 200,000 alarms at random moments of the minute, 100 per call, in chunks of 1 s and in one chunk of
  60 s; ns per alarm.
- An extraction that fires one alarm while 10,000 or 100,000 others wait in the range, in chunks of 1 s and in one
  chunk of 60 s; µs per extraction.
- Extractions every 10 ms of 100,000 alarms of 20 s in chunks of 1 s: the alarms fired per second of work.
- The calls of the expectation per alarm added and per extraction.

### timerange

A time range that one thread fills and extracts, as the process of the service does: the thread registers the alarms
in calls of `addElements` of 100, extracts the fired ones and parks until the next call or alarm. The rate is doubled
from 12,500 alarms per second until a version does not keep up with it.

### service

The same load of an `AlarmTimeRangeService`: four threads register the alarms in calls of `addElements` of 100, and
the process with the default `minimalWait` fires them. The rate is doubled from 6,250 alarms per second.

### sparse

50 alarms per second for 20 s, registered one by one, each due 10 to 100 ms after its registration, with the default
`minimalWait` and with zero. The default of 1.0.4 is 75 ms: a wait shorter than it is a busy loop. That of 1.0.5 is
none.

## The load

A step of `timerange` and `service` registers the alarms at its rate for 4 s. An alarm is due 0.5 to 1.5 s after its
registration, in a range with chunks of 1 s; the same seeds give the same alarms to both versions. An alarm is an
object with a number and a due moment, and its hash code is the number, so no two alarms of a step have the same one.
The range is made by `ITimeRangeFactory.create(duration, interval, completeTimeout, Alarm::due, null)`.

A version keeps up with a rate if it fires at least 95% of it, the 99th percentile of the delays is at most 100 ms,
and every alarm is fired within 10 s after the last one is due. The steps of a version stop at the first rate it does
not keep up with.

| Value | Meaning |
|---|---|
| Alarms/s fired | the alarms fired per second in the window in which they are due at the full rate |
| Delay | from the moment an alarm is due to the moment the consumer gets it: p50, p99, p99.9 and the maximum |
| Backlog | the most alarms due and not fired, in samples every 10 ms |
| Drain | from the moment the last alarm is due to the moment the last alarm is fired |
| CPU | of the thread that fires the alarms, in percent of a core |
| JVM | the CPU time of the JVM, in cores |
| B/alarm | the bytes that the thread of the alarms allocates per alarm |
| GC | the time of the garbage collectors |
| Not fired | the alarms not fired 10 s after the last one is due |
| Out of order | the alarms fired after an alarm due later |
| Registration p99 | the time from a call of `addElements` to the completion of its stage |

## A run

A run on an Intel Core i9-9980HK (8 cores, 16 threads) with macOS 26.6 and JDK 21.0.12, 2 rounds, while other work
ran on the machine: a load average of 4 at the start. The maxima and the 99th percentiles at the high rates differ
the most between the rounds.

### The highest rate kept up with

Alarms per second: the highest rate of the steps that a version kept up with in every round; ≥ if it kept up with the
highest one. A version keeps up with a rate if it fires at least 95% of it, the 99th percentile of the delays is at
most 100 ms and every alarm is fired; its steps stop at the first rate it does not keep up with.

| Scenario | 1.0.4 | 1.0.5 |
|---|---|---|
| A time range in one thread | 50,000 | 400,000 |
| AlarmTimeRangeService | 25,000 | 400,000 |

### The costs of a time range in one thread

A range of a minute with alarms at random moments, and extractions at given moments: no waits. The last column tells
how many times the value of 1.0.5 is better than that of 1.0.4.

| Measure | 1.0.4 | 1.0.5 | 1.0.5 vs 1.0.4 |
|---|---|---|---|
| addElements, chunks of 1 s: ns per alarm | 1,626 | 886 | ×1.84 |
| addElements, one chunk of 60 s: ns per alarm | 1,273 | 901 | ×1.41 |
| An extraction of 1 alarm, 10,000 waiting, chunks of 1 s: µs | 138 | 8.00 | ×17 |
| An extraction of 1 alarm, 100,000 waiting, chunks of 1 s: µs | 392 | 4.85 | ×81 |
| An extraction of 1 alarm, 10,000 waiting, one chunk of 60 s: µs | 4,880 | 2.67 | ×1831 |
| An extraction of 1 alarm, 100,000 waiting, one chunk of 60 s: µs | 65,114 | 2.67 | ×24433 |
| Extractions every 10 ms, chunks of 1 s: alarms per second of work | 31,405 | 4,068,483 | ×130 |
| Calls of the expectation per alarm added | 26 | 1 | ×26 |
| Calls of the expectation per extraction | 27,701 | 0 | — |

### A time range in one thread

One thread registers the alarms in calls of `addElements` of 100, extracts the fired ones and parks until the next
call or alarm, as a process does, without a service. The range has chunks of 1 s; an alarm is due 0.5 to 1.5 s after
its registration. Alarms/s fired: in the window in which the alarms are due at the full rate. Delay: from the moment
an alarm is due to the moment the consumer gets it. Backlog: the most alarms due and not fired. Drain: from the last
due moment to the last alarm fired. CPU: of the thread that fires the alarms, in percent of a core. JVM: the CPU time
of the JVM. B/alarm: the bytes that the thread of the alarms allocates per alarm. Not fired: 10 s after the last alarm
is due. Out of order: the alarms fired after an alarm due later.

| Alarms/s registered | Version | Kept up | Alarms/s fired | p50, ms | p99, ms | Max, ms | Backlog | Drain, ms | CPU, % | JVM, cores | B/alarm | GC, ms | Not fired | Out of order, % |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 12,500 | 1.0.4 | yes | 12,531 | 4.14 | 11.7 | 14.5 | 115 | 1.41 | 82.2 | 0.94 | 143,426 | 14 | 0 | 77.38 |
| 12,500 | 1.0.5 | yes | 12,511 | 0.026 | 0.217 | 8.37 | 21 | 2.01 | 9.5 | 0.13 | 558 | 0 | 0 | 0.00 |
| 25,000 | 1.0.4 | yes | 25,056 | 9.09 | 28.9 | 34.6 | 459 | 1.47 | 89.8 | 0.93 | 76,357 | 18 | 0 | 90.16 |
| 25,000 | 1.0.5 | yes | 24,994 | 0.015 | 0.170 | 3.78 | 1 | 1.44 | 12.6 | 0.19 | 544 | 3 | 0 | 0.00 |
| 50,000 | 1.0.4 | yes | 50,313 | 22.4 | 77.3 | 87.9 | 2,123 | 1.29 | 90.3 | 0.94 | 32,694 | 17 | 0 | 96.54 |
| 50,000 | 1.0.5 | yes | 49,974 | 0.010 | 0.192 | 14.1 | 38 | 0.298 | 19.4 | 0.21 | 492 | 0 | 0 | 0.00 |
| 100,000 | 1.0.4 | no | 100,693 | 69.6 | 203 | 226 | 12,269 | 0.160 | 92.8 | 1.12 | 13,856 | 89 | 0 | 98.78 |
| 100,000 | 1.0.5 | yes | 100,001 | 0.008 | 0.190 | 9.77 | 82 | 0.134 | 30.9 | 0.32 | 449 | 6 | 0 | 0.00 |
| 200,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — |
| 200,000 | 1.0.5 | yes | 200,029 | 0.009 | 12.8 | 32.9 | 3,302 | 0.153 | 52.8 | 0.54 | 377 | 0 | 0 | 0.00 |
| 400,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — |
| 400,000 | 1.0.5 | yes | 400,227 | 0.093 | 19.1 | 41.1 | 5,477 | 0.148 | 78.3 | 0.78 | 299 | 0 | 0 | 0.00 |
| 800,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — |
| 800,000 | 1.0.5 | no | 531,077 | 1,028 | 2,195 | 2,449 | 439,866 | 132 | 95.4 | 1.42 | 237 | 201 | 0 | 14.71 |

### AlarmTimeRangeService

Four threads register the alarms in calls of `addElements` of 100, the process of `AlarmTimeRangeService` with the
default minimalWait fires them. The range has chunks of 1 s; an alarm is due 0.5 to 1.5 s after its registration. The
columns are those of the table above. Registration p99: the time from a call of `addElements` to the completion of its
stage.

| Alarms/s registered | Version | Kept up | Alarms/s fired | p50, ms | p99, ms | Max, ms | Backlog | Drain, ms | CPU, % | JVM, cores | B/alarm | GC, ms | Not fired | Out of order, % | Registration p99, ms |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 6,250 | 1.0.4 | yes | 6,267 | 1.54 | 4.61 | 7.28 | 45 | 0.012 | 91.6 | 1.06 | 319,995 | 19 | 0 | 50.75 | 2.29 |
| 6,250 | 1.0.5 | yes | 6,266 | 0.606 | 1.34 | 3.95 | 16 | 1.61 | 3.1 | 0.09 | 289 | 0 | 0 | 0.00 | 1.01 |
| 12,500 | 1.0.4 | yes | 12,498 | 3.70 | 11.6 | 15.0 | 156 | 0.001 | 91.9 | 0.96 | 163,234 | 20 | 0 | 75.69 | 5.73 |
| 12,500 | 1.0.5 | yes | 12,487 | 0.600 | 1.28 | 12.9 | 35 | 0.724 | 4.2 | 0.08 | 243 | 0 | 0 | 0.00 | 0.470 |
| 25,000 | 1.0.4 | yes | 25,020 | 7.78 | 24.7 | 29.1 | 676 | 0.004 | 93.1 | 0.97 | 80,390 | 16 | 0 | 89.93 | 346 |
| 25,000 | 1.0.5 | yes | 24,996 | 0.583 | 1.30 | 8.14 | 206 | 1.39 | 6.3 | 0.10 | 224 | 0 | 0 | 0.00 | 0.372 |
| 50,000 | 1.0.4 | no | 48,583 | 13.4 | 502 | 578 | 8,490 | 0.003 | 93.6 | 0.98 | 38,237 | 20 | 0 | 95.13 | 1,077 |
| 50,000 | 1.0.5 | yes | 49,954 | 0.554 | 1.32 | 13.3 | 503 | 0.149 | 10.3 | 0.15 | 212 | 6 | 0 | 0.00 | 0.458 |
| 100,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — | — |
| 100,000 | 1.0.5 | yes | 99,964 | 0.494 | 1.31 | 9.97 | 687 | 0.597 | 16.4 | 0.23 | 205 | 8 | 0 | 0.00 | 0.407 |
| 200,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — | — |
| 200,000 | 1.0.5 | yes | 200,014 | 0.308 | 1.32 | 14.2 | 2,536 | 0.799 | 31.6 | 0.36 | 203 | 0 | 0 | 0.00 | 1.79 |
| 400,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — | — |
| 400,000 | 1.0.5 | yes | 399,908 | 0.165 | 1.39 | 1.96 | 630 | 0.675 | 66.2 | 0.74 | 202 | 0 | 0 | 0.00 | 1.24 |
| 800,000 | 1.0.4 | not run | — | — | — | — | — | — | — | — | — | — | — | — | — |
| 800,000 | 1.0.5 | no | 790,962 | 0.130 | 252 | 390 | 81,550 | 0.286 | 84.4 | 1.27 | 195 | 137 | 0 | 18.38 | 872 |

### Rare alarms of AlarmTimeRangeService

One thread registers 50 alarms per second one by one for 20 s, each due 10 to 100 ms later. The default minimalWait of
1.0.4 is 75 ms, a busy wait; that of 1.0.5 is none. CPU: of the thread of the process, in percent of a core.

| minimalWait | Version | p50, ms | p99, ms | p99.9, ms | Max, ms | Later than 50 ms | CPU, % | JVM, cores | Not fired | Out of order, % |
|---|---|---|---|---|---|---|---|---|---|---|
| the default | 1.0.4 | 0.036 | 0.133 | 4.56 | 4.56 | 0 | 96.4 | 0.98 | 0 | 0.00 |
| the default | 1.0.5 | 1.25 | 4.90 | 27.3 | 27.3 | 0 | 0.7 | 0.02 | 0 | 0.00 |
| 0 | 1.0.4 | 1.31 | 4.67 | 17.4 | 17.4 | 0 | 0.8 | 0.03 | 0 | 3.35 |
| 0 | 1.0.5 | 1.31 | 4.67 | 20.0 | 20.0 | 0 | 0.6 | 0.01 | 0 | 0.00 |

### What the numbers show

- The highest rate kept up with is 8 times higher in one thread, 400,000 alarms per second against 50,000, and 16
  times higher with the service, 400,000 against 25,000.
- An extraction of 1.0.4 sorts the whole chunk of the moment again: with 100,000 alarms waiting in one chunk of 60 s
  it takes 65 ms, that of 1.0.5 2.7 µs. 1.0.4 calls the expectation 26 times per alarm added and 27,701 times per
  extraction, 1.0.5 once per alarm.
- At the rates both versions keep up with, the 99th percentile of the delays of 1.0.4 grows with the rate: from 12 to
  77 ms in one thread and from 5 to 25 ms with the service. That of 1.0.5 stays at 0.2 ms in one thread and at 1.3 ms
  with the service.
- 1.0.4 takes 82 to 93% of a core at any rate: it sorts the chunk at every extraction, and the default minimalWait of
  the service is a busy wait. 1.0.5 takes 3 to 10% of a core up to 50,000 alarms per second with the service.
- At these rates 1.0.4 allocates 33 to 320 KB per alarm, 1.0.5 0.2 to 0.6 KB.
- 1.0.4 fires 51 to 99% of the alarms after an alarm due later. 1.0.5 fires them in the order of their moments as
  long as it keeps up. When it falls behind, alarms registered late come after the alarms already fired.
- The 99th percentile of the time of a registration, to the completion of its stage, is 1.1 s for 1.0.4 at 50,000
  alarms per second. For 1.0.5 it is at most 2 ms up to 400,000.
- With the busy wait of 1.0.4 the median delay of rare alarms is 0.04 ms, at the cost of a core. Without it, and in
  1.0.5, the median delay is 1.3 ms: the service waits with `Object.wait(timeout, nanos)`, which rounds the
  nanoseconds up to a millisecond, and the timer of the system adds to it.
