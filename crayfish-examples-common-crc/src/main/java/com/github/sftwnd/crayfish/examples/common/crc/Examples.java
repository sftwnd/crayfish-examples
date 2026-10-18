package com.github.sftwnd.crayfish.examples.common.crc;

/**
 * Helpers of the CRC examples: hexadecimal bytes and checks of the results.
 */
final class Examples {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Examples() {
    }

    /** The bytes as hexadecimal digits separated by spaces: {@code 01 03 00 0a}. */
    static String hex(byte[] data, int offset, int length) {
        StringBuilder result = new StringBuilder(length * 3);
        for (int i = offset; i < offset + length; i++) {
            if (i > offset) {
                result.append(' ');
            }
            result.append(DIGITS[data[i] >> 4 & 0xF]).append(DIGITS[data[i] & 0xF]);
        }
        return result.toString();
    }

    static String hex(byte[] data) {
        return hex(data, 0, data.length);
    }

    /** The bytes of hexadecimal digits, spaces ignored: {@code "01 03 00 0a"}. */
    static byte[] bytes(String hex) {
        String digits = hex.replace(" ", "");
        byte[] result = new byte[digits.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(digits.substring(2 * i, 2 * i + 2), 16);
        }
        return result;
    }

    /** Stops the example if a result is not the expected one. */
    static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException("Unexpected result: " + message);
        }
    }

}
