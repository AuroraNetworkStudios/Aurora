package gg.auroramc.auroralib.api.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberFormat {
    private final DecimalFormat decimalFormat;

    public NumberFormat(String locale, String pattern) {
        this.decimalFormat = new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.forLanguageTag(locale)));
    }

    public String format(double number) {
        return decimalFormat.format(number).replace('\u00A0', ' ');
    }

    public String format(long number) {
        return decimalFormat.format(number).replace('\u00A0', ' ');
    }
}
