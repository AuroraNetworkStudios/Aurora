package gg.auroramc.aurora.commands;

import co.aikar.commands.MessageKeys;
import co.aikar.commands.MinecraftMessageKeys;
import co.aikar.commands.PaperCommandManager;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.message.Chat;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.expansions.leaderboard.LeaderboardExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class CommandManager {
    private final Aurora plugin;
    private final PaperCommandManager commandManager;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.legacyAmpersand().toBuilder()
            .hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    private boolean hasSetup = false;

    public CommandManager(Aurora plugin) {
        this.plugin = plugin;
        this.commandManager = new PaperCommandManager(plugin);
    }

    private void setupCommands() {
        if (!this.hasSetup) {
            commandManager.getLocales().setDefaultLocale(Locale.ENGLISH);
            commandManager.usePerIssuerLocale(false);

            commandManager.getCommandCompletions().registerCompletion("commandActions",
                    (c) -> CommandDispatcher.getActions().stream().map((a) -> "[" + a + "]").toList());
            commandManager.getCommandCompletions().registerCompletion("guiIds",
                    (c) -> Aurora.getExpansionManager().getExpansion(GuiExpansion.class).getGuiIds());
            commandManager.getCommandCompletions().registerCompletion("userMetaKeys",
                    (c) -> Aurora.getUserManager().getUser(c.getContextValue(Player.class)).getMetaData().getMetaKeys());
            commandManager.getCommandCompletions().registerCompletion("userMetaNumberKeys",
                    (c) -> Aurora.getUserManager().getUser(c.getContextValue(Player.class)).getMetaData().getMetaKeys()
                            .stream().filter((k) -> Aurora.getUserManager().getUser(c.getContextValue(Player.class)).getMetaData().getMeta(k) instanceof Number).toList());
            commandManager.getCommandCompletions().registerCompletion("leaderboards",
                    (c) -> Aurora.getExpansionManager().getExpansion(LeaderboardExpansion.class).getBoards());
            commandManager.getCommandCompletions().registerCompletion("languages",
                    (c) -> Aurora.getLanguageProvider().getSupportedLocales().stream().map(Locale::toLanguageTag).toList());
        }

        var msg = Aurora.getMessageConfigs().get(Locale.forLanguageTag(Aurora.getLibConfig().getFallbackLocale()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND_OFFLINE, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.NO_PLAYER_FOUND_SERVER, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.IS_NOT_A_VALID_NAME, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.COULD_NOT_FIND_PLAYER, m(msg.getPlayerNotFound()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED, m(msg.getNoPermission()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.PERMISSION_DENIED_PARAMETER, m(msg.getNoPermission()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.INVALID_SYNTAX, m(msg.getInvalidSyntax()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.MUST_BE_A_NUMBER, m(msg.getMustBeNumber()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.ERROR_PERFORMING_COMMAND, m(msg.getCommandError()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.ERROR_GENERIC_LOGGED, m(msg.getCommandError()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.NOT_ALLOWED_ON_CONSOLE, m(msg.getPlayerOnlyCommand()));
        commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.UNKNOWN_COMMAND, m(msg.getUnknownCommand()));

        if (!this.hasSetup) {
            this.commandManager.registerCommand(new AuroraCommand(plugin));
            this.commandManager.registerCommand(new LanguageCommand());
            this.hasSetup = true;
        }
    }

    public void reload() {
        this.setupCommands();
    }

    private String a(List<String> aliases) {
        return String.join("|", aliases);
    }

    private String m(String msg) {
        return serializer.serialize(Text.component(Chat.translateToMM(msg)));
    }

    public void unregisterCommands() {
        this.commandManager.unregisterCommands();
    }

    public PaperCommandManager getPaperCommandManager() {
        return this.commandManager;
    }
}
