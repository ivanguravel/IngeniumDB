package org.ivzh.bzip.indexer.dto;

import java.io.IOException;

import static org.ivzh.bzip.indexer.util.Const.HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH;
import static org.ivzh.bzip.indexer.util.Const.HUFFMAN_MAXIMUM_ALPHABET_SIZE;

public class Huffman {
    private byte[] selectors;

    private final int[] minimumLengths = new int[HUFFMAN_MAXIMUM_ALPHABET_SIZE];

    private int endOfBlock;


    private final int[][] codeBases = new int[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+1][HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+ 2];

    private final int[][] codeLimits = new int[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+1][HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+ 2];

    private final int[][] codeSymbols =new int[HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+1][HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH+ 2];

    private int currentTable;

    private int groupIndex = -1;

    private int groupPosition = -1;

    public Huffman (final int alphabetSize, final byte[][] tableCodeLengths, final byte[] selectors, int endOfBlock) {

        this.selectors = selectors;
        this.currentTable = this.selectors[0];
        this.endOfBlock = endOfBlock;

    }


}
