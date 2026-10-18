package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class CrcModelExample {

    public static void main(String[] args) {
        AtomicInteger crcId = new AtomicInteger();
        CrcModel[] crcModels = new CrcModel[] { CrcModel.of("CRC-32/XZ"), CrcModel.of("CRC-7/UMTS"), CrcModel.of("CRC-64/XZ") };
        Stream.of("Hello, World!!!", "Welcome, guys :)", "I'll be back...")
                .forEach(text -> logger.info(
                        "CRC[{}]\tfor: {}\tis: 0x{}",
                        crcModels[crcId.get()].name(),
                        text,
                        crcModels[crcId.getAndIncrement()].crc(text.getBytes(StandardCharsets.UTF_8)).hex())
                );
    }

}
