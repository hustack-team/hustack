package com.hust.baseweb.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RandomGenerator {

    static String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";      // Excludes O, I to avoid confusion

    static String LOWER = "abcdefghjkmnpqrstuvwxyz";       // Excludes l, i to avoid confusion

    static String DIGITS = "23456789";                     // Excludes 0, 1 to avoid confusion

    static String SPECIAL = "!@#$%^&*()-_=+[]{}";          // Selected set of special characters

    static String ALL = UPPER + LOWER + DIGITS + SPECIAL;

    static String ALPHA_NUMERIC = UPPER + LOWER + DIGITS;

    static Integer MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING = 3;

    static Integer MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS = 4;

    static SecureRandom random = new SecureRandom();

    /**
     * Generates a random alphanumeric string of specified length.
     * <p>
     * The string will always include at least one uppercase letter, one lowercase letter, and one digit.
     * Ambiguous characters (e.g., O, 0, I, 1, l, i) are excluded to avoid confusion.
     * </p>
     *
     * @param length the total length of the string to generate; must be at least 3
     * @return a randomized alphanumeric string
     * @throws IllegalArgumentException if length is less than the minimum required
     */
    public static String generateAlphaNumericRandomString(int length) {
        if (length < MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING) {
            throw new IllegalArgumentException("Length must be at least " +
                                               MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING +
                                               " to include all character types");
        }

        StringBuilder sb = new StringBuilder(length);

        // Ensure at least one character from each character set
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));

        // Fill the remaining length with random characters from alpha numeric sets
        for (int i = MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING; i < length; i++) {
            sb.append(ALPHA_NUMERIC.charAt(random.nextInt(ALPHA_NUMERIC.length())));
        }

        // Shuffle characters to avoid fixed positions
        List<Character> pwdChars = sb.chars()
                                     .mapToObj(c -> (char) c)
                                     .collect(Collectors.toList());
        Collections.shuffle(pwdChars);

        return pwdChars.stream()
                       .map(String::valueOf)
                       .collect(Collectors.joining());
    }

    /**
     * Generates a random string of specified length including special characters.
     * <p>
     * The string will always include at least one uppercase letter, one lowercase letter,
     * one digit, and one special character. Ambiguous characters are excluded.
     * </p>
     *
     * @param length the total length of the string to generate; must be at least 4
     * @return a randomized string with special characters
     * @throws IllegalArgumentException if length is less than the minimum required
     */
    public static String generateRandomStringWithSpecialChars(int length) {
        if (length < MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS) {
            throw new IllegalArgumentException("Length must be at least " +
                                               MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS +
                                               " to include all character types");
        }

        StringBuilder sb = new StringBuilder(length);

        // Ensure at least one character from each character set
        sb.append(UPPER.charAt(random.nextInt(UPPER.length())));
        sb.append(LOWER.charAt(random.nextInt(LOWER.length())));
        sb.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        sb.append(SPECIAL.charAt(random.nextInt(SPECIAL.length())));

        // Fill the remaining length with random characters from all sets
        for (int i = MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS; i < length; i++) {
            sb.append(ALL.charAt(random.nextInt(ALL.length())));
        }

        // Shuffle characters to avoid fixed positions
        List<Character> pwdChars = sb.chars()
                                     .mapToObj(c -> (char) c)
                                     .collect(Collectors.toList());
        Collections.shuffle(pwdChars);

        return pwdChars.stream()
                       .map(String::valueOf)
                       .collect(Collectors.joining());
    }

    /**
     * Generates a set of unique alphanumeric random strings.
     * <p>
     * Each string will contain at least one uppercase letter, one lowercase letter, and one digit.
     * The result contains only unique values.
     * </p>
     *
     * @param count  the number of unique strings to generate
     * @param length the length of each string; must be at least 3
     * @return a set containing {@code count} unique alphanumeric strings
     * @throws IllegalArgumentException if length is less than the minimum required
     */
    public static Set<String> generateUniqueAlphaNumericRandomStrings(int count, int length) {
        if (length < MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING) {
            throw new IllegalArgumentException("Length must be at least " +
                                               MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING +
                                               " to include all character types");
        }
        Set<String> passwords = new HashSet<>();
        while (passwords.size() < count) {
            passwords.add(generateAlphaNumericRandomString(length));
        }
        return passwords;
    }

    /**
     * Generates a set of unique random strings that include special characters.
     * <p>
     * Each string will contain at least one uppercase letter, one lowercase letter,
     * one digit, and one special character. The result contains only unique values.
     * </p>
     *
     * @param count  the number of unique strings to generate
     * @param length the length of each string; must be at least 4
     * @return a set containing {@code count} unique strings with special characters
     * @throws IllegalArgumentException if length is less than the minimum required
     */
    public static Set<String> generateUniqueRandomStringsWithSpecialChars(int count, int length) {
        if (length < MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS) {
            throw new IllegalArgumentException("Length must be at least " +
                                               MIN_LENGTH_RANDOM_STRING_WITH_SPECIAL_CHARS +
                                               " to include all character types");
        }
        Set<String> passwords = new HashSet<>();
        while (passwords.size() < count) {
            passwords.add(generateRandomStringWithSpecialChars(length));
        }
        return passwords;
    }
}

