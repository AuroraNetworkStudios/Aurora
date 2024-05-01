package gg.auroramc.auroralib.api.message;

import gg.auroramc.auroralib.api.util.NumberSupplier;
import gg.auroramc.auroralib.api.util.StringSupplier;
import lombok.Getter;

import java.util.List;
import java.util.function.Supplier;

public class Placeholder<T> {
    @Getter
    private final String key;
    private final Supplier<T> valueSupplier;

    private Placeholder(String key, T value) {
        this(key, () -> value);
    }

    private Placeholder(String key, Supplier<T> supplier) {
        this.key = key;
        this.valueSupplier = supplier;
    }

    /**
     * Replaces occurrences of a specific key string with the corresponding value string in the given text.
     *
     * @param text The text in which replacements will be made.
     * @return The modified text with replacements.
     */
    public String replace(String text) {
        var value = this.valueSupplier.get();
        if(value instanceof String val) {
            return text.replace(key, val);
        } else if(value instanceof Number val) {
            return text.replace(key, String.valueOf(val));
        }
        return text.replace(key, valueSupplier.get().toString());
    }

    public static Placeholder<String> of(String key, String value) {
        return new Placeholder<>(key, value);
    }

    public static Placeholder<Number> of(String key, Number value) {
        return new Placeholder<>(key, value);
    }

    public static Placeholder<String> of(String key, StringSupplier supplier) {
        return new Placeholder<>(key, supplier);
    }

    public static Placeholder<Number> of(String key, NumberSupplier supplier) {
        return new Placeholder<>(key, supplier);
    }


    /**
     * Executes placeholder replacement in the given text using the provided Placeholder objects.
     *
     * @param text        The text in which placeholder replacement will be performed.
     * @param placeholders The Placeholder objects representing the placeholders to be replaced.
     * @return The text with the placeholder replacements applied.
     */
    public static String execute(String text, Placeholder<?>... placeholders) {
        if(placeholders == null) return text;
        if(placeholders.length < 1) return text;

        for(Placeholder<?> placeholder : placeholders) {
            text = placeholder.replace(text);
        }
        return text;
    }

    /**
     * Executes placeholder replacement in the given text using the provided Placeholder objects.
     *
     * @param text        The text in which placeholder replacement will be performed.
     * @param placeholders The Placeholder objects representing the placeholders to be replaced.
     * @return The text with the placeholder replacements applied.
     */
    public static String execute(String text, List<Placeholder<?>> placeholders) {
        if(placeholders == null) return text;
        if(placeholders.isEmpty()) return text;

        for(Placeholder<?> placeholder : placeholders) {
            text = placeholder.replace(text);
        }
        return text;
    }

    public T getValue() {
        return valueSupplier.get();
    }

    @Override
    public String toString() {
        return "Placeholder{" +
                "key='" + key + '\'' +
                ", value='" + valueSupplier.get() + '\'' +
                '}';
    }
}
