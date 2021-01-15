package bzip2.block;

import bzip2.block.data.DataGenerator;
import com.bzip2.block.dto.BzipBlock;
import com.bzip2.block.Bzip2BlockInputStream;
import com.bzip2.block.Bzip2BlockOutputStream;
import com.bzip2.block.event.Bzip2BlockListener;
import com.bzip2.block.util.BlockOffsetFinder;
import com.bzip2.block.util.BlockOffsetFinder.BlockFindResult;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RandomAccessTest {

    @Test
    public void randomAccessTest(@TempDir Path tempDir) throws IOException {
        Path data = tempDir.resolve("some_data.txt");

        String bzipPath = data + ".bz2";
        int blockSize = 1;
        int linesNum = 540;

        String stringData = DataGenerator.generate(linesNum);
        Files.write(data, stringData.getBytes(StandardCharsets.UTF_8));

        List<BzipBlock> bzipBlocks = new ArrayList<>();
        Bzip2BlockListener blockListener = bzipBlocks::add;

        try (FileInputStream input = new FileInputStream(data.toFile());
             Bzip2BlockOutputStream outputStream = new Bzip2BlockOutputStream(
                     new FileOutputStream(bzipPath), blockSize, blockListener)) {
            IOUtils.copyLarge(input, outputStream);
        }

        long dataOffset = 493250;
        int amount = 512;

        String expectedFilePart = readFilePart(data, dataOffset, amount);

        Optional<BlockFindResult> maybeFindResult = BlockOffsetFinder.findBlock(bzipBlocks, dataOffset);
        Assertions.assertTrue(maybeFindResult.isPresent());
        BlockFindResult findResult = maybeFindResult.get();

        try (Bzip2BlockInputStream inputStream = new Bzip2BlockInputStream(new FileInputStream(bzipPath), blockSize);
             BoundedInputStream boundedStream = new BoundedInputStream(inputStream, amount);
             ByteArrayOutputStream out = new ByteArrayOutputStream(amount)) {
            inputStream.skipToBlock(findResult.getBlock());
            long skipped = inputStream.skip(findResult.getDataSkipAmount());
            Assertions.assertEquals(findResult.getDataSkipAmount(), skipped);

            IOUtils.copy(boundedStream, out);

            String actualFilePart = out.toString("UTF-8");

            Assertions.assertEquals(expectedFilePart, actualFilePart);
        }
    }

    private String readFilePart(Path data, long offset, int amount) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(data.toFile(), "r")) {
            byte[] toRead = new byte[amount];
            file.seek(offset);
            int readAmount = file.read(toRead);
            Assertions.assertEquals(amount, readAmount);
            return new String(toRead, StandardCharsets.UTF_8);
        }
    }
}
