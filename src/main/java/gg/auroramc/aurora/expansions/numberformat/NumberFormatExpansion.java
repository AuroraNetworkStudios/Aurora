package gg.auroramc.aurora.expansions.numberformat;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.util.NumberFormat;
import lombok.Getter;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NumberFormatExpansion implements AuroraExpansion {
    private final Map<Locale, NumberFormat> intFormats = new ConcurrentHashMap<>();
    private final Map<Locale, NumberFormat> doubleFormats = new ConcurrentHashMap<>();
    private final Map<Locale, NumberFormat> shortFormats = new ConcurrentHashMap<>();

    @Override
    public void hook() {
        var config = Aurora.getLibConfig().getNumberFormat();
        intFormats.put(Locale.ROOT, new NumberFormat(config.getLocale(), config.getIntFormat()));
        doubleFormats.put(Locale.ROOT, new NumberFormat(config.getLocale(), config.getDoubleFormat()));
        shortFormats.put(Locale.ROOT, new NumberFormat(config.getLocale(), config.getShortNumberFormat().getFormat()));
    }

    @Override
    public boolean canHook() {
        return true;
    }

    public String formatWholeNumber(long number) {
        return formatWholeNumber(Locale.ROOT, number);
    }

    public String formatDecimalNumber(double number) {
        return formatDecimalNumber(Locale.ROOT, number);
    }

    public String formatNumberShort(double number) {
        return formatNumberShort(Locale.ROOT, number);
    }

    public String formatNumberShort(Locale locale, double number) {
        return formatWithSuffix(locale, number);
    }

    public String formatWholeNumber(Locale locale, long number) {
        if (intFormats.containsKey(locale)) {
            return intFormats.get(locale).format(number);
        } else {
            var config = Aurora.getLibConfig().getNumberFormat();
            var formatter = new NumberFormat(locale.toString().replace("_", "-"), config.getIntFormat());
            intFormats.put(locale, formatter);
            return formatter.format(number);
        }
    }

    public String formatDecimalNumber(Locale locale, double number) {
        if (doubleFormats.containsKey(locale)) {
            return doubleFormats.get(locale).format(number);
        } else {
            var config = Aurora.getLibConfig().getNumberFormat();
            var formatter = new NumberFormat(locale.toString().replace("_", "-"), config.getDoubleFormat());
            doubleFormats.put(locale, formatter);
            return formatter.format(number);
        }
    }

    private String formatWithSuffix(Locale locale, double number) {
        var suffixes = Aurora.getLibConfig().getNumberFormat().getShortNumberFormat().getSuffixes();
        NumberFormat shortFormat;

        if (shortFormats.containsKey(locale)) {
            shortFormat = shortFormats.get(locale);
        } else {
            var config = Aurora.getLibConfig().getNumberFormat();
            var formatter = new NumberFormat(locale.toString().replace("_", "-"), config.getShortNumberFormat().getFormat());
            shortFormats.put(locale, formatter);
            shortFormat = formatter;
        }


        if (number < 1_000) {
            return shortFormat.format(number);
        } else if (number < 1_000_000) {
            return shortFormat.format(number / 1_000) + suffixes.get("thousand");
        } else if (number < 1_000_000_000) {
            return shortFormat.format(number / 1_000_000) + suffixes.get("million");
        } else if (number < 1_000_000_000_000L) {
            return shortFormat.format(number / 1_000_000_000) + suffixes.get("billion");
        } else if (number < 1_000_000_000_000_000L) {
            return shortFormat.format(number / 1_000_000_000_000L) + suffixes.get("trillion");
        } else {
            return shortFormat.format(number / 1_000_000_000_000_000L) + suffixes.get("quadrillion");
        }
    }
}
