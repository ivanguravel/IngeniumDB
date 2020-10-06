package org.ivzh.bzip.indexer.helper;

import java.io.IOException;

import static org.ivzh.bzip.indexer.util.Const.*;

public class HuffmanDecoder {

    private final BitReader bitReader;


    private final byte[] selectors;

    private final int[] minimumLengths = new int[HUFFMAN_MAXIMUM_TABLES];

    private final int[][] codeBases = new int[HUFFMAN_MAXIMUM_TABLES][HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH + 2];

    private final int[][] codeLimits = new int[HUFFMAN_MAXIMUM_TABLES][HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH + 1];

    private final int[][] codeSymbols = new int[HUFFMAN_MAXIMUM_TABLES][HUFFMAN_MAXIMUM_ALPHABET_SIZE];

    private int currentTable;

    private int groupIndex = -1;

    private int groupPosition = -1;

    private void createHuffmanDecodingTables (final int alphabetSize, final byte[][] tableCodeLengths) {

        for (int table = 0; table < tableCodeLengths.length; table++) {

            final int[] tableBases = this.codeBases[table];
            final int[] tableLimits = this.codeLimits[table];
            final int[] tableSymbols = this.codeSymbols[table];

            final byte[] codeLengths = tableCodeLengths[table];
            int minimumLength = HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH;
            int maximumLength = 0;

            for (int i = 0; i < alphabetSize; i++) {
                maximumLength = Math.max (codeLengths[i], maximumLength);
                minimumLength = Math.min (codeLengths[i], minimumLength);
            }
            this.minimumLengths[table] = minimumLength;

            for (int i = 0; i < alphabetSize; i++) {
                tableBases[codeLengths[i] + 1]++;
            }
            for (int i = 1; i < HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH + 2; i++) {
                tableBases[i] += tableBases[i - 1];
            }

            int code = 0;
            for (int i = minimumLength; i <= maximumLength; i++) {
                int base = code;
                code += tableBases[i + 1] - tableBases[i];
                tableBases[i] = base - tableBases[i];
                tableLimits[i] = code - 1;
                code <<= 1;
            }

            int codeIndex = 0;
            for (int bitLength = minimumLength; bitLength <= maximumLength; bitLength++) {
                for (int symbol = 0; symbol < alphabetSize; symbol++) {
                    if (codeLengths[symbol] == bitLength) {
                        tableSymbols[codeIndex++] = symbol;
                    }
                }
            }

        }

    }

    public int nextSymbol() throws IOException {

        final BitReader bitReader = this.bitReader;

        if (((++this.groupPosition % HUFFMAN_GROUP_RUN_LENGTH) == 0)) {
            this.groupIndex++;
            if (this.groupIndex == this.selectors.length) {
                throw new UnsupportedOperationException ("Error decoding BZip2 block");
            }
            this.currentTable = this.selectors[this.groupIndex] & 0xff;
        }

        final int currentTable = this.currentTable;
        final int[] tableLimits = this.codeLimits[currentTable];
        int codeLength = this.minimumLengths[currentTable];

        int codeBits = bitReader.readBits (codeLength);
        for (; codeLength <= HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH; codeLength++) {
            if (codeBits <= tableLimits[codeLength]) {
                // Convert the code to a symbol index and return
                return this.codeSymbols[currentTable][codeBits - this.codeBases[currentTable][codeLength]];
            }
            codeBits = (codeBits << 1) | bitReader.readBits (1);
        }

        throw new UnsupportedOperationException ("Error decoding BZip2 block");

    }

    public HuffmanDecoder(final BitReader bitReader, final int alphabetSize, final byte[][] tableCodeLengths, final byte[] selectors) {

        this.bitReader = bitReader;
        this.selectors = selectors;
        this.currentTable = this.selectors[0];

        createHuffmanDecodingTables (alphabetSize, tableCodeLengths);
    }
}
