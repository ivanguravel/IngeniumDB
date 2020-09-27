package org.ivzh.bzip.indexer.dto;

public class StreamBlock extends Block {

    boolean streamComplete;
    boolean headerless;
    int streamCRC;


    public boolean isStreamComplete() {
        return streamComplete;
    }

    public void setStreamComplete(boolean streamComplete) {
        this.streamComplete = streamComplete;
    }

    public boolean isHeaderless() {
        return headerless;
    }

    public void setHeaderless(boolean headerless) {
        this.headerless = headerless;
    }

    public int getStreamCRC() {
        return streamCRC;
    }

    public void setStreamCRC(int streamCRC) {
        this.streamCRC = streamCRC;
    }
}
