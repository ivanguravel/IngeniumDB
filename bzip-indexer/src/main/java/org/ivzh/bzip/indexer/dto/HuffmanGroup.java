package org.ivzh.bzip.indexer.dto;

import static org.ivzh.bzip.indexer.stream.ExtendedBzipInputStream.*;

public class HuffmanGroup {

    int[] permute = new int[MAX_SYMBOLS];
    int[] limit = new int[MAX_HUFCODE_BITS + 2];
    int[] base = new int[MAX_HUFCODE_BITS + 1];
    int minLen;
    int maxLen;

    public HuffmanGroup(int minLen, int maxLen) {
        this.minLen = minLen;
        this.maxLen = maxLen;
    }


    public int[] getPermute() {
        return permute;
    }

    public void setPermute(int[] permute) {
        this.permute = permute;
    }

    public int[] getLimit() {
        return limit;
    }

    public void setLimit(int[] limit) {
        this.limit = limit;
    }

    public int[] getBase() {
        return base;
    }

    public void setBase(int[] base) {
        this.base = base;
    }

    public int getMinLen() {
        return minLen;
    }

    public void setMinLen(int minLen) {
        this.minLen = minLen;
    }

    public int getMaxLen() {
        return maxLen;
    }

    public void setMaxLen(int maxLen) {
        this.maxLen = maxLen;
    }
}
