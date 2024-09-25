package gg.auroramc.aurora.hooks;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

import java.util.Map;

public class LuckPermsHook {
    public static void registerListeners() {
        LuckPermsProvider.get().getEventBus().subscribe(UserDataRecalculateEvent.class, event ->
                Aurora.getExpansionManager().getExpansion(GuiExpansion.class).refreshPlayerGuis(event.getUser().getUniqueId()));
    }

    public static void grantPermission(Player player, String permission, Map<String, String> contexts) {
        var user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        if(user == null) return;

        var nodeBuilder = Node.builder(permission);

        if(contexts.containsKey("value")) {
            nodeBuilder.value(Boolean.parseBoolean(contexts.get("value")));
        }

        for(var entry : contexts.entrySet()) {
            if(entry.getKey().equals("prefix")) continue;
            if(entry.getKey().equals("value")) continue;
            nodeBuilder.withContext(entry.getKey(), entry.getValue());
        }

        user.data().add(nodeBuilder.build());
        LuckPermsProvider.get().getUserManager().saveUser(user);
    }
}
