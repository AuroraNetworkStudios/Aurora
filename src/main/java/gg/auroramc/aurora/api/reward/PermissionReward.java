package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import lombok.Getter;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.node.Node;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionReward extends AbstractReward {
    @Getter
    private String permission;
    private boolean value;
    private final Map<String, String> contexts = new HashMap<>();

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (permission == null) return;
        var node = buildNode(player, placeholders);
        LuckPermsProvider.get().getUserManager().modifyUser(player.getUniqueId(), user -> user.data().add(node));
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        permission = args.getString("permission", null);
        value = args.getBoolean("value", true);
        display = args.getString("display", "");

        if (args.isConfigurationSection("contexts")) {
            ConfigurationSection contextSection = args.getConfigurationSection("contexts");
            for (String key : contextSection.getKeys(false)) {
                contexts.put(key, contextSection.getString(key));
            }
        }

        if (permission == null) {
            Aurora.logger().warning("PermissionReward doesn't have the permission key");
        }
    }

    public Node buildNode(Player player, List<Placeholder<?>> placeholders) {
        var builder = Node.builder(Text.fillPlaceholders(player, permission, placeholders)).value(value);

        if (!contexts.isEmpty()) {
            var contextSet = MutableContextSet.create();

            for (var entry : contexts.entrySet())
                contextSet.add(entry.getKey(), Text.fillPlaceholders(player, entry.getValue(), placeholders));

            builder.withContext(contextSet);
        }

        return builder.build();
    }
}
