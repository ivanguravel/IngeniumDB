package org.ivzh.bzip.indexer.helper;


import org.ivzh.bzip.indexer.dto.CRC32;
import org.ivzh.bzip.indexer.dto.MTF;

import java.io.IOException;

import static org.ivzh.bzip.indexer.util.Const.*;


/*
 * Block decoding consists of the following stages:
 * 1. Read block header - BZip2BlockDecompressor()
 * 2. Read Huffman tables - readHuffmanTables()
 * 3. Read and decode Huffman encoded data - decodeHuffmanData()
 * 4. Run-Length Decoding[2] - decodeHuffmanData()
 * 5. Inverse Move To Front Transform - decodeHuffmanData()
 * 6. Inverse Burrows Wheeler Transform - initialiseInverseBWT()
 * 7. Run-Length Decoding[1] - read()
 * 8. Optional Block De-Randomisation - read() (through decodeNextBWTByte())
 */
/**
 * Reads and decompresses a single BZip2 block
 */
public class BlockDecompressor {


    private static final int[] RNUMS = {
            619, 720, 127, 481, 931, 816, 813, 233, 566, 247, 985, 724, 205, 454, 863, 491,
            741, 242, 949, 214, 733, 859, 335, 708, 621, 574, 73, 654, 730, 472, 419, 436,
            278, 496, 867, 210, 399, 680, 480, 51, 878, 465, 811, 169, 869, 675, 611, 697,
            867, 561, 862, 687, 507, 283, 482, 129, 807, 591, 733, 623, 150, 238, 59, 379,
            684, 877, 625, 169, 643, 105, 170, 607, 520, 932, 727, 476, 693, 425, 174, 647,
            73, 122, 335, 530, 442, 853, 695, 249, 445, 515, 909, 545, 703, 919, 874, 474,
            882, 500, 594, 612, 641, 801, 220, 162, 819, 984, 589, 513, 495, 799, 161, 604,
            958, 533, 221, 400, 386, 867, 600, 782, 382, 596, 414, 171, 516, 375, 682, 485,
            911, 276, 98, 553, 163, 354, 666, 933, 424, 341, 533, 870, 227, 730, 475, 186,
            263, 647, 537, 686, 600, 224, 469, 68, 770, 919, 190, 373, 294, 822, 808, 206,
            184, 943, 795, 384, 383, 461, 404, 758, 839, 887, 715, 67, 618, 276, 204, 918,
            873, 777, 604, 560, 951, 160, 578, 722, 79, 804, 96, 409, 713, 940, 652, 934,
            970, 447, 318, 353, 859, 672, 112, 785, 645, 863, 803, 350, 139, 93, 354, 99,
            820, 908, 609, 772, 154, 274, 580, 184, 79, 626, 630, 742, 653, 282, 762, 623,
            680, 81, 927, 626, 789, 125, 411, 521, 938, 300, 821, 78, 343, 175, 128, 250,
            170, 774, 972, 275, 999, 639, 495, 78, 352, 126, 857, 956, 358, 619, 580, 124,
            737, 594, 701, 612, 669, 112, 134, 694, 363, 992, 809, 743, 168, 974, 944, 375,
            748, 52, 600, 747, 642, 182, 862, 81, 344, 805, 988, 739, 511, 655, 814, 334,
            249, 515, 897, 955, 664, 981, 649, 113, 974, 459, 893, 228, 433, 837, 553, 268,
            926, 240, 102, 654, 459, 51, 686, 754, 806, 760, 493, 403, 415, 394, 687, 700,
            946, 670, 656, 610, 738, 392, 760, 799, 887, 653, 978, 321, 576, 617, 626, 502,
            894, 679, 243, 440, 680, 879, 194, 572, 640, 724, 926, 56, 204, 700, 707, 151,
            457, 449, 797, 195, 791, 558, 945, 679, 297, 59, 87, 824, 713, 663, 412, 693,
            342, 606, 134, 108, 571, 364, 631, 212, 174, 643, 304, 329, 343, 97, 430, 751,
            497, 314, 983, 374, 822, 928, 140, 206, 73, 263, 980, 736, 876, 478, 430, 305,
            170, 514, 364, 692, 829, 82, 855, 953, 676, 246, 369, 970, 294, 750, 807, 827,
            150, 790, 288, 923, 804, 378, 215, 828, 592, 281, 565, 555, 710, 82, 896, 831,
            547, 261, 524, 462, 293, 465, 502, 56, 661, 821, 976, 991, 658, 869, 905, 758,
            745, 193, 768, 550, 608, 933, 378, 286, 215, 979, 792, 961, 61, 688, 793, 644,
            986, 403, 106, 366, 905, 644, 372, 567, 466, 434, 645, 210, 389, 550, 919, 135,
            780, 773, 635, 389, 707, 100, 626, 958, 165, 504, 920, 176, 193, 713, 857, 265,
            203, 50, 668, 108, 645, 990, 626, 197, 510, 357, 358, 850, 858, 364, 936, 638
    };

 
    private final BitReader bitReader;

    private final CRC32 crc = new CRC32();

    private final int blockCRC;

    private final boolean blockRandomised;

    private int huffmanEndOfBlockSymbol;

    private final byte[] huffmanSymbolMap = new byte[256];

    private final int[] bwtByteCounts = new int[256];

    private byte[] bwtBlock;


    private int[] bwtMergedPointers;

    private int bwtCurrentMergedPointer;


    private int bwtBlockLength;


    private int bwtBytesDecoded;


    private int rleLastDecodedByte = -1;


    private int rleAccumulator;

    private int rleRepeat;

    private int randomIndex = 0;

    private int randomCount = RNUMS[0] - 1;

    private HuffmanDecoder readHuffmanTables() throws IOException {

        final BitReader bitReader = this.bitReader;
        final byte[] huffmanSymbolMap = this.huffmanSymbolMap;
        final byte[][] tableCodeLengths = new byte[HUFFMAN_MAXIMUM_TABLES][HUFFMAN_MAXIMUM_ALPHABET_SIZE];

        /* Read Huffman symbol to output byte map */
        int huffmanUsedRanges = bitReader.readBits (16);
        int huffmanSymbolCount = 0;

        for (int i = 0; i < 16; i++) {
            if ((huffmanUsedRanges & ((1 << 15) >>> i)) != 0) {
                for (int j = 0, k = i << 4; j < 16; j++, k++) {
                    if (bitReader.readBoolean()) {
                        huffmanSymbolMap[huffmanSymbolCount++] = (byte)k;
                    }
                }
            }
        }
        int endOfBlockSymbol = huffmanSymbolCount + 1;
        this.huffmanEndOfBlockSymbol = endOfBlockSymbol;

        /* Read total number of tables and selectors*/
        final int totalTables = bitReader.readBits (3);
        final int totalSelectors = bitReader.readBits (15);
        if (
                (totalTables < HUFFMAN_MINIMUM_TABLES)
                        || (totalTables > HUFFMAN_MAXIMUM_TABLES)
                        || (totalSelectors < 1)
                        || (totalSelectors > HUFFMAN_MAXIMUM_SELECTORS)
        )
        {
            throw new UnsupportedOperationException ("BZip2 block Huffman tables invalid");
        }

        /* Read and decode MTFed Huffman selector list */
        final MTF tableMTF = new MTF();
        final byte[] selectors = new byte[totalSelectors];
        for (int selector = 0; selector < totalSelectors; selector++) {
            selectors[selector] = tableMTF.indexToFront (bitReader.readUnary());
        }

        /* Read the Canonical Huffman code lengths for each table */
        for (int table = 0; table < totalTables; table++) {
            int currentLength = bitReader.readBits (5);
            for (int i = 0; i <= endOfBlockSymbol; i++) {
                while (bitReader.readBoolean()) {
                    currentLength += bitReader.readBoolean() ? -1 : 1;
                }
                tableCodeLengths[table][i] = (byte)currentLength;
            }
        }

        return new HuffmanDecoder (bitReader, endOfBlockSymbol + 1, tableCodeLengths, selectors);

    }

    private void decodeHuffmanData (final HuffmanDecoder huffmanDecoder) throws IOException {

        final byte[] bwtBlock = this.bwtBlock;
        final byte[] huffmanSymbolMap = this.huffmanSymbolMap;
        final int streamBlockSize = this.bwtBlock.length;
        final int huffmanEndOfBlockSymbol = this.huffmanEndOfBlockSymbol;
        final int[] bwtByteCounts = this.bwtByteCounts;
        final MTF symbolMTF = new MTF();
        int bwtBlockLength = 0;
        int repeatCount = 0;
        int repeatIncrement = 1;
        int mtfValue = 0;

        for (;;) {
            final int nextSymbol = huffmanDecoder.nextSymbol();

            if (nextSymbol == HUFFMAN_SYMBOL_RUNA) {
                repeatCount += repeatIncrement;
                repeatIncrement <<= 1;
            } else if (nextSymbol == HUFFMAN_SYMBOL_RUNB) {
                repeatCount += repeatIncrement << 1;
                repeatIncrement <<= 1;
            } else {
                if (repeatCount > 0) {
                    if (bwtBlockLength + repeatCount > streamBlockSize) {
                        throw new UnsupportedOperationException ("BZip2 block exceeds declared block size");
                    }
                    final byte nextByte = huffmanSymbolMap[mtfValue];
                    bwtByteCounts[nextByte & 0xff] += repeatCount;
                    while (--repeatCount >= 0) {
                        bwtBlock[bwtBlockLength++] = nextByte;
                    }

                    repeatCount = 0;
                    repeatIncrement = 1;
                }

                if (nextSymbol == huffmanEndOfBlockSymbol)
                    break;

                if (bwtBlockLength >= streamBlockSize) {
                    throw new UnsupportedOperationException ("BZip2 block exceeds declared block size");
                }

                mtfValue = symbolMTF.indexToFront (nextSymbol - 1) & 0xff;

                final byte nextByte = huffmanSymbolMap[mtfValue];
                bwtByteCounts[nextByte & 0xff]++;
                bwtBlock[bwtBlockLength++] = nextByte;

            }
        }

        this.bwtBlockLength = bwtBlockLength;

    }

    private void initialiseInverseBWT (final int bwtStartPointer) throws IOException {

        final byte[] bwtBlock  = this.bwtBlock;
        final int[] bwtMergedPointers = new int[this.bwtBlockLength];
        final int[] characterBase = new int[256];

        if ((bwtStartPointer < 0) || (bwtStartPointer >= this.bwtBlockLength)) {
            throw new UnsupportedOperationException ("BZip2 start pointer invalid");
        }

        System.arraycopy (this.bwtByteCounts, 0, characterBase, 1, 255);
        for (int i = 2; i <= 255; i++) {
            characterBase[i] += characterBase[i - 1];
        }

        for (int i = 0; i < this.bwtBlockLength; i++) {
            int value = bwtBlock[i] & 0xff;
            bwtMergedPointers[characterBase[value]++] = (i << 8) + value;
        }

        this.bwtBlock = null;
        this.bwtMergedPointers = bwtMergedPointers;
        this.bwtCurrentMergedPointer = bwtMergedPointers[bwtStartPointer];

    }

    private int decodeNextBWTByte() {

        int mergedPointer = this.bwtCurrentMergedPointer;
        int nextDecodedByte =  mergedPointer & 0xff;
        this.bwtCurrentMergedPointer = this.bwtMergedPointers[mergedPointer >>> 8];

        if (this.blockRandomised) {
            if (--this.randomCount == 0) {
                nextDecodedByte ^= 1;
                this.randomIndex = (this.randomIndex + 1) % 512;
                this.randomCount = RNUMS[this.randomIndex];
            }
        }

        this.bwtBytesDecoded++;

        return nextDecodedByte;

    }

    public int read() {

        while (this.rleRepeat < 1) {

            if (this.bwtBytesDecoded == this.bwtBlockLength) {
                return -1;
            }

            int nextByte = decodeNextBWTByte();

            if (nextByte != this.rleLastDecodedByte) {
                // New byte, restart accumulation
                this.rleLastDecodedByte = nextByte;
                this.rleRepeat = 1;
                this.rleAccumulator = 1;
                this.crc.updateCRC (nextByte);
            } else {
                if (++this.rleAccumulator == 4) {
                    // Accumulation complete, start repetition
                    int rleRepeat = decodeNextBWTByte() + 1;
                    this.rleRepeat = rleRepeat;
                    this.rleAccumulator = 0;
                    this.crc.updateCRC (nextByte, rleRepeat);
                } else {
                    this.rleRepeat = 1;
                    this.crc.updateCRC (nextByte);
                }
            }

        }

        this.rleRepeat--;

        return this.rleLastDecodedByte;

    }

    public int read (final byte[] destination, int offset, final int length) {

        int i;
        for (i = 0; i < length; i++, offset++) {
            int decoded = read();
            if (decoded == -1) {
                return (i == 0) ? -1 : i;
            }
            destination[offset] = (byte)decoded;
        }
        return i;

    }

    public int checkCRC() {

        if (this.blockCRC != this.crc.getCRC()) {
            throw new UnsupportedOperationException ("BZip2 block CRC error");
        }

        return this.crc.getCRC();

    }

    public BlockDecompressor(final BitReader bitReader, final int blockSize) throws IOException {

        this.bitReader = bitReader;
        this.bwtBlock = new byte[blockSize];

        final int bwtStartPointer;

        // Read block header
        this.blockCRC = this.bitReader.readInteger();
        this.blockRandomised = this.bitReader.readBoolean();
        bwtStartPointer = this.bitReader.readBits (24);

        // Read block data and decode through to the Inverse Burrows Wheeler Transform stage
        HuffmanDecoder huffmanDecoder = readHuffmanTables();
        decodeHuffmanData (huffmanDecoder);
        initialiseInverseBWT (bwtStartPointer);

    }


}
