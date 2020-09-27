package org.ivzh.bzip.indexer.dto;


// TODO make builder
public class Block {

    int blockNum;
    long blockOffset;
    long blockLength;
    long dataOffset;
    long dataLength;

    public Block() {
    }

    public Block(int blockNum, long blockOffset, long blockLength, long dataOffset, long dataLength) {
        this.blockNum = blockNum;
        this.blockOffset = blockOffset;
        this.blockLength = blockLength;
        this.dataOffset = dataOffset;
        this.dataLength = dataLength;
    }

    public int getBlockNum() {
        return blockNum;
    }

    public void setBlockNum(int blockNum) {
        this.blockNum = blockNum;
    }

    public long getBlockOffset() {
        return blockOffset;
    }

    public void setBlockOffset(long blockOffset) {
        this.blockOffset = blockOffset;
    }

    public long getBlockLength() {
        return blockLength;
    }

    public void setBlockLength(long blockLength) {
        this.blockLength = blockLength;
    }

    public long getDataOffset() {
        return dataOffset;
    }

    public void setDataOffset(long dataOffset) {
        this.dataOffset = dataOffset;
    }

    public long getDataLength() {
        return dataLength;
    }

    public void setDataLength(long dataLength) {
        this.dataLength = dataLength;
    }

    @Override
    public String toString() {
        return "{" +
                "blockNum=" + blockNum +
                ", blockOffset=" + blockOffset +
                ", blockLength=" + blockLength +
                ", dataOffset=" + dataOffset +
                ", dataLength=" + dataLength +
                '}';
    }
}