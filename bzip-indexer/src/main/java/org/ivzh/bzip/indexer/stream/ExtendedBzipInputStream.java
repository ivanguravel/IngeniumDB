package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.SplittableCompressionCodec;
import org.ivzh.bzip.indexer.dto.Block;
import org.ivzh.bzip.indexer.dto.Huffman;
import org.ivzh.bzip.indexer.dto.StreamBlock;

import static org.ivzh.bzip.indexer.util.Const.*;

public class ExtendedBzipInputStream extends InputStream {


    private static final String HEADER = "BZ";
    private static final int HEADER_LEN = HEADER.length();
    private static final String SUB_HEADER = "h9";
    private static final int SUB_HEADER_LEN = SUB_HEADER.length();


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
        StreamBlock streamBlock = initialiseStream();
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

    public Block readBlock(int blockNumber) throws IOException {
//        byte[] headerBytes = new byte[HEADER_LEN];
//        int actualRead = this.file.read(headerBytes, 0, HEADER_LEN);
//        if (actualRead != -1) {
//
//            actualRead = this.file.read(headerBytes, 0,
//                    SUB_HEADER_LEN);
//            String header = new String(headerBytes, StandardCharsets.UTF_8);
//            System.out.println(header);
//        }



        return new Block();
    }

    /**
     * Reads the stream header and checks that the data appears to be a valid BZip2 stream
     * @throws IOException if the stream header is not valid
     */
    private StreamBlock initialiseStream() throws IOException {

        StreamBlock streamBlock = new StreamBlock();


        /* Read the stream header */
        try {
            int marker1 = readBits(16);
            int marker2 = readBits(8);
            int blockSize = (readBits(8) - '0');

            if (
                    ((marker1 != STREAM_START_MARKER_1))
                            || (marker2 != STREAM_START_MARKER_2)
                            || (blockSize < 1) || (blockSize > 9)) {
                throw new UnsupportedOperationException("Invalid BZip2 header");
            }

            streamBlock.setBlockLength(blockSize * 100000);
        } catch (IOException e) {
            // If the stream header was not valid, stop trying to read more data
            streamBlock.setStreamComplete(true);
            throw e;
        }

        return streamBlock;
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

//    private BufferedInputStream readStreamHeader() throws IOException {
//        // We are flexible enough to allow the compressed stream not to
//        // start with the header of BZ. So it works fine either we have
//        // the header or not.
//        if (super.in != null) {
//            bufferedIn.mark(HEADER_LEN);
//            byte[] headerBytes = new byte[HEADER_LEN];
//            int actualRead = bufferedIn.read(headerBytes, 0, HEADER_LEN);
//            if (actualRead != -1) {
//                String header = new String(headerBytes, StandardCharsets.UTF_8);
//                if (header.compareTo(HEADER) != 0) {
//                    bufferedIn.reset();
//                } else {
//                    this.isHeaderStripped = true;
//                    // In case of BYBLOCK mode, we also want to strip off
//                    // remaining two character of the header.
//                    if (this.readMode == SplittableCompressionCodec.READ_MODE.BYBLOCK) {
//                        actualRead = bufferedIn.read(headerBytes, 0,
//                                SUB_HEADER_LEN);
//                        if (actualRead != -1) {
//                            this.isSubHeaderStripped = true;
//                        }
//                    }
//                }
//            }
//        }
//
//        if (bufferedIn == null) {
//            throw new IOException("Failed to read bzip2 stream.");
//        }
//
//        return bufferedIn;
//
//    }


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