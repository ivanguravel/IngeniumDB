package org.ivzh.bzip.indexer.stream;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class IndexedFileStream extends InputStream {

    static Collection<IndexedFileStream> allInstances =
            Collections.synchronizedCollection(new HashSet<IndexedFileStream>());

    File file;
    RandomAccessFile fis;

    public IndexedFileStream(final File file) throws FileNotFoundException {
        this.file = file;
        fis = new RandomAccessFile(file, "r");
        allInstances.add(this);
    }

    public long length() {
        return file.length();
    }

    public boolean eof() throws IOException {
        return fis.length() == fis.getFilePointer();
    }

    public void seek(final long position) throws IOException {
        fis.seek(position);
    }

    public long position() throws IOException {
        return fis.getChannel().position();
    }

    @Override
    public long skip(long n) throws IOException {
        long initPos = position();
        fis.getChannel().position(initPos + n);
        return position() - initPos;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (length < 0) {
            throw new IndexOutOfBoundsException();
        }
        int n = 0;
        while (n < length) {
            final int count = fis.read(buffer, offset + n, length - n);
            if (count < 0) {
                if (n > 0) {
                    return n;
                } else {
                    return count;
                }
            }
            n += count;
        }
        return n;

    }

    @Override
    public int read() throws IOException {
        return fis.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return fis.read(b);
    }

    public String getSource() {
        return file.getAbsolutePath();
    }


    @Override
    public void close() throws IOException {
        allInstances.remove(this);
        fis.close();

    }

    public static synchronized void closeAllInstances() {
        Collection<IndexedFileStream> clonedInstances = new HashSet<IndexedFileStream>();
        clonedInstances.addAll(allInstances);
        for (IndexedFileStream sfs : clonedInstances) {
            try {
                sfs.close();
            } catch (IOException e) {
                //TODO
                //log.error("Error closing SeekableFileStream", e);
            }
        }
        allInstances.clear();
    }
}
