package com.github.sftwnd.crayfish.examples.alarms.performance;

/**
 * A histogram of delays in microseconds: one bucket per microsecond up to 2 ms, then 64 buckets per power of two, so
 * that a percentile is at most 1.6% above the delay. Not thread-safe.
 */
final class Delays {

    private static final int EXACT = 2048;
    private static final int MIN_EXPONENT = 11;
    private static final int SUB_BITS = 6;
    private static final int SUB_MASK = (1 << SUB_BITS) - 1;

    private final long[] counts = new long[EXACT + ((63 - MIN_EXPONENT) << SUB_BITS)];
    private long count;
    private long sum;
    private long max;

    void record(long micros) {
        long value = Math.max(0L, micros);
        counts[index(value)]++;
        count++;
        sum += value;
        max = Math.max(max, value);
    }

    long count() {
        return count;
    }

    long max() {
        return max;
    }

    double mean() {
        return count == 0 ? 0.0D : (double) sum / count;
    }

    /** The delay that the given percent of the delays do not exceed. */
    long percentile(double percent) {
        if (count == 0) {
            return 0L;
        }
        long rank = Math.max(1L, (long) Math.ceil(percent / 100.0D * count));
        long seen = 0L;
        for (int i = 0; i < counts.length; i++) {
            seen += counts[i];
            if (seen >= rank) {
                return Math.min(max, upper(i));
            }
        }
        return max;
    }

    /** The number of the delays longer than the given one, with the error of a bucket. */
    long above(long micros) {
        long result = 0L;
        for (int i = index(micros) + 1; i < counts.length; i++) {
            result += counts[i];
        }
        return result;
    }

    private static int index(long value) {
        if (value < EXACT) {
            return (int) value;
        }
        int exponent = 63 - Long.numberOfLeadingZeros(value);
        int sub = (int) (value >>> (exponent - SUB_BITS)) & SUB_MASK;
        return EXACT + ((exponent - MIN_EXPONENT) << SUB_BITS) + sub;
    }

    private static long upper(int index) {
        if (index < EXACT) {
            return index;
        }
        int exponent = MIN_EXPONENT + ((index - EXACT) >>> SUB_BITS);
        int sub = (index - EXACT) & SUB_MASK;
        return (1L << exponent) + ((long) (sub + 1) << (exponent - SUB_BITS)) - 1L;
    }

}
