package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import com.github.sftwnd.crayfish.common.crc.CrcParameters;
import com.github.sftwnd.crayfish.common.crc.StandardCrc;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;

/**
 * The algorithms of the RevEng catalogue: every one checked against its check value and residue, and the models
 * chosen by the names and the aliases of the catalogue.
 */
@Slf4j
public class CatalogueExample {

    public static void main(String[] args) {
        catalogue();
        names();
    }

    private static void catalogue() {
        byte[] data = CrcParameters.CHECK_INPUT.getBytes(StandardCharsets.US_ASCII);   // "123456789"
        Map<Integer, Integer> widths = new TreeMap<>();
        int names = 0;
        int residues = 0;
        for (StandardCrc standard : StandardCrc.values()) {
            CrcModel model = standard.model();
            CrcParameters parameters = standard.parameters();
            // the check value of the catalogue is the CRC of "123456789"
            check(model.value(data) == standard.check(), standard.crcName() + ": the check value");
            // the data followed by its CRC is a valid codeword
            byte[] codeword = model.codeword(data);
            check(model.isValid(codeword), standard.crcName() + ": the codeword");
            // for whole bytes and refin = refout the CRC of every valid codeword is the residue XOR-ed with xorout
            if (parameters.width() % 8 == 0 && parameters.refin() == parameters.refout()) {
                check(model.value(codeword) == (standard.residue() ^ parameters.xorout()), standard.crcName() + ": the residue");
                residues++;
            }
            widths.merge(parameters.width(), 1, Integer::sum);
            names += standard.names().size();
        }
        logger.info("{} algorithms with {} names and aliases, the check value of each verified, the residue of {}",
                StandardCrc.values().length, names, residues);
        logger.info("Algorithms by width: {}", widths);
    }

    private static void names() {
        byte[] data = "Hello, World!!!".getBytes(StandardCharsets.UTF_8);

        // an alias gives the model of the algorithm, and the CRCs keep the name they were chosen by
        CrcModel pkzip = CrcModel.of("PKZIP");
        logger.info("PKZIP is {}, aliases {}", pkzip.description().name(), pkzip.description().aliases());
        check("CRC-32/ISO-HDLC".equals(pkzip.description().name()), "PKZIP is an alias of CRC-32/ISO-HDLC");

        // the CRCs of the name and of an alias are equal: they are compared by the parameters
        CRC byAlias = pkzip.crc(data);
        CRC byName = CrcModel.of("CRC-32/ISO-HDLC").crc(data);
        logger.info("{}: 0x{}, {}: 0x{}, equal = {}", byAlias.name(), byAlias.hex(), byName.name(), byName.hex(),
                byAlias.equals(byName));
        check(byAlias.equals(byName) && byAlias.hashCode() == byName.hashCode(), "the CRCs of an alias and the name");

        // the case of the letters does not matter, the name is that of the catalogue
        CrcModel crc32c = CrcModel.of("crc-32c");
        logger.info("crc-32c is {} of {}", crc32c.name(), crc32c.description().name());
        check(crc32c == CrcModel.of("CRC-32C"), "a name in any case");

        // an unknown name
        logger.info("CRC-99/UNKNOWN: {}", CrcModel.find("CRC-99/UNKNOWN"));
        check(CrcModel.find("CRC-99/UNKNOWN").isEmpty(), "an unknown name");

        // the parameter line of the catalogue
        String line = CrcModel.of("ARC").description().toString();
        logger.info("ARC: {}", line);
        check(line.equals("width=16  poly=0x8005  init=0x0000  refin=true  refout=true  xorout=0x0000"
                + "  check=0xbb3d  residue=0x0000  name=\"CRC-16/ARC\""), "the parameter line of CRC-16/ARC");
    }

}
