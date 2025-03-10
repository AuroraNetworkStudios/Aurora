package gg.auroramc.aurora.api.reward;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.util.ThreadSafety;
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
    private List<String> permissions;
    private boolean value;
    private final Map<String, String> contexts = new HashMap<>();

    @Override
    public void execute(Player player, long level, List<Placeholder<?>> placeholders) {
        if (permissions.isEmpty()) return;

        var nodes = buildNodes(player, placeholders);

        var user = LuckPermsProvider.get().getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            Aurora.logger().warning("User " + player.getName() + " doesn't have LuckPerms user object");
            return;
        }

        for (var node : nodes) user.data().add(node);

        LuckPermsProvider.get().getUserManager().saveUser(user);
    }

    @Override
    public void init(ConfigurationSection args) {
        super.init(args);
        if (args.isString("permission") && args.getString("permission") != null) {
            permissions = List.of(args.getString("permission"));
        } else if (args.isList("permission")) {
            permissions = args.getStringList("permission");
        } else {
            permissions = List.of();
            Aurora.logger().warning("PermissionReward doesn't have the permission key");
        }
        value = args.getBoolean("value", true);

        if (args.isConfigurationSection("contexts")) {
            ConfigurationSection contextSection = args.getConfigurationSection("contexts");
            for (String key : contextSection.getKeys(false)) {
                contexts.put(key, contextSection.getString(key));
            }
        }
    }

    public List<Node> buildNodes(Player player, List<Placeholder<?>> placeholders) {
        return permissions.stream().map(permission -> {
            var builder = Node.builder(Text.fillPlaceholders(player, permission, placeholders)).value(value);

            if (!contexts.isEmpty()) {
                var contextSet = MutableContextSet.create();

                for (var entry : contexts.entrySet())
                    contextSet.add(entry.getKey(), Text.fillPlaceholders(player, entry.getValue(), placeholders));

                builder.withContext(contextSet);
            }

            return (Node) builder.build();
        }).toList();
    }

    @Override
    public ThreadSafety getThreadSafety() {
        return ThreadSafety.ASYNC_ONLY;
    }
}
