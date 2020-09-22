package org.ivzh.bzip.indexer.stream;

import java.io.*;

public class ExtendedBzipInputStream extends InputStream {


    long FILE_SIGNATURE = 0x3034464649445342L;
    int HEADER_SIZE = 32;

    private RandomAccessFile file;

    public ExtendedBzipInputStream(String filePath, String mode) {
        File helper = new File(filePath);
        try {
            this.file = new RandomAccessFile(helper, mode);
        } catch (FileNotFoundException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
    
    /*
        File format:
            0	8	"BSDIFF40"
            8	8	X
            16	8	Y
            24	8	sizeof(newfile)
            32	X	bzip2(control block)
            32+X	Y	bzip2(diff block)
            32+X+Y	???	bzip2(extra block)
        with control block a set of triples (x,y,z) meaning "add x bytes
        from oldfile to x bytes from the diff block; copy y bytes from the
        extra block; seek forwards in oldfile by z bytes".
    */
    public String readBlockHeaders() {
        byte[] header = null;
        try {
            header = readWindow(HEADER_SIZE);
        } catch (IOException e) {
            throw new UnsupportedOperationException(String.format("can't read header because of: %s", e.getMessage()));
        }

        long signature = readInt64(header, 0);
        if (signature != FILE_SIGNATURE) {
            throw new RuntimeException("Corrupt patch.");
        }

        long controlLength = readInt64(header, 8);
        long diffLength = readInt64(header, 16);
        long newSize = readInt64(header, 24);

        if (controlLength < 0 || diffLength < 0 || newSize < 0) {
            throw new RuntimeException("Corrupt patch.");
        }

        // TODO. read blocks
        return "";
    }

    private long readInt64(byte[] buf, int offset) {
        long value = buf[offset + 7] & 0x7F;

        for (int index = 6; index >= 0; index--) {
            value *= 256;
            value += buf[offset + index];
        }

        if ((buf[offset + 7] & 0x80) != 0)
            value = -value;

        return value;
    }

    private byte[] readWindow(int count) throws IOException {
        if (count < 0) {
            throw new IllegalArgumentException("count");
        }
        byte[] buffer = new byte[count];
        readWindow(buffer, 0, count);

        return buffer;
    }

    private void readWindow(byte[] buffer, int offset, int count) throws IOException {
        // check arguments

        if (buffer == null) {
            throw new IllegalArgumentException("buffer");
        }
        if (offset < 0 || offset > buffer.length) {
            throw new IllegalArgumentException("offset");
        }
        if (count < 0 || buffer.length - offset < count) {
            throw new IllegalArgumentException("count");
        }

        while (count > 0) {
            int bytesRead = this.file.read(buffer, offset, count);
            if (bytesRead == 0) {
                throw new RuntimeException("end of stream");
            }

            offset += bytesRead;
            count -= bytesRead;
        }
    }
}
