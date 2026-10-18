package com.github.sftwnd.crayfish.examples.common.crc;

import com.github.sftwnd.crayfish.common.crc.CRC;
import com.github.sftwnd.crayfish.common.crc.CrcModel;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.github.sftwnd.crayfish.examples.common.crc.Examples.bytes;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.check;
import static com.github.sftwnd.crayfish.examples.common.crc.Examples.hex;

/**
 * A Modbus RTU frame: the PDU with the address of the device followed by its CRC-16/MODBUS, low byte first.
 * The frame is built as a codeword, checked on receipt, and a damaged frame is rejected.
 */
@Slf4j
public class ModbusFrameExample {

    private static final CrcModel MODBUS = CrcModel.of("CRC-16/MODBUS");

    public static void main(String[] args) {
        // Read Holding Registers: device 1, function 3, the registers from 0, 10 registers
        byte[] request = bytes("01 03 00 00 00 0a");
        logger.info("Request:          {}", hex(request));

        // the request followed by its CRC in the byte order of the model: CRC-16/MODBUS is refout, low byte first
        byte[] frame = MODBUS.codeword(request);
        logger.info("Frame:            {}", hex(frame));
        check(Arrays.equals(frame, bytes("01 03 00 00 00 0a c5 cd")), "the frame of the Modbus specification");

        // the same bytes of the CRC, written by the sender into a buffer of its own
        CRC crc = MODBUS.crc(request);
        byte[] buffer = new byte[256];
        System.arraycopy(request, 0, buffer, 0, request.length);
        int length = request.length + crc.bytes(buffer, request.length, ByteOrder.LITTLE_ENDIAN);
        logger.info("CRC:              0x{}, sent as {}", crc.hex(), hex(buffer, request.length, 2));
        check(Arrays.equals(Arrays.copyOf(buffer, length), frame), "the frame built in the buffer");

        // the receiver checks the frame with its CRC in one call
        boolean valid = MODBUS.isValid(frame);
        logger.info("Received frame:   valid = {}", valid);
        check(valid, "the frame is valid");

        // a frame received into a ByteBuffer, for example from a channel
        boolean received = MODBUS.isValid(ByteBuffer.wrap(buffer, 0, length));
        logger.info("Received buffer:  valid = {}", received);
        check(received, "the frame of the buffer is valid");

        // one bit damaged on the line
        byte[] damaged = frame.clone();
        damaged[4] ^= 0x01;
        logger.info("Damaged frame:    {}, valid = {}", hex(damaged), MODBUS.isValid(damaged));
        check(!MODBUS.isValid(damaged), "the damaged frame is rejected");

        // the CRC in the wrong byte order is rejected too
        byte[] swapped = MODBUS.codeword(request, ByteOrder.BIG_ENDIAN);
        logger.info("High byte first:  {}, valid = {}", hex(swapped), MODBUS.isValid(swapped));
        check(!MODBUS.isValid(swapped), "the CRC high byte first is rejected");
    }

}
