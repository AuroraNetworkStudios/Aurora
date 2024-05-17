package gg.auroramc.aurora.config;

import lombok.Getter;

@Getter
public class NumberFormatConfig {
    private String locale = "en-US";
    private String intFormat = "#,###";
    private String doubleFormat = "#,##0.00";
}
