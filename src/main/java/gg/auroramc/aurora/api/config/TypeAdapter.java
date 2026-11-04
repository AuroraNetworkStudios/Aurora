package gg.auroramc.aurora.api.config;

/**
 * Reads a type that isn't a number, text, enum, list, map or config object from a YAML value, see
 * {@link ConfigLoader#registerAdapter}.
 */
@FunctionalInterface
public interface TypeAdapter<T> {
    /**
     * @param raw the YAML value: text, a number, true/false, a list or a section
     * @return the value, or null if it can't be read (it's reported and the default is used)
     */
    T read(Object raw);
}
