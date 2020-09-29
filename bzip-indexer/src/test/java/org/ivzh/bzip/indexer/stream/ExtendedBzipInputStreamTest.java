package org.ivzh.bzip.indexer.stream;

import org.ivzh.bzip.indexer.dto.Block;
import org.ivzh.bzip.indexer.util.CompressDecompressUtil;
import org.ivzh.bzip.indexer.util.IndexUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExtendedBzipInputStreamTest {

    @Test
    public void generateFile() throws IOException {
        List<Block> blocks = new ExtendedBzipInputStream("src/main/java/org/ivzh/bzip/indexer/stream/8381.tar.bz2", "r").indexBzip();
        String json = IndexUtil.blocks2json(blocks);
        IndexUtil.string2file(json, "test.json");
        Assert.assertNotNull(blocks);
        Assert.assertEquals(10, blocks.size());
    }

    @Test
    public void testDecompressor() throws IOException {
        String from = "src/main/java/org/ivzh/bzip/indexer/stream/8381.tar.bz2";
        String to = "src/main/java/org/ivzh/bzip/indexer/stream/8381.tar";
        CompressDecompressUtil.decompress(from, to);
        File outputFile = new File (to);
        Assert.assertTrue(outputFile.exists());
        outputFile.delete();
    }
}
