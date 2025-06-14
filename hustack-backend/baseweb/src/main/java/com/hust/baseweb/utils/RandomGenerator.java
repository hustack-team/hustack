package com.hust.baseweb.utils;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.security.SecureRandom;
import java.util.*;
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
     * Generates a random alphanumeric string of the specified length based on the given character group policy.
     * <p>
     * Depending on the {@code charGroupPolicy}, the generated string will contain at least one character from each
     * required character group. Ensures that:
     * <ul>
     *   <li>{@link CharGroupPolicy#ONLY_UPPER}: contains at least one uppercase letter and one digit.</li>
     *   <li>{@link CharGroupPolicy#ONLY_LOWER}: contains at least one lowercase letter and one digit.</li>
     *   <li>{@link CharGroupPolicy#MIXED} (or any other): contains at least one uppercase letter, one lowercase letter, and one digit.</li>
     * </ul>
     * The rest of the characters are randomly chosen from the allowed set according to the policy,
     * and the final string is shuffled to randomize character positions.
     *
     * @param length          the total length of the generated string. Must be at least the minimum required
     *                        to satisfy the selected {@code charGroupPolicy}, otherwise an {@link IllegalArgumentException} is thrown.
     * @param charGroupPolicy the character group policy that determines which character sets must be included.
     * @return a randomized alphanumeric string of the given length satisfying the character group policy.
     * @throws IllegalArgumentException if {@code length} is less than the minimum required for the given {@code charGroupPolicy}.
     */
    public static String generateAlphaNumericRandomString(int length, CharGroupPolicy charGroupPolicy) {
        int minLength = getMinLengthForMode(charGroupPolicy);
        if (length < minLength) {
            throw new IllegalArgumentException(
                String.format(
                    "Provided length (%d) is too short for policy %s. Minimum required is %d to include all required character types",
                    length, charGroupPolicy, minLength)
            );
        }

        StringBuilder sb = new StringBuilder(length);
        String allowedChars;
        List<Character> required = new ArrayList<>();

        switch (charGroupPolicy) {
            case ONLY_UPPER:
                allowedChars = UPPER + DIGITS;
                required.add(UPPER.charAt(random.nextInt(UPPER.length())));
                required.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
                break;

            case ONLY_LOWER:
                allowedChars = LOWER + DIGITS;
                required.add(LOWER.charAt(random.nextInt(LOWER.length())));
                required.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
                break;
            case MIXED:
            default:
                allowedChars = ALPHA_NUMERIC;
                required.add(UPPER.charAt(random.nextInt(UPPER.length())));
                required.add(LOWER.charAt(random.nextInt(LOWER.length())));
                required.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
                break;
        }

        // Ensure at least one character from each character set
        for (char c : required) {
            sb.append(c);
        }

        // Fill the remaining length with random characters from allowed sets
        for (int i = required.size(); i < length; i++) {
            sb.append(allowedChars.charAt(random.nextInt(allowedChars.length())));
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
    public static Set<String> generateUniqueAlphaNumericRandomStrings(int count, int length, CharGroupPolicy mode) {
        Set<String> passwords = new HashSet<>();
        while (passwords.size() < count) {
            passwords.add(generateAlphaNumericRandomString(length, mode));
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
        Set<String> passwords = new HashSet<>();
        while (passwords.size() < count) {
            passwords.add(generateRandomStringWithSpecialChars(length));
        }
        return passwords;
    }

    private static int getMinLengthForMode(CharGroupPolicy mode) {
        return switch (mode) {
            case ONLY_UPPER, ONLY_LOWER -> 2; // 1 letter + 1 digit
            default -> MIN_LENGTH_ALPHA_NUMERIC_RANDOM_STRING; // 1 upper + 1 lower + 1 digit
        };
    }
}

