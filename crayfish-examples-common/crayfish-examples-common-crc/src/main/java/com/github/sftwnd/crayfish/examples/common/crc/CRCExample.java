package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
public class CRCExample {

    private static final int CHUNK_SIZE = 8 * 1024 * 1024;
    private static final int CHUNKS = 128;
    private static final int BUFFER_SIZE = CHUNK_SIZE * CHUNKS;
    private static ExecutorService executorService = Executors.newFixedThreadPool(12);
    private static final Random RANDOM = new Random();
    private static final CrcModel CRC_MODEL = CrcModel.CRC64_XZ;

    public static void main(String[] args) throws InterruptedException {
        byte[] buff = initializeBuffer(BUFFER_SIZE);
        initExecutorServices();
        try {
            Instant tick = Instant.now();
            CRC serialCrc = process_serial(buff);
            long serialTick = ChronoUnit.MILLIS.between(tick, Instant.now());
            logger.info("Serial result: {} [tick: {} msec = {} mb/sec]", serialCrc, serialTick, mbsec(BUFFER_SIZE, serialTick));
            tick = Instant.now();
            CRC parallelCrc = process_parallel(buff);
            long parallelTick = ChronoUnit.MILLIS.between(tick, Instant.now());
            logger.info("Parallel result: {} [tick: {} msec = {} mb/sec", parallelCrc, parallelTick, mbsec(BUFFER_SIZE, parallelTick));
            logger.info("Parallel process time = {}% of serial process time", Math.round(10000.0D/serialTick*parallelTick)/100);
            if (parallelCrc.equals(serialCrc)) {
                logger.info("CRCs are equals");
            } else {
                logger.warn("CRCs are different");
            }
        } finally {
            executorService.shutdown();
        }
    }

    private static double  mbsec(long size, long msec) {
        Double mb = 1.0D*size/1024/1024;
        Double secs = 1.0D*msec/1000;
        return Math.round(mb*100.0D/secs)/100;
    }

    static CRC process_serial(byte[] buff) {
        return CRC_MODEL.getCRC(buff);
    }

    @SneakyThrows
    static CRC process_parallel(byte[] buff) {
        Map<Integer, Chunk> map = new HashMap<>();
        IntStream.range(0, CHUNKS)
                .forEach(i -> {
                    constructChunk(map, buff, i*CHUNK_SIZE);
                });
        synchronized (map) {
            while(true) {
                map.wait(100);
                Optional<Chunk> chunk = Optional.ofNullable(map.get(0))
                        .filter(c -> c.getCrc().getLength() == BUFFER_SIZE);
                if (chunk.isPresent()) {
                    return chunk.get().getCrc();
                }
            }
        }
    }

    /**
     * Construct chunk from buff and sent it to map
     * @param map map
     * @param buff buff
     * @param offset offset
     */
    private static void constructChunk(@NonNull Map<Integer, Chunk> map, byte[] buff, int offset) {
        executorService.submit(() -> addChunk(map, new Chunk(offset, CRC_MODEL).update(buff, offset, CHUNK_SIZE)));
    }

    /**
     * Comine twho chunks in one
     * @param map map
     * @param chunk chunk
     * @param neighbout neighbour before or after the current one
     */
    private static void combineChunk(@NonNull Map<Integer, Chunk> map, @NonNull Chunk chunk, @NonNull Chunk neighbout) {
        addChunk(map, chunk.combine(neighbout));
    }

    /**
     * Add chunk to map
     * @param map map
     * @param chunk chunk
     */
    private static void addChunk(@NonNull Map<Integer, Chunk> map, @NonNull Chunk chunk) {
        Chunk neighbour = extractNeighbourChunk(map, chunk);
        if (neighbour == null) {
            registerChunk(map, chunk);
        } else {
            executorService.submit(() -> combineChunk(map, chunk, neighbour));
        }
    }

    /**
     * Register chunk in map
     * @param map Map
     * @param chunk Chunk
     */
    private static void registerChunk(@NonNull Map<Integer, Chunk> map, @NonNull Chunk chunk) {
        synchronized (map) {
            map.put(chunk.getStartOffset(), chunk);
            if (chunk.getEndOffset() > chunk.getStartOffset()) {
                map.put(chunk.getEndOffset(), chunk);
                map.notify();
            }
            logger.debug("registerChunk - {}", chunk);
        }
    }

    /**
     * Extract neighbour chunk if exists - just befor or just after the current
     * @param map Map
     * @param chunk chunk
     * @return
     */
    private static Chunk extractNeighbourChunk(@NonNull Map<Integer, Chunk> map, @NonNull Chunk chunk) {
        synchronized (map) {
            Chunk result = map.get(chunk.getStartOffset()-1);
            if (result == null && chunk.getEndOffset() > chunk.getStartOffset()) {
                result = map.get(chunk.getEndOffset()+1);
            }
            return result == null ? null : extractChunk(map, result);
        }
    }

    /**
     * Extract chunk from map
     * @param map Map
     * @param chunk chunk
     * @return extracted chunk if exists or null in the other case
     */
    private static Chunk extractChunk(@NonNull Map<Integer, Chunk> map, @NonNull Chunk chunk) {
        synchronized (map) {
            Chunk result1 = map.remove(chunk.getStartOffset());
            Chunk result2 = map.remove(chunk.getEndOffset());
            Chunk result = result1 != null ? result1 : result2;
            if (result != null) {
                logger.debug("extractChunk - {}", result);
            }
            return result;
        }
    }

    @SneakyThrows
    private static void initExecutorServices() {
        CountDownLatch cdl = new CountDownLatch(100);
        IntStream.range(0, 100).forEach(i -> cdl.countDown());
        cdl.await();
    }

    private static final byte[] initializeBuffer(int bytes) {
        return Stream.of(new byte[bytes]).peek(RANDOM::nextBytes).findFirst().get();
    }

    private static class Chunk {
        @Getter private int startOffset;
        @Getter private CRC crc;
        Chunk(int offset, CrcModel model) {
            this.startOffset = offset;
            this.crc = model.getCRC();
        }
        public int getEndOffset() {
            return getStartOffset() + crc.getLength() - 1;
        }
        public Chunk update(@NonNull byte[] buff, int offset, int length) {
            Optional.of(this)
                    .filter(chunk -> chunk.getCrc().getLength() == 0)
                    .map(Chunk::getCrc)
                    .orElseThrow(() -> new IllegalArgumentException("Chunk is already computet"))
                    .update(buff, offset, length);
            return this;
        }
        @SneakyThrows
        public Chunk combine(@NonNull Chunk chunk) {
            if (chunk.startOffset < this.startOffset) {
                return chunk.combine(this);
            } else if (chunk.startOffset != getEndOffset() + 1) {
                throw new IllegalArgumentException("Unable to combine chunk "+this+" with chunk: "+chunk);
            }
            this.crc.combine(chunk.crc);
            return this;
        }
        @Override
        public String toString() {
            return "Chunk[start: "+startOffset+", crc: "+crc+"]";
        }
    }

}
