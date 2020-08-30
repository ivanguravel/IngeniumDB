package org.forfree.ingenium.server.randomfilegenerator;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;
import java.util.UUID;

public class RandomFileGenerator {


    // generates 2 gb file
    public static void main(String[] args) {
        byte[] buffer = UUID.randomUUID().toString().getBytes();
        int number_of_lines = 2_000_000_000;


        ByteBuffer wrBuf = null;
        FileChannel rwChannel = null;
        try {
            rwChannel = new RandomAccessFile("D:\\testfile.txt", "rw").getChannel();
            wrBuf = rwChannel.map(FileChannel.MapMode.READ_WRITE, 0, number_of_lines);
            for (int i = 0; i < number_of_lines; i++) {
                wrBuf.put(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(rwChannel)) {
                try {
                    rwChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
