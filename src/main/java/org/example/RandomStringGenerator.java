package org.example;
import java.util.concurrent.ThreadLocalRandom;

public class RandomStringGenerator {

    private static final char[] ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                    .toCharArray();

    public static String randomString() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int len = rnd.nextInt(26, 101); // 26 to 100 inclusive
        char[] buf = new char[len];

        for (int i = 0; i < len; i++) {
            buf[i] = ALPHABET[rnd.nextInt(ALPHABET.length)];
        }

        return new String(buf);
    }
}
