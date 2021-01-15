package bzip2.block.data;


public class DataGenerator {
    public static final int LINE_WIDTH = 1000;

    public static String generate(int linesNum) {
        StringBuilder result = new StringBuilder(linesNum * LINE_WIDTH);
        for (int i = 0; i < linesNum; i++) {
            appendLine(result, i);
        }
        return result.toString();
    }

    private static void appendLine(StringBuilder result, int index) {
        String stringIndex = String.valueOf(index);
        result.append(stringIndex).append(' ');

        String addedText;

        if (index % 77 == 0) {
            addedText = charMultiply('x', 280);
        } else {
            addedText = charMultiply('a', 40);
        }

        result.append(addedText);
        result.append(RandomString.make(LINE_WIDTH - stringIndex.length() - 2 - addedText.length()));
        result.append("\n");
    }

    private static String charMultiply(char c, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            result.append(c);
        }
        return result.toString();
    }
}
