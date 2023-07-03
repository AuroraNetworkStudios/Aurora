package gg.auroramc.auroralib.message;

public class Placeholder {
    private final String key;
    private final String value;

    // constructors
    public Placeholder(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public Placeholder(String key, double value) {
        this(key, numberFormat(value));
    }
    public Placeholder(String key, long value) {
        this(key, numberFormat(value));
    }
    public Placeholder(String key, float value) {
        this(key, numberFormat(value));
    }

    /**
     * Formats a number as a String.
     * If the number is a whole number, it is formatted without decimal places.
     * If the number has decimal places, it is formatted with the appropriate decimal places.
     *
     * @param num The number to format.
     * @return The formatted number as a String.
     */
    private static String numberFormat(double num) {
        if (num == Math.floor(num)) {
            return String.format("%d", (long) num);
        } else {
            return String.format("%s", num);
        }
    }

    /**
     * Replaces occurrences of a specific key string with the corresponding value string in the given text.
     *
     * @param text The text in which replacements will be made.
     * @return The modified text with replacements.
     */
    public String replace(String text) {
        return text.replace(key, value);
    }

    /**
     * Checks if the given text contains the specified key string.
     *
     * @param text The text to check for the presence of the key.
     * @return True if the key is found in the text, false otherwise.
     */
    public Boolean has(String text) {
        return text.contains(key);
    }

    /**
     * Checks if the given text contains the specified key string, ignoring the case.
     *
     * @param text The text to check for the presence of the key.
     * @return True if the key is found in the text (ignoring case), false otherwise.
     */
    public Boolean hasIgnoreCase(String text) {
        return text.toLowerCase().contains(key.toLowerCase());
    }

    /**
     * Removes occurrences of the specified key string from the given text.
     *
     * @param text The text from which the key occurrences will be removed.
     * @return The modified text with the key occurrences removed.
     */
    public String remove(String text) {
        return text.replaceAll(key, "");
    }

    /**
     * Removes occurrences of the specified key string from the given text, ignoring the case.
     *
     * @param text The text from which the key occurrences will be removed (ignoring case).
     * @return The modified text with the key occurrences removed (ignoring case).
     */
    public String removeIgnoreCase(String text) {
        return text.toLowerCase().replaceAll(key.toLowerCase(), "");
    }

    /**
     * Creates a new Placeholder instance with a given value.
     *
     * @param key   The key for the placeholder.
     * @param value The String value for the placeholder.
     * @return The created Placeholder instance.
     */
    public static Placeholder of(String key, String value) {
        return new Placeholder(key ,value);
    }

    /**
     * Creates a new Placeholder instance with a given value.
     *
     * @param key   The key for the placeholder.
     * @param value The value for the placeholder.
     * @return The created Placeholder instance.
     */
    public static Placeholder of(String key, Double value) {
        return new Placeholder(key ,value);
    }

    /**
     * Creates a new Placeholder instance with a given value.
     *
     * @param key   The key for the placeholder.
     * @param value The value for the placeholder.
     * @return The created Placeholder instance.
     */
    public static Placeholder of(String key, Float value) {
        return new Placeholder(key ,value);
    }

    /**
     * Creates a new Placeholder instance with a given value.
     *
     * @param key   The key for the placeholder.
     * @param value The value for the placeholder.
     * @return The created Placeholder instance.
     */
    public static Placeholder of(String key, Long value) {
        return new Placeholder(key ,value);
    }

    /**
     * Executes placeholder replacement in the given text using the provided Placeholder objects.
     *
     * @param text        The text in which placeholder replacement will be performed.
     * @param placeholders The Placeholder objects representing the placeholders to be replaced.
     * @return The text with the placeholder replacements applied.
     */
    public static String execute(String text, Placeholder... placeholders) {
        if(placeholders == null) return text;
        if(placeholders.length < 1) return text;

        for(Placeholder placeholder : placeholders) {
            text = placeholder.replace(text);
        }
        return text;
    }

    public String getKey() {
        return key;
    }
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Placeholder{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
