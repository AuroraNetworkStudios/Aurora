package gg.auroramc.aurora.api;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Chat;
import org.bukkit.Bukkit;

import java.util.function.Supplier;

public class AuroraLogger {
    private String prefix = Chat.translateToMM("&#1286b8Aurora &8&l| ");
    private Supplier<Boolean> debug = () -> Aurora.getLibConfig().getDebug();

    public AuroraLogger() {
    }

    public AuroraLogger(String prefix, Supplier<Boolean> debug) {
        this.prefix = Chat.translateToMM(String.format("&#1286b8%s &8&l| ", prefix));
        this.debug = debug;
    }

    public void info(String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + "<gray>" + msg);
    }

    public void severe(String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + "<red>" + msg);
    }

    public void warning(String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + "<yellow>" + msg);
    }

    public void info(String module, String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + module + " - <gray>" + msg);
    }

    public void severe(String module, String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + module + " - <red>" + msg);
    }

    public void warning(String module, String msg) {
        Chat.sendMessage(Bukkit.getConsoleSender(), prefix + module + " - <yellow>" + msg);
    }

    public void debug(String msg) {
        if(debug.get()) {
            Chat.sendMessage(Bukkit.getConsoleSender(), prefix + "<aqua>" + msg);
        }
    }
}
