package com.bzip2.block.dto;

public class BzipBlock {

    private final int uncompressedDataSize;
    private final int blockSize;
    private final long compressedByteOffset;
    private final byte compressedBitOffset;

    public BzipBlock(int uncompressedDataSize, long compressedByteOffset, byte compressedBitOffset, int blockSize) {
        this.uncompressedDataSize = uncompressedDataSize;
        this.blockSize = blockSize;
        this.compressedByteOffset = compressedByteOffset;
        this.compressedBitOffset = compressedBitOffset;
    }

    public int getUncompressedDataSize() {
        return uncompressedDataSize;
    }

    public long getCompressedByteOffset() {
        return compressedByteOffset;
    }

    public byte getCompressedBitOffset() {
        return compressedBitOffset;
    }

    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public String toString() {
        return "Bzip2Block{" + "dataSize=" + uncompressedDataSize +
                ", byteOffset=" + compressedByteOffset +
                ", bitOffset=" + compressedBitOffset +
                ", blockSize=" + blockSize +
                '}';
    }
}
