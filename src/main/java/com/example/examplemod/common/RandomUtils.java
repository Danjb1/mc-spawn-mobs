package com.example.examplemod.common;

import java.util.Random;

/**
 * Utility methods for generating random numbers.
 *
 * @author Dan Quill
 */
public final class RandomUtils {

    private RandomUtils() {}

    /**
     * Multiplies the given value by either -1 or 1 at random.
     *
     * @param i
     * @return
     */
    public static float randomSign(float i) {
        return i * (Math.random() < 0.5 ? -1 : 1);
    }

    /**
     * Multiplies the given value by either -1 or 1 at random, using the given
     * random number generator.
     *
     * @param i
     * @param random
     * @return
     */
    public static float randomSign(float i, Random random) {
        return i * (random.nextDouble() < 0.5 ? -1 : 1);
    }

    /**
     * Returns a random double between the 2 limits.
     *
     * @param min
     * @param max
     * @return
     */
    public static double randBetween(double min, double max) {
        return min + (Math.random() * (max - min));
    }

    /**
     * Returns a random double between the 2 limits, using the given random
     * number generator.
     *
     * @param min
     * @param max
     * @param random
     * @return
     */
    public static double randBetween(double min, double max, Random random) {
        return min + (random.nextDouble() * (max - min));
    }

    /**
     * Returns a random int between the 2 limits (inclusive).
     *
     * @param min
     * @param max
     * @return
     */
    public static int randBetween(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    /**
     * Returns a random int between the 2 limits, using the given random number
     * generator.
     *
     * @param min
     * @param max
     * @param random
     * @return
     */
    public static int randBetween(int min, int max, Random random) {
        return min + (int) (random.nextDouble() * ((max - min) + 1));
    }

}
