package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import org.ivzh.bzip.indexer.dto.Block;
import org.ivzh.bzip.indexer.dto.Huffman;

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

    public List<Block> indexBzip() throws IOException {
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

    private int readBits (final int count) throws IOException {

        int bitBuffer = this.fileBuffer.length;
        long bitCount = this.streamOffset;

        if (bitCount < count) {
            while (bitCount < count) {
                int byteRead = this.file.read();

                if (byteRead < 0) {
                    throw new UnsupportedOperationException ();
                }

                bitBuffer = (bitBuffer << 8) | byteRead;
                bitCount += 8;
            }

        }

        bitCount -= count;
        this.streamOffset = bitCount;

        return (bitBuffer >>> bitCount) & ((1 << count) - 1);

    }


    private int readInteger() throws IOException {
        return (readBits (16) << 16) | (readBits (16));
    }

    private Huffman readHuffmanTables() throws IOException {

        final byte[] huffmanSymbolMap = new byte[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH];
        final byte[][] tableCodeLengths = new byte[HUFFMAN_MAXIMUM_ALPHABET_SIZE+1][HUFFMAN_MAXIMUM_ALPHABET_SIZE+1];

        int huffmanEndOfBlock = 0;

        /* Read Huffman symbol to output byte map */
        int huffmanUsedRanges = readBits (16);
        int huffmanSymbolCount = 0;

        for (int i = 0; i < 16; i++) {
            if ((huffmanUsedRanges & ((1 << 15) >>> i)) != 0) {
                for (int j = 0, k = i << 4; j < 16; j++, k++) {
                    if (readBoolean()) {
                        huffmanSymbolMap[huffmanSymbolCount++] = (byte)k;
                    }
                }
            }
        }
        int endOfBlockSymbol = huffmanSymbolCount + 1;

        /* Read total number of tables and selectors*/
        final int totalTables = readBits (3);
        final int totalSelectors = readBits (15);


        /* Read and decode MTFed Huffman selector list */
        final byte[] selectors = new byte[totalSelectors];
        for (int selector = 0; selector < totalSelectors; selector++) {
            selectors[selector] = (byte) readUnary();
        }

        /* Read the Canonical Huffman code lengths for each table */
        for (int table = 0; table < totalTables; table++) {
            int currentLength = readBits (5);
            for (int i = 0; i <= endOfBlockSymbol; i++) {
                while (readBoolean()) {
                    currentLength += readBoolean() ? -1 : 1;
                }
                tableCodeLengths[table][i] = (byte)currentLength;
            }
        }

        return new Huffman ( endOfBlockSymbol + 1, tableCodeLengths, selectors, endOfBlockSymbol);

    }

    public boolean readBoolean() throws IOException {

        int bitBuffer = this.fileBuffer.length;
        long bitCount = this.streamOffset;

        if (bitCount > 0) {
            bitCount--;
        } else {
            int byteRead = this.file.read();

            if (byteRead < 0) {
                throw new UnsupportedOperationException ();
            }

            bitBuffer = (bitBuffer << 8) | byteRead;
            bitCount += 7;
        }

        this.streamOffset = bitCount;
        return ((bitBuffer & (1 << bitCount))) != 0;

    }


    public int readUnary() throws IOException {

        int bitBuffer = this.fileBuffer.length;
        long bitCount = this.streamOffset;
        int unaryCount = 0;

        for (;;) {
            if (bitCount > 0) {
                bitCount--;
            } else  {
                int byteRead = this.file.read();

                if (byteRead < 0) {
                    throw new UnsupportedOperationException ("Insufficient data");
                }

                bitBuffer = (bitBuffer << 8) | byteRead;
                bitCount += 7;
            }

            if (((bitBuffer & (1 << bitCount))) == 0) {
                this.streamOffset = bitCount;
                return unaryCount;
            }
            unaryCount++;
        }

    }

}