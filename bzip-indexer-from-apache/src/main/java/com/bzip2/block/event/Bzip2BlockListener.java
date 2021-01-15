package com.bzip2.block.event;

import com.bzip2.block.dto.BzipBlock;

import java.util.EventListener;

public interface Bzip2BlockListener extends EventListener {
    void blockWritten(BzipBlock block);
}
