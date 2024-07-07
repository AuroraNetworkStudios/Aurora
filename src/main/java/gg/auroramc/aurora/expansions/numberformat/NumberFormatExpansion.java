package gg.auroramc.aurora.expansions.numberformat;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.util.NumberFormat;
import lombok.Getter;

@Getter
public class NumberFormatExpansion implements AuroraExpansion {
    private NumberFormat intFormat;
    private NumberFormat doubleFormat;
    private NumberFormat shortFormat;

    @Override
    public void hook() {
        var config = Aurora.getLibConfig().getNumberFormat();
        intFormat = new NumberFormat(config.getLocale(), config.getIntFormat());
        doubleFormat = new NumberFormat(config.getLocale(), config.getDoubleFormat());
        shortFormat = new NumberFormat(config.getLocale(), config.getShortNumberFormat().getFormat());
    }

    @Override
    public boolean canHook() {
        return true;
    }

    public String formatWholeNumber(long number) {
        return intFormat.format(number);
    }

    public String formatDecimalNumber(double number) {
        return doubleFormat.format(number);
    }

    public String formatNumberShort(double number) {
        return formatWithSuffix(number);
    }

    private String formatWithSuffix(double number) {
        var suffixes = Aurora.getLibConfig().getNumberFormat().getShortNumberFormat().getSuffixes();

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
