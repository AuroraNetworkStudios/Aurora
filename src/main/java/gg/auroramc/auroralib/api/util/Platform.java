package gg.auroramc.auroralib.api.util;

import gg.auroramc.auroralib.AuroraLib;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class Platform {
    @Getter
    private static final boolean paper = checkForPaper();

    private static final LegacyComponentSerializer legacyComponentSerializer =
            LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private static boolean checkForPaper() {
        try {
            Class.forName("com.destroystokyo.paper.Namespaced");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static void setItemName(ItemMeta meta, Component name) {
        if (paper) {
            meta.displayName(name);
        } else {
            meta.setDisplayName(legacyComponentSerializer.serialize(name));
        }
    }

    public static void setItemLore(ItemMeta meta, List<Component> lore) {
        if (paper) {
            meta.lore(lore);
        } else {
            meta.setLore(lore.stream().map(legacyComponentSerializer::serialize).toList());
        }
    }

    public static void sendMessage(Player player, Component message) {
        if (isPaper()) {
            player.sendMessage(message);
        } else {
            AuroraLib.getAdventure().player(player).sendMessage(message);
        }
    }

    public static void sendMessage(CommandSender sender, Component message) {
        if (isPaper()) {
            sender.sendMessage(message);
        } else {
            AuroraLib.getAdventure().sender(sender).sendMessage(message);
        }
    }

    public static void sendActionBar(Player player, Component message) {
        if (isPaper()) {
            player.sendActionBar(message);
        } else {
            AuroraLib.getAdventure().player(player).sendActionBar(message);
        }
    }

    public static Inventory createChestInventory(InventoryHolder holder, int size, Component title) {
        if(isPaper()) {
            return Bukkit.createInventory(holder, size, title);
        } else {
            return Bukkit.createInventory(holder, size, legacyComponentSerializer.serialize(title));
        }
    }
}
