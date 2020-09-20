package org.ivzh.bzip.indexer.stream;

import org.ivzh.bzip.indexer.index.DecompressedBlock;

import java.io.IOException;
import java.io.InputStream;

public class EnhancedGzipInputStream extends InputStream {

    private DecompressedBlock mCurrentBlock = null;
    private SeekableStream mFile = null;

    @Override
    public int read() throws IOException {
        return 0;
    }


    /**
     * Seek to the given position in the file.  Note that pos is a special virtual file pointer,
     * not an actual byte offset.
     *
     * @param pos virtual file pointer position
     * @throws IOException if stream is closed or not a file based stream
     */
    public void seek(final long pos) throws IOException {

        // Cannot seek on streams that are not file based
        if (mFile == null) {
            throw new UnsupportedOperationException("Can't seek null stream");
        }

        // Decode virtual file pointer
        // Upper 48 bits is the byte offset into the compressed stream of a
        // block.
        // Lower 16 bits is the byte offset into the uncompressed stream inside
        // the block.
        final long compressedOffset = getAddressOfIndex(pos);
        final int uncompressedOffset = getIndexOffset(pos);
        final int available;
        if (mCurrentBlock != null && mCurrentBlock.mBlockAddress == compressedOffset) {
            available = mCurrentBlock.mBlock.length;
        } else {
            prepareForSeek();
            mFile.seek(compressedOffset);
            mStreamOffset = compressedOffset;
            mCurrentBlock = nextBlock(getBufferForReuse(mCurrentBlock));
            mCurrentOffset = 0;
            available = available();
        }
        if (uncompressedOffset > available || (uncompressedOffset == available && !eof())) {
            throw new IOException(INVALID_FILE_PTR_MSG + pos + " for " + getSource());
        }
        mCurrentOffset = uncompressedOffset;
    }


    private static long getAddressOfIndex(final long virtualFilePointer) {
        return (virtualFilePointer >> 16) & 0xFFFFFFFFFFFFL;
    }

    public static int getIndexOffset(final long virtualFilePointer) {
        return (int) (virtualFilePointer & OFFSET_MASK);
    }
}
