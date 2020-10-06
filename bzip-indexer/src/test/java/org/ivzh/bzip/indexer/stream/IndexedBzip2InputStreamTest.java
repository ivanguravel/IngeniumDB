package org.ivzh.bzip.indexer.stream;

import org.ivzh.bzip.indexer.dto.Block;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

public class IndexedBzip2InputStreamTest {

    @Test
    public void testBz2WithOneBlock() throws IOException {
        String from = "src/main/java/org/ivzh/bzip/indexer/examples/8381.tar.bz2";
        String to = "src/main/java/org/ivzh/bzip/indexer/stream/8381.tar";

        List<Block> index = indexBzip2(from, to);
        assertNotNull(index);

        assertFalse(index.isEmpty());
        assertEquals(1, index.size());
        Block block = index.get(0);
        assertEquals(32, block.getBlockOffset());
        assertEquals(4096, block.getBlockLength());
    }

    @Test
    public void testBz2WithMultipleBlocks() throws IOException {
        String from = "src/main/java/org/ivzh/bzip/indexer/examples/sample4.bz2";
        String to = "src/main/java/org/ivzh/bzip/indexer/stream/sample4";

        List<Block> index = indexBzip2(from, to);
        assertNotNull(index);

        assertFalse(index.isEmpty());
        assertEquals(10, index.size());
        Block block = index.get(0);
        assertEquals(32, block.getBlockOffset());
        assertEquals(99981, block.getBlockLength());

        LinkedList<Block> linkedList = (LinkedList<Block>) index;
        assertEquals(39019, linkedList.getLast().getBlockLength());
    }


    private List<Block> indexBzip2(String file, String to) throws IOException {
        IndexedBzip2InputStream indexedBzip2InputStream = decompress(file, to);
        File outputFile = new File (to);
        Assert.assertTrue(outputFile.exists());
        outputFile.delete();

        return indexedBzip2InputStream.getIndex();
    }


    private static IndexedBzip2InputStream decompress(String file, String to) throws IOException {
        File inputFile = new File (file);
        if (!inputFile.exists() || !inputFile.canRead() || !file.endsWith(".bz2")) {
            throw new IllegalArgumentException("can't work with file");
        }

        File outputFile = new File (to);

        int buffValue = Integer.MAX_VALUE -100;

        InputStream fileInputStream = new BufferedInputStream(new FileInputStream(inputFile));
        IndexedBzip2InputStream inputStream = new IndexedBzip2InputStream(fileInputStream, false);
        OutputStream fileOutputStream = new BufferedOutputStream (new FileOutputStream (outputFile), buffValue);

        byte[] decoded = new byte [buffValue];
        int bytesRead;
        while ((bytesRead = inputStream.read (decoded)) != -1) {
            fileOutputStream.write (decoded, 0, bytesRead) ;
        }
        fileOutputStream.close();

        return inputStream;
    }
}
