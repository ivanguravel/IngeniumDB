package bzip2.block;

import bzip2.block.data.DataGenerator;
import com.bzip2.block.dto.BzipBlock;
import com.bzip2.block.Bzip2BlockInputStream;
import com.bzip2.block.Bzip2BlockOutputStream;
import com.bzip2.block.event.Bzip2BlockListener;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bzip2.block.data.DataGenerator.LINE_WIDTH;


public class BlockIoStreamTest {

    @Test
    public void testBlockSizes(@TempDir Path tempDir) throws IOException {
        Path data = tempDir.resolve("data.txt");

        String bzipPath = data + ".bz2";
        int blockSize = 2;
        int linesNum = 650;

        List<BzipBlock> bzipBlocks = new ArrayList<>();
        Bzip2BlockListener blockListener = bzipBlocks::add;

        String stringData = DataGenerator.generate(linesNum);
        Files.write(data, stringData.getBytes(StandardCharsets.UTF_8));

        try (FileInputStream input = new FileInputStream(data.toFile());
                Bzip2BlockOutputStream outputStream = new Bzip2BlockOutputStream(
                new FileOutputStream(bzipPath), blockSize, blockListener)) {
            IOUtils.copyLarge(input, outputStream);
        }

        Assertions.assertTrue(bzipBlocks.size() > 0, "No Bzip blocks were recorded");

        Map<Integer, Long> actFileSizes = extractBlocks(bzipBlocks, bzipPath, blockSize);
        checkSizes(bzipBlocks, actFileSizes);


        int totalSize = bzipBlocks.stream()
                .mapToInt(BzipBlock::getUncompressedDataSize)
                .sum();

        Assertions.assertEquals(linesNum * LINE_WIDTH, totalSize,
                "Total block size mismatch");

    }

    private Map<Integer, Long> extractBlocks(List<BzipBlock> bzipBlocks, String bzipPath, int blockSize)
            throws IOException {

        Map<Integer, Long> result = new HashMap<>();

        for (int i = 0; i < bzipBlocks.size(); i++) {
            BzipBlock block = bzipBlocks.get(i);
            String outPath = bzipPath.replace(".txt.bz2","_b_" + i + ".txt");

            try (Bzip2BlockInputStream stream = new Bzip2BlockInputStream(new FileInputStream(bzipPath), blockSize);
                 FileOutputStream output = new FileOutputStream(outPath)) {
                stream.skipToBlock(block);
                IOUtils.copyLarge(stream, output);
            }
            long size = Files.size(Paths.get(outPath));
            result.put(i, size);
        }
        return result;
    }

    private void checkSizes(List<BzipBlock> blocks, Map<Integer, Long> actFileSizes) {
        for (int i = 0; i < blocks.size(); i++) {
            BzipBlock currentBlock = blocks.get(i);

            Long actualSize;
            if (i == blocks.size() - 1) {
                actualSize = actFileSizes.get(i);
            } else {
                actualSize = actFileSizes.get(i) - actFileSizes.get(i + 1);
            }

            long diff = actualSize - currentBlock.getUncompressedDataSize();
            System.out.println(currentBlock + "\t\tActual File Size: " + actualSize + " \tDiff: " + diff);
        }
    }

}
