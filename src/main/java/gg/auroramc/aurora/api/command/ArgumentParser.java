package gg.auroramc.aurora.api.command;

import java.util.HashMap;
import java.util.Map;

public class ArgumentParser {
    public static Map<String, String> parseString(String input) {
        Map<String, String> resultMap = new HashMap<>();

        // Find where the first key-value pair starts
        int firstKeyStart = input.indexOf('=');
        if (firstKeyStart == -1) {
            // No key-value pairs found, return the whole string as prefix
            resultMap.put("prefix", input.trim());
            return resultMap;
        }

        // We need to backtrack to find the start of the first key (just before the first '=' sign)
        int prefixEnd = input.lastIndexOf(' ', firstKeyStart);
        if (prefixEnd == -1) {
            prefixEnd = 0;
        }

        // Extract the prefix (text before the first key-value pair)
        String prefix = input.substring(0, prefixEnd).trim();
        resultMap.put("prefix", prefix);

        // Now, extract the remaining part of the string which contains the key-value pairs
        String keyValuePart = input.substring(prefixEnd).trim();
        int i = 0;
        int length = keyValuePart.length();

        while (i < length) {
            // Step 1: Extract the key
            StringBuilder keyBuilder = new StringBuilder();
            while (i < length && keyValuePart.charAt(i) != '=') {
                keyBuilder.append(keyValuePart.charAt(i));
                i++;
            }

            // Move past '='
            i++;

            // Step 2: Extract the value inside curly braces
            if (i < length && keyValuePart.charAt(i) == '{') {
                i++; // Move past '{'
                StringBuilder valueBuilder = new StringBuilder();
                while (i < length && keyValuePart.charAt(i) != '}') {
                    valueBuilder.append(keyValuePart.charAt(i));
                    i++;
                }

                // Move past '}'
                i++;

                // Step 3: Put key-value pair in the map
                resultMap.put(keyBuilder.toString().trim(), valueBuilder.toString());
            }

            // Move past space after the closing curly brace
            while (i < length && keyValuePart.charAt(i) == ' ') {
                i++;
            }
        }

        return resultMap;
    }
}
