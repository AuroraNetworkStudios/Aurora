package gg.auroramc.aurora.config;

import lombok.Getter;

import java.util.Map;

@Getter
public class ShortNumberFormatConfig {
    private String format = "#,###";
    private Map<String, String> suffixes = Map.of("thousand", "K", "million", "M", "billion", "B", "trillion", "T", "quadrillion", "Q");
}