package org.ivzh.bzip.indexer.util;

public interface Const {
    int COMPRESSED_BLOCK_SIZE = 65_536;
    int HEADER_SIZE = 36;
    int BLOCK_LENGTH_OFFSET = 16;

    int HUFFMAN_MAXIMUM_ALPHABET_SIZE = 258;
    int HUFFMAN_DECODE_MAXIMUM_CODE_LENGTH = 23;


    /**
     * 'B' 'Z' that marks the start of a BZip2 stream
     */
    int STREAM_START_MARKER_1 = 0x425a;

    /**
     * 'h' that distinguishes BZip from BZip2
     */
    int STREAM_START_MARKER_2 = 0x68;
}
