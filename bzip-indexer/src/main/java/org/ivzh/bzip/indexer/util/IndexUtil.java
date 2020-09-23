package org.ivzh.bzip.indexer.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ivzh.bzip.indexer.dto.Block;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;

public class IndexUtil {


    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();



    public static String blocks2json(List<Block> blocks) {
        return gson.toJson(blocks);
    }

    public static void string2file(String s, String filePath) throws IOException {
        Files.write(Paths.get(filePath), Collections.singletonList(s),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private IndexUtil() {}
}
