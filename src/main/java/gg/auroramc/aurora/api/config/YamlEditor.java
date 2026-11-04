package gg.auroramc.aurora.api.config;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Changes a single plain value in a YAML file by editing its line, so the rest of the file (comments, quotes, layout,
 * line endings) stays exactly as it is.
 */
final class YamlEditor {
    // Never matches a key, stands for lines whose key isn't understood (list items, odd keys), so what's under them
    // can't be taken for something under the line before
    private static final String UNKNOWN = "\u0000";

    private YamlEditor() {
    }

    /**
     * @return false if the key isn't a plain value on its own line (missing, a section, a list or a value continuing
     * on the next lines), nothing is written then
     */
    static boolean setScalar(File file, String key, Object value) throws IOException {
        if (!file.exists() || !(value == null || value instanceof String || value instanceof Number || value instanceof Boolean)) {
            return false;
        }
        var text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        var newline = text.contains("\r\n") ? "\r\n" : "\n";
        var lines = new ArrayList<>(List.of(text.split("\r?\n", -1)));
        var index = find(lines, key);
        if (index < 0) return false;
        var line = lines.get(index);
        var colon = keyEnd(line);
        var rest = line.substring(colon + 1);
        var split = splitComment(rest);
        var oldValue = split[0].strip();
        if (oldValue.isEmpty() || oldValue.startsWith("|") || oldValue.startsWith(">") || oldValue.startsWith("[") || oldValue.startsWith("{")
                || oldValue.startsWith("&") || oldValue.startsWith("*") || oldValue.startsWith("!") || split[2] != null) {
            return false;
        }
        // A plain value can continue on the next, deeper indented lines
        var indent = indentOf(line);
        for (int i = index + 1; i < lines.size(); i++) {
            var next = lines.get(i);
            if (next.isBlank() || next.strip().startsWith("#")) continue;
            if (indentOf(next) > indent) return false;
            break;
        }
        lines.set(index, line.substring(0, colon + 1) + " " + format(value, oldValue) + split[1]);
        Files.writeString(file.toPath(), String.join(newline, lines), StandardCharsets.UTF_8);
        return true;
    }

    // The line of the key, or -1
    private static int find(List<String> lines, String key) {
        var parts = key.split("\\.");
        var path = new ArrayList<String>();
        var indents = new ArrayList<Integer>();
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.equals("---")) continue;
            var indent = indentOf(line);
            while (!indents.isEmpty() && indents.getLast() >= indent) {
                indents.removeLast();
                path.removeLast();
            }
            var colon = trimmed.startsWith("-") || line.contains("\t") ? -1 : keyEnd(line);
            indents.add(indent);
            path.add(colon < 0 ? UNKNOWN : unquote(line.substring(indent, colon).strip()));
            if (colon >= 0 && path.size() == parts.length && String.join(".", path).equals(key)) return i;
        }
        return -1;
    }

    // The position of the colon ending the key: the first ": " or a colon at the end, outside quotes. -1 if none
    private static int keyEnd(String line) {
        char quote = 0;
        for (int i = indentOf(line); i < line.length(); i++) {
            var c = line.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if ((c == '"' || c == '\'') && i == indentOf(line)) {
                quote = c;
            } else if (c == '#' && i > 0 && line.charAt(i - 1) == ' ') {
                return -1;
            } else if (c == ':' && (i + 1 == line.length() || line.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return i;
    }

    private static String unquote(String key) {
        if (key.length() >= 2 && (key.startsWith("\"") && key.endsWith("\"") || key.startsWith("'") && key.endsWith("'"))) {
            return key.substring(1, key.length() - 1);
        }
        return key;
    }

    // [value, " # comment" or "", non-null if a quote isn't closed on the line]; a # inside quotes isn't a comment
    private static String[] splitComment(String rest) {
        char quote = 0;
        var trimmedStart = rest.length() - rest.stripLeading().length();
        for (int i = 0; i < rest.length(); i++) {
            var c = rest.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if ((c == '"' || c == '\'') && i == trimmedStart) {
                quote = c;
            } else if (c == '#' && (i == 0 || rest.charAt(i - 1) == ' ')) {
                var start = i;
                while (start > 0 && rest.charAt(start - 1) == ' ') start--;
                return new String[]{rest.substring(0, start), rest.substring(start), null};
            }
        }
        return new String[]{rest, "", quote != 0 ? "open" : null};
    }

    // In the quote style the old value used, quoted anyway if it would be read as something else
    private static String format(Object value, String oldValue) {
        if (value == null) return "null";
        if (!(value instanceof String text)) return String.valueOf(value);
        var multiLine = text.contains("\n") || text.contains("\r") || text.contains("\t");
        if (oldValue.startsWith("'") && !multiLine) return "'" + text.replace("'", "''") + "'";
        if (oldValue.startsWith("\"") || needsQuotes(text) || multiLine) {
            return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
        }
        return text;
    }

    private static boolean needsQuotes(String text) {
        if (text.isEmpty() || !text.equals(text.strip())) return true;
        // Anything YAML wouldn't read back as this exact text (dates, 0x1F, .inf, ...)
        try {
            var yaml = new YamlConfiguration();
            yaml.loadFromString("k: " + text);
            if (!(yaml.get("k") instanceof String read) || !read.equals(text)) return true;
        } catch (Exception e) {
            return true;
        }
        if (text.matches("(?i)true|false|yes|no|on|off|y|n|null|~") || text.matches("[-+]?[0-9._]+(e[-+]?[0-9]+)?")) return true;
        return Pattern.compile("[:#\\[\\]{},&*!|>'\"%@`]").matcher(text).find() || text.startsWith("-") || text.startsWith("?");
    }
}
