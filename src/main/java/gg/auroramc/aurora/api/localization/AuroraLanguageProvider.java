package gg.auroramc.aurora.api.localization;

import gg.auroramc.aurora.Aurora;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

@Setter
@Getter
public class AuroraLanguageProvider implements LanguageProvider {

    private List<Locale> supportedLocales = List.of(Locale.ENGLISH);
    private Locale fallbackLocale = Locale.ENGLISH;

    @Override
    public Locale getPlayerLocale(Player player) {
        var user = Aurora.getUserManager().getUser(player);
        if (!user.isLoaded()) return fallbackLocale;
        return user.getLocalizationData().getLocalization();
    }

    @Override
    public void setPlayerLocale(Player player, Locale locale) {
        var user = Aurora.getUserManager().getUser(player);
        user.getLocalizationData().setLocalization(locale);
    }

}
