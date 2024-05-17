package gg.auroramc.aurora.expansions.numberformat;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.expansions.AuroraExpansion;
import gg.auroramc.aurora.api.util.NumberFormat;
import lombok.Getter;

@Getter
public class NumberFormatExpansion implements AuroraExpansion {
    private NumberFormat intFormat;
    private NumberFormat doubleFormat;

    @Override
    public void hook() {
        var config = Aurora.getLibConfig().getNumberFormat();
        intFormat = new NumberFormat(config.getLocale(), config.getIntFormat());
        doubleFormat = new NumberFormat(config.getLocale(), config.getDoubleFormat());
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
}
