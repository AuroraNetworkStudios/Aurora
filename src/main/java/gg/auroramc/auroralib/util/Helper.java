package gg.auroramc.auroralib.util;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Helper {

    /**
     * Converts a List to an array of the same type.
     *
     * @param value The List to convert.
     * @param array An array of the desired type and size.
     * @param <T>   The generic type parameter.
     * @return An array containing the elements of the List.
     */
    public static <T> T[] toOtherList(List<T> value, T[] array) {
        // If the provided array is too small, create a new array of the appropriate size
        if(array.length < value.size()) {
            return value.toArray(Arrays.copyOf(array, value.size()));
        } else {
            return value.toArray(array);
        }
    }

    /**
     * Converts an array to a List of the same type.
     *
     * @param value The array to convert.
     * @param <T>   The generic type parameter.
     * @return A List containing the elements of the array.
     */
    public static <T> List<T> toOtherList(T[] value) {
        // Create a new ArrayList and pass the Arrays.asList() result as the constructor argument
        // This will create a new modifiable List containing the elements of the array
        // if only Arrays.asList() is present the size is fixed and might throw UnsupportedOperationException
        return new ArrayList<>(Arrays.asList(value));
    }

    /**
     * Returns a random element from the given list.
     *
     * @param list the list from which to retrieve a random element
     * @param <T> the type of elements in the list
     * @return a random element from the list
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static <T> T randomFromList(List<T> list) throws IllegalArgumentException {
        if(list == null || list.isEmpty())
            throw new IllegalArgumentException("List cannot be null or empty!");

        return list.get(new Random().nextInt(list.size()));
    }

    /**
     * Reverses the elements in the given list in-place.
     *
     * @param list the list to be reversed
     * @param <T> the type of elements in the list
     * @return the reversed list
     * @throws IllegalArgumentException if the list is null
     */
    public static <T> List<T> reverseList(List<T> list) throws IllegalArgumentException {
        if(list == null)
            throw new IllegalArgumentException("List cannot be null!");

        int start = 0;
        int end = list.size() - 1;

        while( start < end ) {
            T temp = list.get(start);
            list.set(start, list.get(end));
            list.set(end, temp);
            start++;
            end--;
        }
        return list;
    }

    /**
     * Checks if the given string is a palindrome.
     *
     * @param str the string to be checked
     * @return true if the string is a palindrome, false otherwise
     */
    public static boolean isPalindrome(String str) {
        if(str == null) return false;
        int start = 0;
        int end = str.length() - 1;

        while (start < end) {
            if(str.charAt(start) != str.charAt(end)) return false;
            start++;
            end--;
        }
        return true;
    }

    /**
     * Generates a random integer between the specified minimum and maximum values (inclusive).
     *
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return a random integer between min and max
     */
    public static int randomInt(int min, int max) {
        Random random = new Random();

        if(min > max) return random.nextInt((min - max) + 1) + max;
        if(min == max) return min;
        return random.nextInt((max - min) + 1) + min;
    }

    /**
     * Generates a random integer between 0 (inclusive) and the specified maximum value (inclusive).
     *
     * @param max the maximum value (inclusive)
     * @return a random integer between 0 and max
     */
    public static int randomInt(int max) {
        Random random = new Random();
        int min = 0;

        if(min > max) return random.nextInt((min - max) + 1) + max;
        if(min == max) return min;
        return random.nextInt((max - min) + 1) + min;
    }

    /**
     * Generates a random integer between 0 and 100 (inclusive).
     *
     * @return a random integer between 0 and 100
     */
    static int randomInt() {
        Random random = new Random();

        return random.nextInt((100) + 1);
    }

    /**
     * Checks if the elements in the given list are sorted in ascending order.
     *
     * @param list the list to check for sorted order
     * @param <T> the type of elements in the list
     * @return true if the list is sorted in ascending order, false otherwise
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static <T extends Comparable<T>> boolean isListSorted(List<T> list) throws IllegalArgumentException {
        if (list == null || list.size() <= 1) {
            return true;
        }
        for (int i = 1; i < list.size(); i++) {
            if (list.get(i).compareTo(list.get(i - 1)) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates and returns the average of the numbers in the given list.
     *
     * @param numbers the list of numbers
     * @return the average of the numbers
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static double getAverage(List<Double> numbers) throws IllegalArgumentException {
        if (numbers == null || numbers.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        double sum = 0;
        for (double number : numbers) {
            sum += number;
        }
        return sum / numbers.size();
    }

    /**
     * Counts the occurrences of the specified element in the given list.
     *
     * @param list the list in which to count occurrences
     * @param element the element to count occurrences of
     * @param <T> the type of elements in the list
     * @return the number of occurrences of the element in the list
     * @throws IllegalArgumentException if the list is null
     */
    public static <T> int countOccurrences(List<T> list, T element) throws IllegalArgumentException {
        if (list == null) {
            throw new IllegalArgumentException("List cannot be null");
        }
        int count = 0;
        for (T item : list) {
            if (item.equals(element)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns the maximum element from the given list.
     *
     * @param list the list from which to find the maximum element
     * @param <T> the type of elements in the list
     * @return the maximum element from the list
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static <T extends Comparable<T>> T findMax(List<T> list) throws IllegalArgumentException {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        T max = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T current = list.get(i);
            if (current.compareTo(max) > 0) {
                max = current;
            }
        }
        return max;
    }

    /**
     * Returns the minimum element from the given list.
     *
     * @param list the list from which to find the minimum element
     * @param <T> the type of elements in the list
     * @return the minimum element from the list
     * @throws IllegalArgumentException if the list is null or empty
     */
    public static <T extends Comparable<T>> T findMin(List<T> list) throws IllegalArgumentException {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }
        T min = list.get(0);
        for (int i = 1; i < list.size(); i++) {
            T current = list.get(i);
            if (current.compareTo(min) < 0) {
                min = current;
            }
        }
        return min;
    }

    /**
     * Removes duplicate elements from the given list while preserving the order.
     *
     * @param list the list from which to remove duplicate elements
     * @param <T> the type of elements in the list
     * @return a new list with duplicate elements removed
     * @throws IllegalArgumentException if the list is null
     */
    public static <T> List<T> removeDuplicates(List<T> list) throws IllegalArgumentException {
        if (list == null) {
            throw new IllegalArgumentException("List cannot be null");
        }
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    /**
     * Checks if the given string contains any special characters.
     * Special characters include any non-alphanumeric characters.
     *
     * @param str the string to check for special characters
     * @return true if the string contains special characters, false otherwise
     * @throws IllegalArgumentException if the string is null
     */
    public static boolean containsSpecialCharacters(String str) throws IllegalArgumentException {
        if (str == null) {
            throw new IllegalArgumentException("String cannot be null");
        }
        return !str.matches("[a-zA-Z0-9]+");
    }

    /**
     * Replaces all occurrences of the specified characters in the strings of the given list.
     *
     * @param input the list of strings to perform replacement on
     * @param chars the characters to replace
     * @return a new list with all occurrences of the specified characters replaced
     * @throws IllegalArgumentException if the input list is null or if any of the strings in the list are null
     */
    public static List<String> replaceCharacters(List<String> input, String... chars) throws IllegalArgumentException {
        if (input == null) {
            throw new IllegalArgumentException("Input list cannot be null");
        }
        List<String> result = new ArrayList<>();
        for (String str : input) {
            if (str == null) {
                throw new IllegalArgumentException("Strings in the input list cannot be null");
            }
            for (String c : chars) {
                str = str.replace(c, "");
            }
            result.add(str);
        }
        return result;
    }

    /**
     * Formats a date or timestamp to a specific string format.
     *
     * @param date the date or timestamp to format
     * @param format the pattern to format the date or timestamp to
     * @return the formatted date or timestamp as a string
     */
    public static String formatDate(LocalDate date, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return date.format(formatter);
    }

    /**
     * Calculates the difference between two dates in days.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the number of days between the two dates
     */
    public static long calculateDateDifferenceInDays(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Recursively traverses a directory and retrieves a list of files.
     *
     * @param directory the directory to traverse
     * @return a list of files in the directory and its subdirectories
     */
    public static List<File> traverseDirectory(File directory) {
        List<File> fileList = new ArrayList<>();
        traverse(directory, fileList);
        return fileList;
    }

    /**
     * Recursively traverses a directory and adds files to the given list.
     *
     * @param directory the directory to traverse
     * @param fileList the list to which files are added
     */
    private static void traverse(File directory, List<File> fileList) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                traverse(file, fileList);
            }
        } else {
            fileList.add(directory);
        }
    }


    /**
     * Removes leading and trailing whitespace from a string.
     *
     * @param input the string to trim
     * @return the trimmed string
     */
    public static String trimString(String input) {
        return input.trim();
    }

    /**
     * Splits a string into a list of substrings based on a delimiter.
     *
     * @param input the string to split
     * @param delimiter the delimiter used to split the string
     * @return a list of substrings
     */
    public static List<String> splitString(String input, String delimiter) {
        String[] parts = input.split(delimiter);
        return Arrays.asList(parts);
    }

    /**
     * Calculates the factorial of a number.
     *
     * @param n the number to calculate the factorial for
     * @return the factorial of the number
     */
    public static int calculateFactorial(int n) {
        if (n == 0) {
            return 1;
        }
        return n * calculateFactorial(n - 1);
    }

    /**
     * Calculates the greatest common divisor (GCD) of two numbers.
     *
     * @param a the first number
     * @param b the second number
     * @return the GCD of the two numbers
     */
    public static int calculateGCD(int a, int b) {
        if (b == 0) {
            return a;
        }
        return calculateGCD(b, a % b);
    }

    /**
     * Converts a string to an integer.
     *
     * @param str the string to convert
     * @return the converted integer
     * @throws NumberFormatException if the string does not represent a valid integer
     */
    public static int convertStringToInt(String str) throws NumberFormatException {
        return Integer.parseInt(str);
    }

    /**
     * Converts a string representation of a date to a LocalDate object.
     *
     * @param dateString the string representation of the date
     * @param pattern the pattern used to parse the date string
     * @return the parsed LocalDate object
     * @throws IllegalArgumentException if the date string is not in the expected format
     */
    public static LocalDate parseDate(String dateString, String pattern) throws IllegalArgumentException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(dateString, formatter);
    }


}
