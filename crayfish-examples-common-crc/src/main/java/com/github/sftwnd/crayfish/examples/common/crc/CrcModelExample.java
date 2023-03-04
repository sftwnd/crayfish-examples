package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class CrcModelExample {

    public static void main(String[] args) {
        AtomicInteger crcId = new AtomicInteger();
        CrcModel[] crcModels = new CrcModel[] { CrcModel.CRC32_XZ, CrcModel.CRC7_UMTS, CrcModel.CRC64_XZ };
        Stream.of("Hello, World!!!", "Welcome, guys :)", "I'll be back...")
                .forEach(text -> logger.info(
                        "CRC[{}]\tfor: {}\tis: 0x{}",
                        crcModels[crcId.get()],
                        text,
                        crc(crcModels[crcId.getAndIncrement()], text) )
                );
    }

    private static String crc(final @NonNull CrcModel model, final @NonNull String text) {
        CRC crc = model.getCRC();
        crc.update(text.getBytes());
        return "0x" + Long.toHexString(crc.getCrc());
    }

}