package bzip2.block.data;

import java.util.Random;

/**
 * A provider of randomized {@link java.lang.String} values.
 */
public class RandomString {

    /**
     * The symbols which are used to create a random {@link java.lang.String}.
     */
    private static final char[] SYMBOL;

    /*
     * Creates the symbol array.
     */
    static {
        StringBuilder symbol = new StringBuilder();
        for (char character = '0'; character <= '9'; character++) {
            symbol.append(character);
        }
        for (char character = 'a'; character <= 'z'; character++) {
            symbol.append(character);
        }
        for (char character = 'A'; character <= 'Z'; character++) {
            symbol.append(character);
        }
        SYMBOL = symbol.toString().toCharArray();
    }

    /**
     * A provider of random values.
     */
    private final Random random;

    /**
     * The length of the random strings that are created by this instance.
     */
    private final int length;

    /**
     * Creates a random {@link java.lang.String} provider where each value is of the given length.
     *
     * @param length The length of the random {@link String}.
     */
    public RandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("A random string's length cannot be zero or negative");
        }
        this.length = length;
        random = new Random();
    }

    /**
     * Creates a random {@link java.lang.String} of the given {@code length}.
     *
     * @param length The length of the random {@link String}.
     * @return A random {@link java.lang.String}.
     */
    public static String make(int length) {
        return new RandomString(length).nextString();
    }

    /**
     * Creates a new random {@link java.lang.String}.
     *
     * @return A random {@link java.lang.String} of the given length for this instance.
     */
    public String nextString() {
        char[] buffer = new char[length];
        for (int index = 0; index < length; index++) {
            buffer[index] = SYMBOL[random.nextInt(SYMBOL.length)];
        }
        return new String(buffer);
    }
}
