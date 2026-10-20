package com.github.sftwnd.crayfish.examples.alarms.performance;

import java.time.Instant;
import java.util.concurrent.locks.LockSupport;

/**
 * An alarm of the load: its number and the moment it is due. Alarms with the same number are equal, and the hash
 * code is the number, so that no two alarms of a step have the same hash code.
 */
final class Alarm {

    final int id;
    final long dueMicros;
    final Instant due;

    Alarm(int id, long dueMicros) {
        this.id = id;
        this.dueMicros = dueMicros;
        this.due = instant(dueMicros);
    }

    Instant due() {
        return due;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Alarm && ((Alarm) other).id == this.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    /** The current moment in microseconds of the epoch: the delays are measured in microseconds. */
    static long now() {
        return micros(Instant.now());
    }

    static long micros(Instant instant) {
        return instant.getEpochSecond() * 1_000_000L + instant.getNano() / 1_000L;
    }

    static Instant instant(long micros) {
        return Instant.ofEpochSecond(Math.floorDiv(micros, 1_000_000L), Math.floorMod(micros, 1_000_000L) * 1_000L);
    }

    static void parkUntil(long micros) {
        for (long wait = micros - now(); wait > 0; wait = micros - now()) {
            LockSupport.parkNanos(wait * 1_000L);
        }
    }

}
