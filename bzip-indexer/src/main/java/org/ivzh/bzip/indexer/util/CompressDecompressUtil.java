package org.ivzh.bzip.indexer.util;

import org.itadaki.bzip2.BZip2InputStream;

import java.io.*;

public class CompressDecompressUtil {

    private CompressDecompressUtil() {}

    public static void decompress(String file, String to) throws IOException {
        File inputFile = new File (file);
        if (!inputFile.exists() || !inputFile.canRead() || !file.endsWith(".bz2")) {
            throw new IllegalArgumentException("can't work with file");
        }

        File outputFile = new File (to);

        InputStream fileInputStream = new BufferedInputStream(new FileInputStream(inputFile));
        BZip2InputStream inputStream = new BZip2InputStream (fileInputStream, false);
        OutputStream fileOutputStream = new BufferedOutputStream (new FileOutputStream (outputFile), 524288);

        byte[] decoded = new byte [524288];
        int bytesRead;
        while ((bytesRead = inputStream.read (decoded)) != -1) {
            fileOutputStream.write (decoded, 0, bytesRead) ;
        }
        fileOutputStream.close();
    }
}
