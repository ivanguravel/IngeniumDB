package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import org.ivzh.bzip.indexer.dto.*;
import org.ivzh.bzip.indexer.helper.BitReader;

import static org.ivzh.bzip.indexer.util.Const.*;

public class ExtendedBzipInputStream extends InputStream {


    private static final String WHOLEPI = "314159265359";
    private static final String SQRTPI = "177245385090";
    private static final int MIN_GROUPS = 2;
    private static final int MAX_GROUPS = 6;
    private static final int GROUP_SIZE = 50;
    private static final int SYMBOL_RUNA = 0;
    private static final int SYMBOL_RUNB = 1;
    public static final int MAX_HUFCODE_BITS = 20;
    public static final int MAX_SYMBOLS = 258;


    private static final String HEADER = "BZ";
    private static final int HEADER_LEN = HEADER.length();
    private static final String SUB_HEADER = "h9";
    private static final int SUB_HEADER_LEN = SUB_HEADER.length();


    private RandomAccessFile file;
    private BitReader reader;
    private CRC32 blockCRC;
    private StreamBlock streamBlock;

    StreamBlockOutput outputStream;

    int dbufSize;
    int nextoutput;
    int outputsize;
    int streamCRC;

    int targetBlockCRC;

    int[] dbuf;

    int writePos;
    int writeCurrent;
    int writeCount;
    int writeRun;


    public ExtendedBzipInputStream(String filePath, String mode) {
        File helper = new File(filePath);
        try {
            this.file = new RandomAccessFile(helper, mode);
        } catch (FileNotFoundException e) {
            throw new UnsupportedOperationException(e.getMessage());
        }
        this.reader = new BitReader(file);
        this.blockCRC = new CRC32();
        this.streamBlock = new StreamBlock();
//        try {
//            startBunzip();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private ExtendedBzipInputStream(RandomAccessFile file, StreamBlockInput inputStream, StreamBlockOutput outputStream) throws IOException {
        this.writePos = this.writeCurrent = this.writeCount = 0;
        this.file = file;
        this.reader = new BitReader(this.file);
        this.startBunzip(inputStream, outputStream);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }


    public void startBunzip(StreamBlockInput inputStream, StreamBlockOutput outputStream) throws IOException {
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

    public List<Block> indexBzip() throws IOException {
        //StreamBlock streamBlock = initialiseStream();
        int count = 0;
        List<Block> result = new LinkedList<>();

        Block block = new Block();

        StreamBlockInput inputStream = new StreamBlockInput();
        inputStream.randomAccessFile = file;
        StreamBlockOutput outputStream = new StreamBlockOutput();
        outputStream.pos = 0;
        this.outputStream = outputStream;

        ExtendedBzipInputStream bz = new ExtendedBzipInputStream(this.file, inputStream, outputStream);
        int blockSize = bz.dbufSize;
        while (true) {
            if ( inputStream.eof()) break;

            int position = inputStream.pos *8 + bz.reader.bitOffset;
            if (bz.reader.hasByte) {
                position -= 8;
            }

            if (bz.initBlock()) {
                int start = outputStream.pos;
                bz.readBunzip();
                block.setBlockOffset(position);
                block.setBlockLength(outputStream.pos - start);
            } else {
                byte crc = bz.reader.read(32); // (but we ignore the crc)
                if (!inputStream.eof()) {
                    // note that start_bunzip will also resync the bit reader to next byte
                    bz.startBunzip(inputStream, outputStream);

                    if (bz.dbufSize == blockSize) {
                        System.err.println("shouldn't change block size within multistream file");
                    }
                } else {
                    break;
                }
            }
        }

        return result;
    }


    int readBunzip () {
        int copies, previous, outbyte;
    /* james@jamestaylor.org: writeCount goes to -1 when the buffer is fully
       decoded, which results in this returning RETVAL_LAST_BLOCK, also
       equal to -1... Confusing, I'm returning 0 here to indicate no
       bytes written into the buffer */
        if (this.writeCount < 0) {
            return 0;
        }

        int gotcount = 0;
        int[] dbuf = this.dbuf;
        int pos = this.writePos;
        int current = this.writeCurrent;
        int dbufCount = this.writeCount;
        outputsize = this.outputsize;
        int run = this.writeRun;

        while (dbufCount == 0) {
            dbufCount--;
            previous = current;
            pos = dbuf[pos];
            current = pos & 0xff;
            pos >>= 8;
            if (run++ == 3) {
                copies = current;
                outbyte = previous;
                current = -1;
            } else {
                copies = 1;
                outbyte = current;
            }
            this.blockCRC.updateCRCRun(outbyte, copies);
            while (copies-- != 0) {
                this.outputStream.writeByte(outbyte);
                this.nextoutput++;
            }
            if (current != previous)
                run = 0;
        }
        this.writeCount = dbufCount;
        // check CRC
        if (this.blockCRC.getCRC() != this.targetBlockCRC) {
            throw new UnsupportedOperationException("Bad block CRC " +
                    "(got " + this.blockCRC.getCRC() +
                    " expected " + this.targetBlockCRC + ")");
        }
        return this.nextoutput;
    }

    public Block readBlock(int blockNumber) throws IOException {
        // make a stream from a buffer, if necessary
        StreamBlock inputStream = new StreamBlock();

        return new Block();
    }


    public boolean initBlock() throws IOException {
        boolean moreBlocks = getNextBlock();
        if ( !moreBlocks ) {
            this.writeCount = -1;
            return false; /* no more blocks */
        }
        this.blockCRC = new CRC32();
        return true;
    }

    public boolean getNextBlock() throws IOException {
        int i, j, k;
        BitReader reader = this.reader;
        // this is get_next_block() function from micro-bunzip:
  /* Read in header signature and CRC, then validate signature.
     (last block signature means CRC is for whole file, return now) */
        String h = reader.pi();
        if (SQRTPI.equals(h)) { // last block
            return false; /* no more blocks */
        }
        if (!WHOLEPI.equals(h)) {
            throw new UnsupportedOperationException("non bzip data -2");
        }
        this.targetBlockCRC = reader.read(32) >>> 0; // (convert to unsigned)
        this.streamCRC = (this.targetBlockCRC ^
                ((this.streamCRC << 1) | (this.streamCRC >>> 31))) >>> 0;
  /* We can add support for blockRandomised if anybody complains.  There was
     some code for this in busybox 1.0.0-pre3, but nobody ever noticed that
     it didn't actually work. */
        if (reader.read(1) == 0) {
            throw new UnsupportedOperationException("OBSOLETE_INPUT");
        }
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
        int groupCount = reader.read(3);
        if (groupCount < MIN_GROUPS || groupCount > MAX_GROUPS) {
            throw new UnsupportedOperationException("Data error -5");
        }


        /* nSelectors: Every GROUP_SIZE many symbols we select a new huffman coding
         group.  Read in the group selector list, which is stored as MTF encoded
         bit runs.  (MTF=Move To Front, as each value is used it's moved to the
         start of the list.) */
        int nSelectors = reader.read(15);
        if (nSelectors == 0) {
            throw new UnsupportedOperationException("Data error -5 for nSectors");
        }

        int[] mtfSymbol = new int[256];
        for (i = 0; i < groupCount; i++)
            mtfSymbol[i] = i;

        int[] selectors = new int[nSelectors]; // was 32768...

        for (i = 0; i < nSelectors; i++) {
            /* Get next value */
            for (j = 0; i < reader.read(1); j++)
                if (j >= groupCount) {
                    throw new UnsupportedOperationException("DATA_ERROR -5");
                }
            /* Decode MTF to get the next selector */
            selectors[i] = mtf(mtfSymbol, j);
        }

       /* Read the huffman coding tables for each group, which code for symTotal
     literal symbols, plus two run symbols (RUNA, RUNB) */
        int symCount = symTotal + 2;
        List<HuffmanGroup> groups = new LinkedList<>();
        HuffmanGroup hufGroup = null;
        for (j = 0; j < groupCount; j++) {
            int[] length = new int[symCount];
            int[] temp = new int[MAX_HUFCODE_BITS + 1];
        /* Read huffman code lengths for each symbol.  They're stored in
           a way similar to mtf; record a starting value for the first symbol,
           and an offset from the previous value for everys symbol after that. */
            t = reader.read(5); // lengths
            for (i = 0; i < symCount; i++) {
                for (; ; ) {
                    if (t < 1 || t > MAX_HUFCODE_BITS) {
                        throw new UnsupportedOperationException("DATA ERROR");
                    }
                    /* If first bit is 0, stop.  Else second bit indicates whether
                       to increment or decrement the value. */
                    if (reader.read(1) != 0)
                        break;
                    if (reader.read(1) != 0)
                        t++;
                    else
                        t--;
                }
                length[i] = t;
            }

            /* Find largest and smallest lengths in this group */
            int minLen, maxLen;
            minLen = maxLen = length[0];
            for (i = 1; i < symCount; i++) {
                if (length[i] > maxLen)
                    maxLen = length[i];
                else if (length[i] < minLen)
                    minLen = length[i];
            }

            /* Calculate permute[], base[], and limit[] tables from length[].
             *
             * permute[] is the lookup table for converting huffman coded symbols
             * into decoded symbols.  base[] is the amount to subtract from the
             * value of a huffman symbol of a given length when using permute[].
             *
             * limit[] indicates the largest numerical value a symbol with a given
             * number of bits can have.  This is how the huffman codes can vary in
             * length: each code with a value>limit[length] needs another bit.
             */
            hufGroup = new HuffmanGroup(minLen, maxLen);
            /* Calculate permute[].  Concurently, initialize temp[] and limit[]. */
            int pp = 0;
            for (i = minLen; i <= maxLen; i++) {
                temp[i] = hufGroup.getLimit()[i] = 0;
                for (t = 0; t < symCount; t++)
                    if (length[t] == i)
                        hufGroup.getPermute()[pp++] = t;
            }
            /* Count symbols coded for at each bit length */
            for (i = 0; i < symCount; i++) {
                temp[length[i]]++;
            }
            /* Calculate limit[] (the largest symbol-coding value at each bit
             * length, which is (previous limit<<1)+symbols at this level), and
             * base[] (number of symbols to ignore at each bit length, which is
             * limit minus the cumulative count of symbols coded for already). */
            pp = t = 0;
            for (i = minLen; i < maxLen; i++) {
                pp += temp[i];
              /* We read the largest possible symbol size and then unget bits
                 after determining how many we need, and those extra bits could
                 be set to anything.  (They're noise from future symbols.)  At
                 each level we're really only interested in the first few bits,
                 so here we set all the trailing to-be-ignored bits to 1 so they
                 don't affect the value>limit[length] comparison. */
                hufGroup.getLimit()[i] = pp - 1;
                pp <<= 1;
                t += temp[i];
                hufGroup.getBase()[i + 1] = pp - t;
            }
            hufGroup.getLimit()[maxLen + 1] = Integer.MAX_VALUE; /* Sentinal value for reading next sym. */
            hufGroup.getLimit()[maxLen] = pp + temp[maxLen] - 1;
            hufGroup.getBase()[minLen] = 0;

            groups.add(hufGroup);
        }

         /* We've finished reading and digesting the block header.  Now read this
     block's huffman coded symbols from the file and undo the huffman coding
     and run length encoding, saving the result into dbuf[dbufCount++]=uc */

        /* Initialize symbol occurrence counters and symbol Move To Front table */
        int[] byteCount = new int[256];
        for (i = 0; i < 256; i++)
            mtfSymbol[i] = i;
        /* Loop through compressed symbols. */
        int runPos = 0, dbufCount = 0, selector = 0, uc;
        int[] dbuf = this.dbuf = new int[this.dbufSize];
        symCount = 0;
        for (; ; ) {
            /* Determine which huffman coding group to use. */
            if ((symCount--) != 0) {
                symCount = GROUP_SIZE - 1;
                if (selector >= nSelectors) {
                    throw new UnsupportedOperationException("DATA ERROR");
                }
                hufGroup = groups.get(selectors[selector++]);
            }
            /* Read next huffman-coded symbol. */
            i = hufGroup.getMinLen();
            j = reader.read(i);
            for (; ; i++) {
                if (i > hufGroup.getMaxLen()) {
                    throw new UnsupportedOperationException("DATA ERROR");
                }
                if (j <= hufGroup.getLimit()[i])
                    break;
                j = (j << 1) | reader.read(1);
            }
            /* Huffman decode value to get nextSym (with bounds checking) */
            j -= hufGroup.getBase()[i];
            if (j < 0 || j >= MAX_SYMBOLS) {
                throw new UnsupportedOperationException("DATA ERROR");
            }
            int nextSym = hufGroup.getPermute()[j];
            /* We have now decoded the symbol, which indicates either a new literal
               byte, or a repeated run of the most recent literal byte.  First,
               check if nextSym indicates a repeated run, and if so loop collecting
               how many times to repeat the last literal. */
            if (nextSym == SYMBOL_RUNA || nextSym == SYMBOL_RUNB) {
                /* If this is the start of a new run, zero out counter */
                if (runPos == 0) {
                    runPos = 1;
                    t = 0;
                }
          /* Neat trick that saves 1 symbol: instead of or-ing 0 or 1 at
             each bit position, add 1 or 2 instead.  For example,
             1011 is 1<<0 + 1<<1 + 2<<2.  1010 is 2<<0 + 2<<1 + 1<<2.
             You can make any bit pattern that way using 1 less symbol than
             the basic or 0/1 method (except all bits 0, which would use no
             symbols, but a run of length 0 doesn't mean anything in this
             context).  Thus space is saved. */
                if (nextSym == SYMBOL_RUNA)
                    t += runPos;
                else
                    t += 2 * runPos;
                runPos <<= 1;
                continue;
            }
        /* When we hit the first non-run symbol after a run, we now know
           how many times to repeat the last literal, so append that many
           copies to our buffer of decoded symbols (dbuf) now.  (The last
           literal used is the one at the head of the mtfSymbol array.) */
            if (runPos != 0) {
                runPos = 0;
                if (dbufCount + t > this.dbufSize) {
                    throw new UnsupportedOperationException("DATA ERROR");
                }
                uc = symToByte[mtfSymbol[0]];
                byteCount[uc] += t;
                while (t-- != 0)
                    dbuf[dbufCount++] = uc;
            }
            /* Is this the terminating symbol? */
            if (nextSym > symTotal)
                break;
        /* At this point, nextSym indicates a new literal character.  Subtract
           one to get the position in the MTF array at which this literal is
           currently to be found.  (Note that the result can't be -1 or 0,
           because 0 and 1 are RUNA and RUNB.  But another instance of the
           first symbol in the mtf array, position 0, would have been handled
           as part of a run above.  Therefore 1 unused mtf position minus
           2 non-literal nextSym values equals -1.) */
            if (dbufCount >= this.dbufSize) {
                throw new UnsupportedOperationException("DATA ERROR");
            }
            i = nextSym - 1;
            uc = mtf(mtfSymbol, i);
            uc = symToByte[uc];
            /* We have our literal byte.  Save it into dbuf. */
            byteCount[uc]++;
            dbuf[dbufCount++] = uc;
        }


        /* At this point, we've read all the huffman-coded symbols (and repeated
     runs) for this block from the input stream, and decoded them into the
     intermediate buffer.  There are dbufCount many decoded bytes in dbuf[].
     Now undo the Burrows-Wheeler transform on dbuf.
     See http://dogma.net/markn/articles/bwt/bwt.htm
  */
        if (origPointer < 0 || origPointer >= dbufCount) {
            throw new UnsupportedOperationException("DATA ERROR");
        }
        /* Turn byteCount into cumulative occurrence counts of 0 to n-1. */
        j = 0;
        for (i = 0; i < 256; i++) {
            k = j + byteCount[i];
            byteCount[i] = j;
            j = k;
        }
        /* Figure out what order dbuf would be in if we sorted it. */
        for (i = 0; i < dbufCount; i++) {
            uc = dbuf[i] & 0xff;
            dbuf[byteCount[uc]] |= (i << 8);
            byteCount[uc]++;
        }
  /* Decode first byte by hand to initialize "previous" byte.  Note that it
     doesn't get output, and if the first three characters are identical
     it doesn't qualify as a run (hence writeRunCountdown=5). */
        int pos = 0, current = 0, run = 0;
        if (dbufCount != 0) {
            pos = dbuf[origPointer];
            current = (pos & 0xff);
            pos >>= 8;
            run = -1;
        }
        this.writePos = pos;
        this.writeCurrent = current;
        this.writeCount = dbufCount;
        this.writeRun = run;

        return true; /* more blocks to come */
    }


    private int mtf(int[] array, int index) {
        int src = array[index];
        for (int i = index; i > 0; i--) {
            array[i] = array[i - 1];
        }
        array[0] = src;
        return src;
    }



    /**
     * Reads the stream header and checks that the data appears to be a valid BZip2 stream
     *
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
                ((buffer[offset + 1] & 0xFF) << 8));
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
        return (this.reader.read(16) << 16) | (this.reader.read(16));
    }

    private Huffman readHuffmanTables() throws IOException {

        final byte[] huffmanSymbolMap = new byte[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH];
        final byte[][] tableCodeLengths = new byte[HUFFMAN_MAXIMUM_ALPHABET_SIZE + 1][HUFFMAN_MAXIMUM_ALPHABET_SIZE + 1];

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
        final int totalTables = this.reader.read(3);
        final int totalSelectors = this.reader.read(15);


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

        return new Huffman(endOfBlockSymbol + 1, tableCodeLengths, selectors, endOfBlockSymbol);

    }
}