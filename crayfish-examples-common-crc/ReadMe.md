# Common CRC examples

Examples of [crayfish-common-crc](https://github.com/sftwnd/crayfish-common-crc) 2.1.0. Every example checks its
results and stops with an exception if one of them is not the expected one. The commands run from the root of the
project after `mvn install`, on Java 11 or later.

## CrcModel

The CRC of a text with a model chosen by its name.

```shell
mvn -pl ./crayfish-examples-common-crc exec:java
```

## Modbus RTU frame

`ModbusFrameExample`: a Modbus RTU request built as a codeword, the PDU followed by its CRC-16/MODBUS low byte first
(`codeword`, `crc.bytes(..., ByteOrder.LITTLE_ENDIAN)`), checked on receipt with `isValid`; a damaged frame and a CRC in
the wrong byte order are rejected.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-modbus exec:java
```

## Parallel CRC of a large file

`ParallelCrcExample`: the CRCs of the parts of a file calculated in parallel threads and joined with `combine`, for
CRC-32 and CRC-64/XZ; the same CRC as a sequential calculation of the channel and as `java.util.zip.CRC32`. The
arguments are the size of the file in MiB and the number of the parts; the times are an illustration, not a benchmark.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-parallel exec:java -Dexec.args="256 8"
```

## Resume and append

`ResumeCrcExample`: a journal written through a `CheckedOutputStream` with a `CrcChecksum` and its CRC saved as a value
and a length; a later run checks the file, appends to it and resumes the CRC with `model.crc(value, length).checksum()`;
the CRC of the appended part combined with the first one, and a damaged byte found on the next start.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-resume exec:java
```

## Find an unknown CRC

`FindCrcExample`: the algorithm of 1-Wire ROM codes found from samples with `StandardCrc.matching` and `retainAll`: one
sample of an 8-bit CRC matches several algorithms, two samples leave CRC-8/MAXIM-DOW.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-find exec:java
```

## Catalogue and names

`CatalogueExample`: all 112 algorithms of the RevEng catalogue checked against their check value, as codewords, and
against their residue where it applies; names and aliases in any case of the letters; the CRCs of an alias and of the
name are equal; the parameter line of the catalogue.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-catalogue exec:java
```

## PNG chunks

`PngChunksExample`: every chunk of a PNG image checked with `isValid(..., ByteOrder.BIG_ENDIAN)`; in the byte order of
the model, low byte first for CRC-32, an undamaged chunk is not valid; the chunks built with `codeword`, the image read
by `ImageIO`, a damaged chunk found. The argument is the path of a PNG file to check instead of the image built.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-png exec:java
mvn -pl ./crayfish-examples-common-crc -P crc-png exec:java -Dexec.args="/path/to/image.png"
```

## A CRC of your own

`CustomCrcExample`: the same model of parameters outside the catalogue with `CrcModel.builder(width)`,
`CrcParameters.parse` and `CrcModel.of(parameters)`; the check value verified by the builder; the name made of the
parameters chooses the model again; the parameters of an algorithm of the catalogue give its model.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-custom exec:java
```

## Bits

`BitsExample`: CRC-15/CAN of a CAN frame, CRC-5/USB of a USB token and CRC-11/FLEXRAY of a FlexRay header with
`valueOfBits`; the receiver checks the bits followed by their CRC with one call.

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-bits exec:java
```

## The JDK classes and the lookup tables

`JdkCrcExample`: CRC-32 and CRC-32C by the lookup tables of the library and by `java.util.zip.CRC32` and `CRC32C`:
equal CRCs, combined across the ways, `isJdk()`, `withJdk`, `CrcDescription.setJdk`; the times are an illustration,
not a benchmark. The default way can be given by the system property:

```shell
mvn -pl ./crayfish-examples-common-crc -P crc-jdk exec:java
mvn -pl ./crayfish-examples-common-crc -P crc-jdk exec:java -Dcom.github.sftwnd.crayfish.common.crc.jdk=true
```
