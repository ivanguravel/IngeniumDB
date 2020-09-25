package org.ivzh.bzip.indexer.util;

public interface Const {
    int COMPRESSED_BLOCK_SIZE = 65_536;
    int HEADER_SIZE = 36;
    int BLOCK_LENGTH_OFFSET = 16;

    int HUFFMAN_MAXIMUM_ALPHABET_SIZE = 258;
    int HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH = 23;
}
