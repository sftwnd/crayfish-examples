package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import java.util.zip.CRC32;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;

/**
 * The CRC of a large file calculated in parts by parallel threads: the CRCs of the parts are joined with
 * {@code combine} into the CRC of the whole file, the same as a sequential calculation and as {@link CRC32}.
 * <p>
 * Arguments: the size of the file in MiB (64 by default) and the number of the parts (the number of the processors
 * by default). The times are of one run without a warm-up, an illustration rather than a benchmark.
 */
@Slf4j
public class ParallelCrcExample {

    private static final int MIB = 1 << 20;

    public static void main(String[] args) throws IOException {
        long size = (args.length > 0 ? Long.parseLong(args[0]) : 64L) * MIB;
        int parts = args.length > 1 ? Integer.parseInt(args[1]) : Runtime.getRuntime().availableProcessors();
        Path file = Files.createTempFile("crc-parallel-", ".bin");
        try {
            write(file, size);
            logger.info("File of {} MiB in {} parts", size / MIB, parts);
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
                long jdk = crc32(channel);
                for (String name : new String[] { "CRC-32", "CRC-64/XZ" }) {
                    CrcModel model = CrcModel.of(name);
                    CRC sequential = sequential(model, channel);
                    CRC parallel = parallel(model, channel, size, parts);
                    check(parallel.equals(sequential), "the parallel CRC of " + name + " is the sequential one");
                    check(parallel.length() == size, "the CRC of " + name + " is of the whole file");
                }
                check(CrcModel.of("CRC-32").crc(channel.position(0)).value() == jdk, "CRC-32 is java.util.zip.CRC32");
                logger.info("java.util.zip.CRC32: 0x{}", Long.toHexString(jdk));
            }
        } finally {
            Files.delete(file);
        }
    }

    private static CRC sequential(CrcModel model, FileChannel channel) throws IOException {
        long start = System.nanoTime();
        CRC crc = model.crc(channel.position(0));    // the channel is read from its position to its end
        logger.info("{} sequential: 0x{} in {} ms", model.name(), crc.hex(), millis(start));
        return crc;
    }

    private static CRC parallel(CrcModel model, FileChannel channel, long size, int parts) {
        long start = System.nanoTime();
        long partSize = (size + parts - 1) / parts;
        // the CRCs of the parts are independent; reduce keeps their order, and combine joins two neighbours
        CRC crc = LongStream.range(0, parts).parallel()
                .mapToObj(part -> {
                    long position = part * partSize;
                    return crcOfPart(model, channel, position, Math.max(0, Math.min(partSize, size - position)));
                })
                .reduce(CRC::combine)
                .orElseThrow();
        logger.info("{} parallel:   0x{} in {} ms", model.name(), crc.hex(), millis(start));
        return crc;
    }

    private static CRC crcOfPart(CrcModel model, FileChannel channel, long position, long length) {
        CRC crc = model.crc();
        ByteBuffer buffer = ByteBuffer.allocate(MIB);
        try {
            for (long offset = position, end = position + length; offset < end; ) {
                buffer.limit((int) Math.min(buffer.capacity(), end - offset));
                int read = channel.read(buffer, offset);    // a read at a position: the threads share the channel
                if (read < 0) {
                    throw new EOFException("The end of the file at " + offset);
                }
                offset += read;
                buffer.flip();
                crc = crc.update(buffer);
                buffer.clear();
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return crc;
    }

    private static long crc32(FileChannel channel) throws IOException {
        CRC32 crc32 = new CRC32();
        ByteBuffer buffer = ByteBuffer.allocate(MIB);
        channel.position(0);
        while (channel.read(buffer) >= 0) {
            buffer.flip();
            crc32.update(buffer);
            buffer.clear();
        }
        return crc32.getValue();
    }

    private static void write(Path file, long size) throws IOException {
        Random random = new Random(size);
        byte[] block = new byte[MIB];
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            for (long written = 0; written < size; written += block.length) {
                random.nextBytes(block);
                channel.write(ByteBuffer.wrap(block, 0, (int) Math.min(block.length, size - written)));
            }
        }
    }

    private static long millis(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

}
