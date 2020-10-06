package org.ivzh.bzip.indexer.helper;

import java.io.IOException;
import java.io.InputStream;

public class BitReader {

    private static final int[] BITMASK = {0x00, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF};

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private InputStream inputStream;
    private int bitBuffer;
    private int bitCount;

    public BitReader (final InputStream inputStream) {

        this.inputStream = inputStream;

    }

    public int readBits (final int count) throws IOException {

        int bitBuffer = this.bitBuffer;
        int bitCount = this.bitCount;

        if (bitCount < count) {
            while (bitCount < count) {
                int byteRead = this.inputStream.read();

                if (byteRead < 0) {
                    throw new UnsupportedOperationException ("Insufficient data");
                }

                bitBuffer = (bitBuffer << 8) | byteRead;
                bitCount += 8;
            }

            this.bitBuffer = bitBuffer;
        }

        bitCount -= count;
        this.bitCount = bitCount;

        return (bitBuffer >>> bitCount) & ((1 << count) - 1);

    }

    public boolean readBoolean() throws IOException {

        int bitBuffer = this.bitBuffer;
        int bitCount = this.bitCount;

        if (bitCount > 0) {
            bitCount--;
        } else {
            int byteRead = this.inputStream.read();

            if (byteRead < 0) {
                throw new UnsupportedOperationException ("Insufficient data");
            }

            bitBuffer = (bitBuffer << 8) | byteRead;
            bitCount += 7;
            this.bitBuffer = bitBuffer;
        }

        this.bitCount = bitCount;
        return ((bitBuffer & (1 << bitCount))) != 0;

    }

    public int readUnary() throws IOException {

        int bitBuffer = this.bitBuffer;
        int bitCount = this.bitCount;
        int unaryCount = 0;

        for (;;) {
            if (bitCount > 0) {
                bitCount--;
            } else  {
                int byteRead = this.inputStream.read();

                if (byteRead < 0) {
                    throw new UnsupportedOperationException ("Insufficient data");
                }

                bitBuffer = (bitBuffer << 8) | byteRead;
                bitCount += 7;
            }

            if (((bitBuffer & (1 << bitCount))) == 0) {
                this.bitBuffer = bitBuffer;
                this.bitCount = bitCount;
                return unaryCount;
            }
            unaryCount++;
        }

    }


    /**
     * Reads 32 bits of input as an integer
     * @return The integer read
     * @throws IOException if 32 bits are not available in the input stream
     */
    public int readInteger() throws IOException {

        return (readBits (16) << 16) | (readBits (16));

    }


    public String pi() throws IOException {
        byte[] buf = new byte[6];
        for (int i = 0; i < buf.length; i++) {

            buf[i] = (byte) this.readBits(8);
        }
        return bufToHex(buf);
    }

    public static String bufToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
}
