package org.ivzh.bzip.indexer.dto;

public class StreamBlockOutput extends StreamBlock {

    public void writeByte(int outbyte) {
        this.pos++;
    }
}
