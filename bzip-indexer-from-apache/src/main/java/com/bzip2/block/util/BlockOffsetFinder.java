package com.bzip2.block.util;

import com.bzip2.block.dto.BzipBlock;
import java.util.Collection;
import java.util.Optional;

public final class BlockOffsetFinder {
    private BlockOffsetFinder() {
    }


    public static Optional<BlockFindResult> findBlock(Collection<BzipBlock> blocks, long desiredOffset) {
        Optional<BlockFindResult> result = Optional.empty();
        long startOffset = 0;

        for (BzipBlock block : blocks) {
            long endOffset = startOffset + block.getUncompressedDataSize();

            if (desiredOffset >= startOffset && desiredOffset < endOffset) {
                long offsetInsideBlock = desiredOffset - startOffset;
                result = Optional.of(new BlockFindResult(block, offsetInsideBlock));
                break;
            }
            startOffset += block.getUncompressedDataSize();
        }
        return result;
    }

    public static class BlockFindResult {

        private final BzipBlock block;
        private final long dataSkipAmount;

        private BlockFindResult(BzipBlock block, long dataSkipAmount) {
            this.block = block;
            this.dataSkipAmount = dataSkipAmount;
        }

        public BzipBlock getBlock() {
            return block;
        }

        public long getDataSkipAmount() {
            return dataSkipAmount;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("BlockFindResult{");
            sb.append("block=").append(block);
            sb.append(", dataSkipAmount=").append(dataSkipAmount);
            sb.append('}');
            return sb.toString();
        }
    }

}
