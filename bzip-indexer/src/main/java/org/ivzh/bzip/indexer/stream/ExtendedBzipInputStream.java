package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import org.ivzh.bzip.indexer.dto.Block;
import org.ivzh.bzip.indexer.dto.CRC32;
import org.ivzh.bzip.indexer.dto.Huffman;
import org.ivzh.bzip.indexer.dto.StreamBlock;
import org.ivzh.bzip.indexer.helper.BitReader;

import static org.ivzh.bzip.indexer.util.Const.*;

public class ExtendedBzipInputStream extends InputStream {


    private static final String WHOLEPI = "314159265359";
    private static final String SQRTPI = "177245385090";



    private static final String HEADER = "BZ";
    private static final int HEADER_LEN = HEADER.length();
    private static final String SUB_HEADER = "h9";
    private static final int SUB_HEADER_LEN = SUB_HEADER.length();


    private RandomAccessFile file;
    private BitReader reader;
    private CRC32 crc32;
    private StreamBlock streamBlock;

     int dbufSize;
     int nextoutput;
     int streamCRC;

     int targetBlockCRC;




    public ExtendedBzipInputStream(String filePath, String mode) {
        File helper = new File(filePath);
        try {
            this.file = new RandomAccessFile(helper, mode);
        } catch (FileNotFoundException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
        this.reader = new BitReader(file);
        this.crc32 = new CRC32();
        this.streamBlock = new StreamBlock();
        try {
            startBunzip();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    public List<Block> indexBzip() throws IOException {
        StreamBlock streamBlock = initialiseStream();
        int count = 0;
        List<Block> result = new LinkedList<>();

//        inputStream.delegate = coerceInputStream(input);
//        inputStream.pos = 0;
//        inputStream.readByte = function() {
//            this.pos++;
//            return this.delegate.readByte();
//        };
//        if (inputStream.delegate.eof) {
//            inputStream.eof = inputStream.delegate.eof.bind(inputStream.delegate);
//        }
//        var outputStream = new Stream();
//        outputStream.pos = 0;
//        outputStream.writeByte = function() { this.pos++; };

        return result;
    }

    public Block readBlock(int blockNumber) throws IOException {
        // make a stream from a buffer, if necessary
        StreamBlock inputStream = new StreamBlock();

        return new Block();
    }

    public void getNextBlock() throws IOException {
        int i, j, k;
        BitReader reader = this.reader;
        // this is get_next_block() function from micro-bunzip:
  /* Read in header signature and CRC, then validate signature.
     (last block signature means CRC is for whole file, return now) */
        String h = reader.pi();
        if (SQRTPI.equals(h)) { // last block
            return; /* no more blocks */
        }
        if (!WHOLEPI.equals(h)) {
             throw new UnsupportedOperationException("non bzip data -2");
        }
        this.targetBlockCRC = reader.read(32) >>> 0; // (convert to unsigned)
        this.streamCRC = (this.targetBlockCRC ^
                ((this.streamCRC << 1) | (this.streamCRC>>>31))) >>> 0;
  /* We can add support for blockRandomised if anybody complains.  There was
     some code for this in busybox 1.0.0-pre3, but nobody ever noticed that
     it didn't actually work. */
//        if (reader.read(1))
//            _throw(Err.OBSOLETE_INPUT);
        byte origPointer = reader.read(24);
        if (origPointer > this.dbufSize) {
            throw new UnsupportedOperationException("initial position out of bounds");

        }
  /* mapping table: if some byte values are never used (encoding things
     like ascii text), the compression code removes the gaps to have fewer
     symbols to deal with, and writes a sparse bitfield indicating which
     values were present.  We make a translation table to convert the symbols
     back to the corresponding bytes. */
        int t = reader.read(16);
        byte[] symToByte = new byte[256];
        byte symTotal = 0;
        for (i = 0; i < 16; i++) {
            if ((t & (1 << (0xF - i))) != 0) {
                int o = i * 16;
                k = reader.read(16);
                for (j = 0; j < 16; j++)
                    if ((k & (1 << (0xF - j))) != 0)
                        symToByte[symTotal++] = (byte) (o + j);
            }
        }

        /* How many different huffman coding groups does this block use? */
        // TODO huffman
    }




    public void startBunzip() throws IOException {
        byte[] buf = new byte[4];
        StringBuilder sb = new StringBuilder();

        if (this.file.read(buf, 0, 4) != 4 ||
                "BZh".equals(sb.append(buf[0]).append(buf[1]).append(buf[2]).toString())) {
            throw new IllegalArgumentException("can't read magic bytes of bzip archive");
        }

        int level = buf[3] - 0x30;
        if (level < 1 || level > 9) {
            throw new IllegalArgumentException("magic bytes are our of range");
        }

        /* Fourth byte (ascii '1'-'9'), indicates block size in units of 100k of
     uncompressed data.  Allocate intermediate buffer for block. */
        this.dbufSize = 100000 * level;
        this.nextoutput = 0;
       // this.outputStream = outputStream;
        this.streamCRC = 0;
    }

    /**
     * Reads the stream header and checks that the data appears to be a valid BZip2 stream
     * @throws IOException if the stream header is not valid
     */
    private StreamBlock initialiseStream() throws IOException {

        StreamBlock streamBlock = new StreamBlock();


        /* Read the stream header */
        try {
            int marker1 = this.reader.read(16);
            int marker2 = this.reader.read(8);
            int blockSize = (this.reader.read(8) - '0');

            if (
                    ((marker1 != STREAM_START_MARKER_1))
                            || (marker2 != STREAM_START_MARKER_2)
                            || (blockSize < 1) || (blockSize > 9)) {
                throw new UnsupportedOperationException("Invalid BZip2 header");
            }

           // streamBlock.setBlockLength(blockSize * 100000);
        } catch (IOException e) {
            // If the stream header was not valid, stop trying to read more data
            //streamBlock.setStreamComplete(true);
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
        return (this.reader.read (16) << 16) | (this.reader.read (16));
    }

    private Huffman readHuffmanTables() throws IOException {

        final byte[] huffmanSymbolMap = new byte[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH];
        final byte[][] tableCodeLengths = new byte[HUFFMAN_MAXIMUM_ALPHABET_SIZE+1][HUFFMAN_MAXIMUM_ALPHABET_SIZE+1];

        int huffmanEndOfBlock = 0;

        /* Read Huffman symbol to output byte map */
        int huffmanUsedRanges = this.reader.read(16);
        int huffmanSymbolCount = 0;

        for (int i = 0; i < 16; i++) {
            if ((huffmanUsedRanges & ((1 << 15) >>> i)) != 0) {
//                for (int j = 0, k = i << 4; j < 16; j++, k++) {
//                    if (readBoolean()) {
//                        huffmanSymbolMap[huffmanSymbolCount++] = (byte)k;
//                    }
//                }
            }
        }
        int endOfBlockSymbol = huffmanSymbolCount + 1;

        /* Read total number of tables and selectors*/
        final int totalTables = this.reader.read (3);
        final int totalSelectors = this.reader.read (15);


        /* Read and decode MTFed Huffman selector list */
        final byte[] selectors = new byte[totalSelectors];
//        for (int selector = 0; selector < totalSelectors; selector++) {
//            selectors[selector] = (byte) readUnary();
//        }

        /* Read the Canonical Huffman code lengths for each table */
//        for (int table = 0; table < totalTables; table++) {
//            int currentLength = this.reader.read (5);
//            for (int i = 0; i <= endOfBlockSymbol; i++) {
//                while (readBoolean()) {
//                    currentLength += readBoolean() ? -1 : 1;
//                }
//                tableCodeLengths[table][i] = (byte)currentLength;
//            }
//        }

        return new Huffman ( endOfBlockSymbol + 1, tableCodeLengths, selectors, endOfBlockSymbol);

    }
}