package org.ivzh.bzip.indexer;


import org.ivzh.bzip.indexer.stream.ExtendedBzipInputStream;

import java.io.IOException;

public class App {

    public static void main(String[] args) throws IOException {

        new ExtendedBzipInputStream("D:\\projects\\IngeniumDB\\bzip-indexer\\src\\main\\java\\org\\ivzh\\bzip\\indexer\\stream\\8381.tar.bz2", "r").indexBzip();


    }


}
