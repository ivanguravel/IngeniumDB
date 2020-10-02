package org.ivzh.bzip.indexer.dto;

public class StreamBlock {
    public int read(byte[] buffer, int bufOffset, int length) {
        byte bytesRead = 0;
        while (bytesRead < length) {
            byte c = this.readByte();
            if (c < 0) { // EOF
                return (bytesRead==0) ? -1 : bytesRead;
            }
            buffer[bufOffset++] = c;
            bytesRead++;
        }
        return bytesRead;
    }

    // mock 4 now
    public byte readByte() {
        return 0;
    }
}
