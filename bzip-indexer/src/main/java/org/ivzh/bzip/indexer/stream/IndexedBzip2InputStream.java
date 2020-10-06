package org.ivzh.bzip.indexer.stream;


import org.ivzh.bzip.indexer.dto.Block;
import org.ivzh.bzip.indexer.helper.BitReader;
import org.ivzh.bzip.indexer.helper.BlockDecompressor;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import static org.ivzh.bzip.indexer.util.Const.*;

public class IndexedBzip2InputStream extends InputStream {


    private InputStream inputStream;

    private BitReader bitReader;


    private final boolean headerless;

    private boolean streamComplete = false;

    private int streamBlockSize;

    private int streamCRC = 0;

    private BlockDecompressor blockDecompressor = null;


    private Block currentBlock;

    private LinkedList<Block> index = new LinkedList<Block>();


    /* (non-Javadoc)
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {

        int nextByte = -1;
        if (this.blockDecompressor == null) {
            initialiseStream();
        } else {
            nextByte = this.blockDecompressor.read();
        }

        if (nextByte == -1) {
            if (initialiseNextBlock()) {
                nextByte = this.blockDecompressor.read();
            }
        }

        return nextByte;

    }


    /* (non-Javadoc)
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read (final byte[] destination, final int offset, final int length) throws IOException {

        int bytesRead = -1;
        if (this.blockDecompressor == null) {
            initialiseStream();
        } else {
            bytesRead = this.blockDecompressor.read (destination, offset, length);
        }

        if (bytesRead == -1) {
            if (initialiseNextBlock()) {
                bytesRead = this.blockDecompressor.read (destination, offset, length);
                this.currentBlock.setBlockLength(bytesRead);
                this.index.add(this.currentBlock);
            }
        }

        return bytesRead;

    }


    /* (non-Javadoc)
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {

        if (this.bitReader != null) {
            this.streamComplete = true;
            this.blockDecompressor = null;
            this.bitReader = null;

            try {
                this.inputStream.close();
            } finally {
                this.inputStream = null;
            }
        }

    }

    private void initialiseStream() throws IOException {

        if (this.bitReader == null) {
            throw new UnsupportedOperationException ("Stream closed");
        }

        if (this.streamComplete) {
            return;
        }

        try {
            int marker1 = this.headerless ? 0 : this.bitReader.readBits (16);
            int marker2 = this.bitReader.readBits (8);
            int blockSize = (this.bitReader.readBits (8) - '0');

            if (
                    (!this.headerless && (marker1 != STREAM_START_MARKER_1))
                            || (marker2 != STREAM_START_MARKER_2)
                            || (blockSize < 1) || (blockSize > 9))
            {
                throw new UnsupportedOperationException ("Invalid BZip2 header");
            }

            this.streamBlockSize = blockSize * 100000;
        } catch (IOException e) {
            this.streamComplete = true;
            throw e;
        }


    }

    private boolean initialiseNextBlock() throws IOException {

        if (this.streamComplete) {
            return false;
        }

        if (this.blockDecompressor != null) {
            int blockCRC = this.blockDecompressor.checkCRC();
            this.streamCRC = ((this.streamCRC << 1) | (this.streamCRC >>> 31)) ^ blockCRC;
        }

        final int marker1 = this.bitReader.readBits (24);
        final int marker2 = this.bitReader.readBits (24);

        if (marker1 == BLOCK_HEADER_MARKER_1 && marker2 == BLOCK_HEADER_MARKER_2) {
            try {
                this.currentBlock = new Block();
                this.currentBlock.setBlockOffset(marker1 / 100000);

                if (index.isEmpty()) {
                    this.currentBlock.setBlockNum(0);
                } else {
                    this.currentBlock.setBlockNum(this.index.getLast().getBlockNum() + 1);
                }

                this.blockDecompressor = new BlockDecompressor (this.bitReader, this.streamBlockSize);
            } catch (IOException e) {
                this.streamComplete = true;
                throw e;
            }
            return true;
        } else if (marker1 == STREAM_END_MARKER_1 && marker2 == STREAM_END_MARKER_2) {
            this.streamComplete = true;
            final int storedCombinedCRC = this.bitReader.readInteger();
            if (storedCombinedCRC != this.streamCRC) {
                throw new UnsupportedOperationException ("BZip2 stream CRC error");
            }
            return false;
        }

        this.streamComplete = true;
        throw new UnsupportedOperationException ("BZip2 stream format error");

    }

    // return index which is created during looping over blocks
    public List<Block> getIndex() {
        return index;
    }

    public IndexedBzip2InputStream(final InputStream inputStream, final boolean headerless) {

        if (inputStream == null) {
            throw new IllegalArgumentException ("Null input stream");
        }

        this.inputStream = inputStream;
        this.bitReader = new BitReader(inputStream);
        this.headerless = headerless;

    }

}
