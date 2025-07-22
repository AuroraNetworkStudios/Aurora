package gg.auroramc.aurora.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Chat;
import org.bukkit.entity.Player;

import java.util.Locale;

@CommandAlias("language")
public class LanguageCommand extends BaseCommand {

    @Default()
    @CommandCompletion("@languages @nothing")
    @CommandPermission("aurora.core.user.language")
    public void onLanguageChange(Player player, String language) {
        if (language == null || language.isEmpty()) return;
        var provider = Aurora.getLanguageProvider();

        var oldMessages = Aurora.getMsg(player);

        try {
            var locale = Locale.forLanguageTag(language);
            if (!provider.getSupportedLocales().contains(locale)) {
                Chat.sendMessage(player, oldMessages.getLocaleNotSupported());
                return;
            }
            provider.setPlayerLocale(player, locale);
        } catch (Exception e) {
            Chat.sendMessage(player, oldMessages.getLocaleInvalid());
        }


        var newMessages = Aurora.getMsg(player);
        Chat.sendMessage(player, newMessages.getLocaleChanged());
    }
}
