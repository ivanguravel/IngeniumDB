package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import org.ivzh.bzip.indexer.dto.Block;

import static org.ivzh.bzip.indexer.util.Const.*;

public class ExtendedBzipInputStream extends InputStream {
    private RandomAccessFile file;
    private byte[] fileBuffer;
    private long streamOffset;

    public ExtendedBzipInputStream(String filePath, String mode) {
        File helper = new File(filePath);
        try {
            this.file = new RandomAccessFile(helper, mode);
        } catch (FileNotFoundException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
        this.fileBuffer = null;
        this.streamOffset = 0;
    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    public List<Block> indexBzip() {
        int count = 0;
        List<Block> result = new LinkedList<>();
        Block block = readBlock(count);
        result.add(block);
        while (block.getBlockLength() != 0) {
            block = readBlock(++count);
            result.add(block);
        }
        return result;
    }

    protected Block readBlock(int blockNumber) {
        if (fileBuffer == null) {
            fileBuffer = new byte[COMPRESSED_BLOCK_SIZE];
        }
        long blockAddress = streamOffset;
        try {
            final int headerByteCount = read(fileBuffer, 0, HEADER_SIZE);
            streamOffset += headerByteCount;
            if (headerByteCount == 0) {
                // last block
                return new Block(blockNumber, blockAddress, 0, 0, 0);
            }
//            if (headerByteCount != HEADER_SIZE) {
//                throw  new UnsupportedOperationException("header block was broken");
//            }
            final int blockLength = getBlockSize(fileBuffer, BLOCK_LENGTH_OFFSET) + 1;

            final int remaining = blockLength - headerByteCount;
            final int dataByteCount = read(fileBuffer, HEADER_SIZE, remaining);
            streamOffset += dataByteCount;
            // TODO find decompressed size
            return new Block(blockNumber, streamOffset, blockLength, 0, dataByteCount);
        } catch (IOException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
    }

    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < length) {
            final int count = file.read(buffer, offset + n, length - n);
            if (count < 0) {
                if (n > 0) {
                    return n;
                } else {
                    return count;
                }
            }
            n += count;
        }
        return n;
    }

    private int getBlockSize(final byte[] buffer, final int offset) {
        return ((buffer[offset] & 0xFF) |
                ((buffer[offset+1] & 0xFF) << 8));
    }

}