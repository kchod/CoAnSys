/*
 * This file is part of CoAnSys project.
 * Copyright (c) 2012-2015 ICM-UW
 * 
 * CoAnSys is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * CoAnSys is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with CoAnSys. If not, see <http://www.gnu.org/licenses/>.
 */
package pl.edu.icm.coansys.commons.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 *
 * @author Łukasz Dumiszewski
 *
 */
public final class StringTools {

    private static final List<String> partNames = Arrays.asList("PART", "CZESC", "CZ");
    private static final String PART_NAME = "part";
    private static final Map<String, String> wordToDecimal = Maps.newHashMap();
    private static final Map<Character, String> greekLetters = Maps.newHashMap();

    static {
        wordToDecimal.put("ONE", "1");
        wordToDecimal.put("TWO", "2");
        wordToDecimal.put("THREE", "3");
        wordToDecimal.put("FOUR", "4");
        wordToDecimal.put("FIVE", "5");
        wordToDecimal.put("SIX", "6");
        wordToDecimal.put("SEVEN", "7");
        wordToDecimal.put("EIGHT", "8");
        wordToDecimal.put("NINE", "9");
        wordToDecimal.put("TEN", "10");

        greekLetters.put('α', "alpha");
        greekLetters.put('β', "beta");
        greekLetters.put('γ', "gamma");
        greekLetters.put('δ', "delta");
        greekLetters.put('ε', "epsilon");
        greekLetters.put('κ', "kappa");
        greekLetters.put('λ', "lambda");
        greekLetters.put('σ', "sigma");
        greekLetters.put('π', "pi");
    }

    public static String normalizePartQualifiers(String str) {
        String[] tokens = str.split(" ");
        List<String> newTokens = new ArrayList<String>();

        boolean recentlyPartName = false;
        
        for (int i = 0; i < tokens.length; i++) {
            if (partNames.contains(tokens[i].toUpperCase())) {
                newTokens.add(PART_NAME);
                recentlyPartName = true;
            } else if (!recentlyPartName && isDecimalNumber(tokens[i])) {
                newTokens.add(PART_NAME);
                newTokens.add(tokens[i]);
                recentlyPartName = false;
            } else {
                newTokens.add(tokens[i]);
                recentlyPartName = false;
            }
        }
        return StringUtils.join(newTokens, " ");
    }

    private StringTools() {
        throw new IllegalStateException();
    }

    /**
     * Returns the trailing integer from the given string or null if the string
     * does not end with number Example: Alice has got a cat 12 - will return 12
     * (the position of '1') Alice has got a black cat - will return null (no
     * trailing number in the string)
     */
    public static String getTrailingInteger(String str) {
        int positionOfTrailingInteger = getPositionOfTrailingInteger(str);
        if (positionOfTrailingInteger == -1) {
            // string does not end in digits
            return null;
        }
        return str.substring(positionOfTrailingInteger);
    }

    /**
     * Returns the position of the first digit in the trailing number of the
     * given string or -1 if the string does not end with number Example: Alice
     * has got a cat 12 - will return 20 (the position of '1') Alice has got a
     * black cat - will return -1 (no trailing number in the string)
     */
    public static int getPositionOfTrailingInteger(String str) {
        int pos;
        for (pos = str.length() - 1; pos >= 0; pos--) {
            char c = str.charAt(pos);
            if (!Character.isDigit(c)) {
                break;
            }
        }

        if (pos == str.length() - 1) {
            return -1;
        }

        return pos + 1;
    }

    /**
     * XIV - true, MC - true, Mc - true, MRA - false
     */
    public static boolean isRomanNumber(String value) {
        return value.toUpperCase().matches("^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$");
    }
    
    public static boolean isDecimalNumber(String value) {
        return value.matches("^\\d+$");
    }

    /**
     * Converts roman number to decimal.
     *
     * @throws IllegalArgumentException if the number is not a valid roman number, see:
     * {@link StringTools#isRomanNumber(String)}
     */
    public static int romanToDecimal(String romanNumber) {
        Preconditions.checkArgument(isRomanNumber(romanNumber));

        int decimal = 0;
        int lastNumber = 0;
        String romanNumeral = romanNumber.toUpperCase();
        /* operation to be performed on upper cases even if user enters roman values in lower case chars */
        for (int x = romanNumeral.length() - 1; x >= 0; x--) {
            char convertToDecimal = romanNumeral.charAt(x);

            switch (convertToDecimal) {
                case 'M':
                    decimal = processDecimal(1000, lastNumber, decimal);
                    lastNumber = 1000;
                    break;

                case 'D':
                    decimal = processDecimal(500, lastNumber, decimal);
                    lastNumber = 500;
                    break;

                case 'C':
                    decimal = processDecimal(100, lastNumber, decimal);
                    lastNumber = 100;
                    break;

                case 'L':
                    decimal = processDecimal(50, lastNumber, decimal);
                    lastNumber = 50;
                    break;

                case 'X':
                    decimal = processDecimal(10, lastNumber, decimal);
                    lastNumber = 10;
                    break;

                case 'V':
                    decimal = processDecimal(5, lastNumber, decimal);
                    lastNumber = 5;
                    break;

                case 'I':
                    decimal = processDecimal(1, lastNumber, decimal);
                    lastNumber = 1;
                    break;
                default:
                    break;
            }
        }
        return decimal;
    }

    /**
     * If the trailing part of the value is roman number then replaces it with
     * decimal number and returns the changed value, otherwise returns the
     * passed value
     */
    public static String replaceLastRomanNumberToDecimal(String value) {
        if (value == null || !value.contains(" ")) {
            return value;
        }
        String number = value.substring(value.lastIndexOf(' ')).trim();
        if (isRomanNumber(number)) {
            int decimalNumber = romanToDecimal(number);
            return value.substring(0, value.lastIndexOf(' ') + 1) + decimalNumber;
        }

        return value;
    }

    /**
     * If the trailing part of the value is a string denoting number (one, two,
     * three... ten) then it is replaced with an appropriate number
     */
    public static String replaceLastWordNumberToDecimal(String value) {

        if (value == null || !value.contains(" ")) {
            return value;
        }
        String number = value.substring(value.lastIndexOf(' ')).trim().toUpperCase();
        if (isEngWordNumber(number)) {
            return value.substring(0, value.lastIndexOf(' ') + 1) + wordToDecimal.get(number);
        }

        return value;
    }

    public static String replaceNumbersToDecimal(String value) {
        if (value == null) {
            return value;
        }

        String[] tokens = value.split(" ");
        String[] newTokens = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (isRomanNumber(token)) {
                newTokens[i] = String.valueOf(romanToDecimal(token));
            } else if (isEngWordNumber(token)) {
                newTokens[i] = String.valueOf(wordToDecimal.get(token.toUpperCase()));
            } else {
                newTokens[i] = token;
            }
        }
        return StringUtils.join(newTokens, " ");
    }

    public static boolean isEngWordNumber(String value) {
        return value.toUpperCase().matches("ONE|TWO|THREE|FOUR|FIVE|SIX|SEVEN|EIGHT|NINE|TEN");
    }

    /**
     * Normalizes the given value. The normalized strings are better suited for
     * not strict comparisons, in which we don't care about characters that are
     * not letters or digits, about accidental spaces, or about different
     * diacritics etc. <br/><br/>
     * This method: <br/>
     * - Replaces some greek letters by theirs "word" equivalend (eg. alpha, beta) <br/>
     * - Replaces all characters that are not letters, digits by spaces<br/>
     * - Replaces white spaces with spaces <br/>
     * - Trims <br />
     * - Compacts many-spaces gaps to one-space gaps <br/>
     * - Removes diacritics <br/>
     * - Lower cases <br/>
     *
     * Returns null if the value is null
     *
     * @see DiacriticsRemover#removeDiacritics(String, boolean)
     *
     *
     */
    public static String normalize(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);
            if (greekLetters.keySet().contains(c)) {
                sb.append(greekLetters.get(c));
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else {
                sb.append(" ");
            }
        }
        String result = sb.toString();
        result = DiacriticsRemover.removeDiacritics(result);
        result = removeStopWords(result);
        result = result.toLowerCase();
        result = result.trim().replaceAll(" +", " ");
        return result;
    }

    /**
     * Removes stop words <br/>
     * The comparison of ... -> comparison ... <br/><br/>
     *
     * Stop words supported so far: <br/>
     * the, a, an, of, and, or
     *
     * The white spaces between the stop words and other words are compacted to
     * one space<br/>
     */
    public static String removeStopWords(final String value) {
        String result = value.replaceAll("^([T|t][H|h][E|e]\\s+)|\\s+[T|t][H|h][E|e]\\s+", " ");
        result = result.replaceAll("^([O|o][F|f]\\s+)|\\s+[O|o][F|f]\\s+", " ");
        result = result.replaceAll("^[a|A]\\s+|\\s+[a|A]\\s+", " ");
        result = result.replaceAll("^([A|a][N|n]\\s+)|\\s+[A|a][N|n]\\s+", " ");
        result = result.replaceAll("^([A|a][N|n][D|d]\\s+)|\\s+[A|a][N|n][D|d]\\s+", " ");
        result = result.replaceAll("^([O|o][R|r]\\s+)|\\s+[O|o][R|r]\\s+", " ");
        return result;

    }

    /**
     * Is the levenshtein distance of the two strings < maxDistance?
     */
    public static boolean inLevenshteinDistance(String title1, String title2, int maxDistance) {
        int distance = org.apache.commons.lang.StringUtils.getLevenshteinDistance(title1, title2);
        if (distance > maxDistance) {
            return false;
        }
        return true;
    }

    /**
     * Counts digits to all chars in string ratio
     */
    public static int digitsPercentage(String s) {
        int allChars = s.length();
        int digits = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                digits++;
            }
        }
        return digits * 100 / allChars;
    }

    //******************** PRIVATE ********************
    private static int processDecimal(int decimal, int lastNumber, int lastDecimal) {
        if (lastNumber > decimal) {
            return lastDecimal - decimal;
        } else {
            return lastDecimal + decimal;
        }
    }
}
