package org.ivzh.bzip.indexer.dto;

import java.io.IOException;

public class StreamBlockInput extends StreamBlock {



    public int pos = 0;
    public byte readByte() throws IOException {
        this.pos++;
        return (byte) randomAccessFile.read();
    }
    public void seek (int pos) throws IOException {
        this.pos = pos;
        randomAccessFile.seek(pos);
    }
    public boolean eof() throws IOException { return this.pos >= randomAccessFile.length(); }


}
