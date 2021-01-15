package com.bzip2.block;

import com.bzip2.block.dto.BzipBlock;
import com.bzip2.block.event.Bzip2BlockListener;
import org.apache.commons.compress.compressors.bzip2.AbstractBZip2OutputStream;
import org.apache.commons.compress.utils.CountingOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Notifies about each written block
 */
public class Bzip2BlockOutputStream extends AbstractBZip2OutputStream {

    private final Bzip2BlockListener blockListener;
    private int previousRunLengthOffset;

    public Bzip2BlockOutputStream(OutputStream out, Bzip2BlockListener blockListener)
            throws IOException {
        super(out);
        this.blockListener = blockListener;
    }

    public Bzip2BlockOutputStream(OutputStream out, int blockSize, Bzip2BlockListener blockListener)
            throws IOException {
        super(out, blockSize);
        this.blockListener = blockListener;
    }

    @Override
    protected void onEndOfBlock() throws IOException {
        flushBsBuff();
        if (null != blockListener) {
            blockListener.blockWritten(getBlockInfo());
        }
        blockBytesWritten = 0;
    }

    private BzipBlock getBlockInfo() {
        long byteOffset = getInnerOutputStream().getBytesWritten();

        // workaround for adding one to size of last block
        int dataSize = isBlockNotFull() ? blockBytesWritten + 1 : blockBytesWritten;

        // case when have repeated byte at the end of block
        int runLengthOffset = getRunLength() - 1;
        if (runLengthOffset > 0) {
            dataSize -= runLengthOffset;
        }
        if (previousRunLengthOffset > 0) {
            dataSize += previousRunLengthOffset;
        }
        previousRunLengthOffset = runLengthOffset;

        if (bsLive > Byte.MAX_VALUE) {
            throw new IllegalStateException("bsLive value too large: " + bsLive);
        }
        return new BzipBlock(dataSize, byteOffset, (byte) bsLive, getBlockSize());
    }

    @Override
    protected OutputStream wrapOutputStream(OutputStream out) {
        return new CountingOutputStream(out);
    }

    @Override
    protected CountingOutputStream getInnerOutputStream() {
        return (CountingOutputStream) super.getInnerOutputStream();
    }

    /**
     * Needed to correctly count compressed block size. Copied from bsW method
     */
    private void flushBsBuff() throws IOException {
        final OutputStream outShadow = this.getInnerOutputStream();
        int bsLiveShadow = this.bsLive;
        int bsBuffShadow = this.bsBuff;

        while (bsLiveShadow >= 8) {
            outShadow.write(bsBuffShadow >> 24); // write 8-bit
            bsBuffShadow <<= 8;
            bsLiveShadow -= 8;
        }

        this.bsBuff = bsBuffShadow;
        this.bsLive = bsLiveShadow;
    }
}
