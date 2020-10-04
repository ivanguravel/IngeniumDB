package org.ivzh.bzip.indexer.dto;

import java.io.IOException;
import java.io.RandomAccessFile;

public class StreamBlock {
    public RandomAccessFile randomAccessFile;
    public int pos = 0;
    public int read(byte[] buffer, int bufOffset, int length) throws IOException {
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
    public byte readByte() throws IOException {
        return 0;
    }
}
