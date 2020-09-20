package org.ivzh.bzip.indexer.index;

public class DecompressedBlock {
    /**
     * Decompressed block
     */
    private final byte[] mBlock;
    /**
     * Compressed size of block (the uncompressed size can be found using
     * mBlock.length)
     */
    private final int mBlockCompressedSize;
    /**
     * Stream offset of start of block
     */
    private final long mBlockAddress;
    /**
     * Exception thrown (if any) when attempting to decompress block
     */
    private final Exception mException;

    public DecompressedBlock(long blockAddress, byte[] block, int compressedSize) {
        mBlock = block;
        mBlockAddress = blockAddress;
        mBlockCompressedSize = compressedSize;
        mException = null;
    }

    public DecompressedBlock(long blockAddress, int compressedSize, Exception exception) {
        mBlock = new byte[0];
        mBlockAddress = blockAddress;
        mBlockCompressedSize = compressedSize;
        mException = exception;
    }
}