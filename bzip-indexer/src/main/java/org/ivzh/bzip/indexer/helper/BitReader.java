package org.ivzh.bzip.indexer.helper;

import java.io.IOException;
import java.io.RandomAccessFile;

public class BitReader {

    private static final int[] BITMASK = {0x00, 0x01, 0x03, 0x07, 0x0F, 0x1F, 0x3F, 0x7F, 0xFF};

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

     private final RandomAccessFile raf;

    public int bitOffset;
     private byte curByte;
     public boolean hasByte;

    public BitReader(RandomAccessFile raf) {
        this.raf = raf;

        this.bitOffset = 0;
        this.curByte = 0;
        this.hasByte = false;
    }

    public void ensureByte() throws IOException {
        if (!this.hasByte) {
            this.curByte = this.raf.readByte();
            this.hasByte = true;
        }
    }

    // reads bits from the buffer
    public byte read(int bits) throws IOException {
        byte result = 0;
        while (bits > 0) {
            this.ensureByte();
            int remaining = 8 - this.bitOffset;
            // if we're in a byte
            if (bits >= remaining) {
                result <<= remaining;
                result |= BITMASK[remaining] & this.curByte;
                this.hasByte = false;
                this.bitOffset = 0;
                bits -= remaining;
            } else {
                result <<= bits;
                int shift = remaining - bits;
                result |= (this.curByte & (BITMASK[bits] << shift)) >> shift;
                this.bitOffset += bits;
                bits = 0;
            }
        }
        return result;
    }

    // seek to an arbitrary point in the buffer (expressed in bits)
    public void seek(int pos) throws IOException {
        int n_bit = pos % 8;
        int n_byte = (pos - n_bit) / 8;
        this.bitOffset = n_bit;
        this.raf.seek(n_byte);
        this.hasByte = false;
    }

    public String pi() throws IOException {
        byte[] buf = new byte[6];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = this.read(8);
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
