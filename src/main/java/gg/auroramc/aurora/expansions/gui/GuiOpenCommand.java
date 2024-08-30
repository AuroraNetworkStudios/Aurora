package gg.auroramc.aurora.expansions.gui;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GuiOpenCommand extends Command {
    private final Gui gui;

    public GuiOpenCommand(String name, Gui gui) {
        super(name);
        this.gui = gui;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(!(sender instanceof Player)) {
            return true;
        }

        gui.open((Player) sender);

        return true;
    }
}
