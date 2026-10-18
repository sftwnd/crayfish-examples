package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CrcModel;
import com.github.sftwnd.crayfish.common.crc.CrcParameters;
import com.github.sftwnd.crayfish.common.crc.StandardCrc;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;

/**
 * A CRC algorithm that is not in the catalogue: the same model built with the builder, read from a parameter line and
 * given by the parameters; its name made of the parameters chooses it again. The parameters of an algorithm of the
 * catalogue give the model of the catalogue.
 */
@Slf4j
public class CustomCrcExample {

    public static void main(String[] args) {
        // the polynomial of CRC-16/XMODEM with another initial value
        CrcModel built = CrcModel.builder(16)
                .poly(0x1021)
                .reflected(false)
                .init(0x1234)
                .build();
        long check = built.value(CrcParameters.CHECK_INPUT.getBytes(StandardCharsets.US_ASCII));
        logger.info("Built:  {}", built.description());
        check(built.name().equals("CRC-16/P1021_I1234"), "the name made of the parameters");
        check(StandardCrc.find(built.parameters()).isEmpty(), "the parameters are not in the catalogue");

        // the builder verifies the check value given to it
        CrcModel verified = CrcModel.builder(16).poly(0x1021).reflected(false).init(0x1234).check(check).build();
        check(verified == built, "one model per parameters and name");
        try {
            CrcModel.builder(16).poly(0x1021).reflected(false).init(0x1234).check(check ^ 1).build();
            check(false, "a wrong check value is rejected");
        } catch (IllegalArgumentException ex) {
            logger.info("A wrong check value: {}", ex.getMessage());
        }
        // a forgotten parameter that has no neutral value
        try {
            CrcModel.builder(16).poly(0x1021).init(0x1234).build();
            check(false, "a model without refin and refout is rejected");
        } catch (IllegalStateException ex) {
            logger.info("No refin and refout: {}", ex.getMessage());
        }

        // the parameter line of the catalogue, also with the check value and the residue to verify
        CrcModel parsed = CrcModel.of(CrcParameters.parse(built.description().toString()));
        CrcModel line = CrcModel.of(CrcParameters.parse("width=16 poly=0x1021 init=0x1234 refin=false refout=false xorout=0x0000"));
        CrcModel given = CrcModel.of(16, 0x1021, 0x1234, false, false, 0);
        logger.info("Parsed: {}, from the line: {}, by the parameters: {}", parsed.name(), line.name(), given.name());
        check(parsed == built && line == built && given == built, "the same model by the parameters");

        // the name made of the parameters chooses the model again, in any case of its letters
        check(CrcModel.of(built.name()) == built, "the model by its name");
        check(CrcModel.of(built.name().toLowerCase()) == built, "the model by its name in lower case");

        // the parameters of an algorithm of the catalogue give its model, whatever way they come
        CrcModel catalogue = CrcModel.builder(16).poly(0x1021).reflected(false).init(0x1D0F).build();
        logger.info("init=0x1D0F: {}, aliases {}", catalogue.name(), catalogue.description().aliases());
        check(catalogue == CrcModel.of("CRC-16/SPI-FUJITSU"), "the model of the catalogue");
    }

}
