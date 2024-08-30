package gg.auroramc.aurora.expansions.gui;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.menu.AuroraMenu;
import gg.auroramc.aurora.api.menu.ItemBuilder;
import gg.auroramc.aurora.api.menu.MenuAction;
import gg.auroramc.aurora.api.menu.Requirement;
import gg.auroramc.aurora.api.message.Placeholder;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class Gui implements AuroraGui {
    private Command command;
    private final GuiConfig config;

    public Gui(GuiConfig config) {
        this.config = config;
        if (config.getRegisterCommand() != null) {
            this.command = new GuiOpenCommand(config.getRegisterCommand(), this);
            Aurora.getInstance().getServer().getCommandMap().register("aurora", this.command);
        }
    }

    @Override
    public void dispose() {
        if (command != null) command.unregister(Aurora.getInstance().getServer().getCommandMap());
    }

    @Override
    public void open(Player player) {
        if (config.getOpenRequirements() != null) {
            if (!Requirement.passes(player, config.getOpenRequirements())) {
                return;
            }
        }

        var menu = new AuroraMenu(player, config.getTitle(), config.getRows() * 9, config.isUpdate(), Placeholder.of("{player}", player.getName()));

        if (config.getFiller() != null) {
            menu.addFiller(ItemBuilder.of(config.getFiller()).toItemStack(player));
        } else {
            menu.addFiller(ItemBuilder.filler(Material.AIR));
        }

        config.getItems().forEach((id, itemConfig) ->
                menu.addItem(ItemBuilder.of(itemConfig).build(player), (e) -> {
                    if (itemConfig.getOnClick() != null && !itemConfig.getOnClick().isEmpty() && !itemConfig.getOnClick().contains("[close]")) {
                        return MenuAction.REFRESH_MENU;
                    }
                    if (e.isLeftClick() && itemConfig.getOnLeftClick() != null && !itemConfig.getOnLeftClick().isEmpty() && !itemConfig.getOnLeftClick().contains("[close]")) {
                        return MenuAction.REFRESH_MENU;
                    }
                    if (e.isRightClick() && itemConfig.getOnRightClick() != null && !itemConfig.getOnRightClick().isEmpty() && !itemConfig.getOnRightClick().contains("[close]")) {
                        return MenuAction.REFRESH_MENU;
                    }
                    return MenuAction.NONE;
                }));

        if (config.getCloseActions() != null) {
            menu.onClose((m, e) -> {
                for (var cmd : config.getCloseActions()) {
                    CommandDispatcher.dispatch(player, cmd, Placeholder.of("{player}", player.getName()));
                }
            });
        }

        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            menu.open(player, false);
            if (config.getOpenActions() != null) {
                for (var cmd : config.getOpenActions()) {
                    CommandDispatcher.dispatch(player, cmd, Placeholder.of("{player}", player.getName()));
                }
            }
        }, null);
    }
}
