package gg.auroramc.aurora.api.localization;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public interface LanguageProvider {
    Locale getPlayerLocale(Player player);

    void setPlayerLocale(Player player, Locale locale);

    Locale getFallbackLocale();

    void setFallbackLocale(Locale locale);

    List<Locale> getSupportedLocales();

    void setSupportedLocales(List<Locale> locales);
}
