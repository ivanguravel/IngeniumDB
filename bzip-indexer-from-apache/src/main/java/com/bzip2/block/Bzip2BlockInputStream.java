package com.bzip2.block;


import com.bzip2.block.dto.BzipBlock;
import org.apache.commons.compress.compressors.bzip2.AbstractBZip2InputStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * Changes in AbstractBZip2InputStream:
 *
 * 1. Added checkCrc flag to skip stream CRC verification. CRC verification will fail if we skip some blocks
 * 2. Do not call initBlock and init in constructor. Allow to set block size manually.
 *      Allow user to skip bytes before manual block initialization
 * 3. Allow to call initBlock explicitly.
 */
public class Bzip2BlockInputStream extends AbstractBZip2InputStream {

    private final InputStream originalStream;

    public Bzip2BlockInputStream(InputStream in, int blockSize) throws IOException {
        super(in);
        blockSize100k = checkBlockSize(blockSize);
        checkCrc = false;
        originalStream = in;
    }

    private int checkBlockSize(int blockSize) {
        if (blockSize < 1) {
            throw new IllegalArgumentException("blockSize(" + blockSize + ") < 1");
        }
        if (blockSize > 9) {
            throw new IllegalArgumentException("blockSize(" + blockSize + ") > 9");
        }
        return blockSize;
    }

    public void skipToBlock(BzipBlock block) throws IOException {
        skipToBlock(block.getCompressedByteOffset(), block.getCompressedBitOffset());
    }

    public void skipToBlock(long byteOffset, byte bitOffset) throws IOException {
        long actuallySkipped = originalStream.skip(byteOffset);
        if (actuallySkipped != byteOffset) {
            throw new IllegalStateException("Expected to skip " + byteOffset
                    + " bytes, but actually skipped " + actuallySkipped);
        }
        skipBits(bitOffset);
        initBlock();
    }

    public void skipBits(byte count) throws IOException {
        if (count < 0) {
            throw new IllegalArgumentException("Negative bit count to skip");
        }
        if (0 == count) {
            return;
        }
        byte readBatchSize = Integer.SIZE;
        byte exceedsBatchSize = (byte) (count - readBatchSize);

        if (exceedsBatchSize > 0) {
            getBitInputStream().readBits(readBatchSize);
            skipBits(exceedsBatchSize);
        } else {
            getBitInputStream().readBits(count);
        }
    }
}
