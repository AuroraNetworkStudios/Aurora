package gg.auroramc.auroralib.expansions.numberformat;

import gg.auroramc.auroralib.AuroraLib;
import gg.auroramc.auroralib.api.expansions.AuroraExpansion;
import gg.auroramc.auroralib.api.util.NumberFormat;
import lombok.Getter;

@Getter
public class NumberFormatExpansion implements AuroraExpansion {
    private NumberFormat intFormat;
    private NumberFormat doubleFormat;

    @Override
    public void hook() {
        var config = AuroraLib.getLibConfig().getNumberFormat();
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
