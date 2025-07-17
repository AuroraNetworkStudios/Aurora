package gg.auroramc.aurora.hooks;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.item.TypeId;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent;
import io.lumine.mythic.bukkit.utils.numbers.Numbers;
import io.lumine.mythic.core.drops.droppables.ItemDrop;
import io.lumine.mythic.paper.adapters.item.ItemComponentPaperItemStack;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class MythicMobsHook implements Listener {

    public static void hook() {
        Bukkit.getPluginManager().registerEvents(new MythicMobsHook(), Aurora.getInstance());
    }

    @EventHandler
    public void onDropLoad(MythicDropLoadEvent event) {
        if (!event.getDropName().equals("auroralib")) return;

        String itemId = event.getConfig().getString("item");
        if (itemId == null) return;

        if (itemId.startsWith("\"") && itemId.endsWith("\"")) {
            itemId = itemId.substring(1, itemId.length() - 1);
        }

        ItemStack item = AuroraAPI.getItemManager().resolveItem(TypeId.fromString(itemId));
        if (item == null) return;

        event.register(new AuroraItemDrop(event.getContainer().getLine(), item, event.getConfig()));
    }

    public static class AuroraItemDrop extends ItemDrop implements IItemDrop {
        private final ItemComponentPaperItemStack item;

        public AuroraItemDrop(String line, ItemStack item, MythicLineConfig config) {
            super(line, config);
            this.item = new ItemComponentPaperItemStack(item);
        }

        @Override
        public AbstractItemStack getDrop(DropMetadata meta, double amount) {
            int finalAmount = this.rollBonuses(meta, Numbers.floor(this.item.getAmount() * amount));
            return this.item.copy().amount(finalAmount);
        }
    }
}
