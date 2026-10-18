package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcChecksum;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.CheckedOutputStream;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;

/**
 * A journal file whose CRC is kept next to it: the CRC of the records is calculated while they are written, saved as
 * a value and a length, and resumed from them by a later run that appends to the file. On a start the file is checked
 * against the saved CRC, so a damaged file is found.
 */
@Slf4j
public class ResumeCrcExample {

    private static final CrcModel MODEL = CrcModel.of("CRC-64/XZ");

    public static void main(String[] args) throws IOException {
        Path directory = Files.createTempDirectory("crc-resume-");
        Path journal = directory.resolve("journal.log");
        Path state = directory.resolve("journal.crc");
        try {
            // the first run writes the journal from the start
            CrcChecksum checksum = MODEL.checksum();
            write(journal, checksum, 1, 1_000, StandardOpenOption.CREATE_NEW);
            CRC first = checksum.crc();
            save(state, first);

            // a later run knows only the saved value and length: it checks the file and appends to it
            CRC saved = load(state);
            check(crcOf(journal).equals(saved), "the journal is not damaged");
            CrcChecksum resumed = saved.checksum();
            write(journal, resumed, 1_001, 1_500, StandardOpenOption.APPEND);
            CRC whole = resumed.crc();
            save(state, whole);

            // the CRC of the whole file, read again
            check(crcOf(journal).equals(whole), "the resumed CRC is the CRC of the whole file");
            check(Files.size(journal) == whole.length(), "the length of the CRC is the size of the file");

            // the CRC of the appended records alone, joined with the CRC of the first run
            try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.READ)) {
                CRC appended = MODEL.crc(channel.position(first.length()));
                logger.info("Appended records: CRC 0x{} of {} bytes", appended.hex(), appended.length());
                check(first.combine(appended).equals(whole), "the CRC of both runs combined");
            }

            // a damaged byte is found on the next start
            try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.WRITE)) {
                channel.write(ByteBuffer.wrap(new byte[] { '#' }), 100);
            }
            CRC damaged = crcOf(journal);
            logger.info("Damaged journal:  CRC 0x{}, saved 0x{}: damaged = {}",
                    damaged.hex(), load(state).hex(), !damaged.equals(load(state)));
            check(!damaged.equals(load(state)), "the damaged journal is found");
        } finally {
            Files.deleteIfExists(journal);
            Files.deleteIfExists(state);
            Files.delete(directory);
        }
    }

    private static void write(Path journal, CrcChecksum checksum, int from, int to, StandardOpenOption option)
            throws IOException {
        try (OutputStream file = Files.newOutputStream(journal, option, StandardOpenOption.WRITE);
             CheckedOutputStream out = new CheckedOutputStream(file, checksum)) {
            for (int record = from; record <= to; record++) {
                out.write(String.format("record %05d: the state of the record %d%n", record, record)
                        .getBytes(StandardCharsets.UTF_8));
            }
        }
        logger.info("Records {}-{} written: CRC 0x{} of {} bytes",
                from, to, checksum.crc().hex(), checksum.length());
    }

    private static CRC crcOf(Path journal) throws IOException {
        try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.READ)) {
            return MODEL.crc(channel);
        }
    }

    /** The CRC as the line {@code <value> <length>}, the value in the hexadecimal digits of the model. */
    private static void save(Path state, CRC crc) throws IOException {
        Files.write(state, (crc.hex() + ' ' + crc.length()).getBytes(StandardCharsets.US_ASCII));
    }

    private static CRC load(Path state) throws IOException {
        String[] fields = new String(Files.readAllBytes(state), StandardCharsets.US_ASCII).split(" ");
        return MODEL.crc(Long.parseUnsignedLong(fields[0], 16), Long.parseLong(fields[1]));
    }

}
