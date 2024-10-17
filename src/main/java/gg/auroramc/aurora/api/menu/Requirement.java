package gg.auroramc.aurora.api.menu;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.config.premade.RequirementConfig;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Requirement {
    private static final Map<String, Function<String[], Boolean>> resolvers = Maps.newConcurrentMap();

    public static void register(String name, Function<String[], Boolean> resolver) {
        resolvers.put("[" + name + "]", resolver);
    }

    public static boolean isAllMet(Player player, List<String> requirements, List<Placeholder<?>> placeholders) {
        if (requirements == null) return true;
        for (var requirement : requirements) {
            if (!isMet(player, requirement, placeholders)) return false;
        }
        return true;
    }

    public static boolean passes(Player player, List<RequirementConfig> requirements, List<Placeholder<?>> placeholders) {
        if (requirements == null) return true;
        for (var requirement : requirements) {
            if (!isMet(player, requirement.getRequirement(), placeholders)) {
                if (requirement.getDenyActions() != null) {
                    requirement.getDenyActions().forEach(action -> CommandDispatcher.dispatch(player, action, placeholders));
                }
                return false;
            }
        }
        return true;
    }

    public static boolean isMet(Player player, String requirement, List<Placeholder<?>> placeholders) {
        if (requirement == null || requirement.isEmpty()) return true;

        if (requirement.startsWith("!")) {
            return !isMet(player, requirement.substring(1), placeholders);
        }

        requirement = Placeholder.execute(requirement, placeholders);

        if (requirement.startsWith("[permission]")) {
            var perm = requirement.substring(13);
            return player.hasPermission(perm);
        }

        var args = requirement.split(" ");

        if (args[0].equalsIgnoreCase("[money]")) {
            var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
            var economy = args.length > 2 ? expansion.getEconomy(args[2]) : expansion.getDefaultEconomy();
            String currency = null;
            if (args.length > 3) {
                currency = args[3];
            }
            if (currency != null) {
                if (economy.supportsCurrency() && economy.validateCurrency(currency)) {
                    return economy.hasBalance(player, currency, Double.parseDouble(args[1]));
                } else {
                    Aurora.logger().warning("Currency " + currency + " is not supported by economy provider " + economy + ". Please check your requirement configuration: " + requirement);
                }
            } else {
                return economy.hasBalance(player, Double.parseDouble(args[1]));
            }
        }

        if (args[0].equalsIgnoreCase("[exp-level]")) {
            return player.getLevel() >= Integer.parseInt(args[1]);
        }

        if (args[0].equalsIgnoreCase("[placeholder]")) {
            if (!DependencyManager.hasDep(Dep.PAPI)) return false;
            var placeholderValue = PlaceholderAPI.setPlaceholders(player, args[1]);
            return doPlaceholderCheck(args, placeholderValue);
        }

        if (args[0].equalsIgnoreCase("[arg]")) {
            var placeholderValue = Placeholder.execute("{arg_" + args[1] + "}", placeholders);
            return doPlaceholderCheck(args, placeholderValue);
        }

        if (args[0].equalsIgnoreCase("[has-items]")) {
            // oraxen:example/45 oraxen:example2/32

            for (int i = 1; i < args.length; i++) {
                var split = args[i].split("/");
                var item = AuroraAPI.getItemManager().resolveItem(TypeId.fromDefault(split[0]));
                var amount = Integer.parseInt(split[1]);

                if (!player.getInventory().containsAtLeast(item, amount)) {
                    return false;
                }
            }
            return true;
        }

        var customResolver = resolvers.get(args[0]);

        if (customResolver != null) {
            return customResolver.apply(args);
        }

        return false;
    }

    private static boolean doPlaceholderCheck(String[] args, String placeholderValue) {
        var compareValue = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

        return switch (args[2]) {
            case "==" -> placeholderValue.equals(compareValue);
            case ">=" -> Double.parseDouble(placeholderValue) >= Double.parseDouble(compareValue);
            case "<=" -> Double.parseDouble(placeholderValue) <= Double.parseDouble(compareValue);
            case "<" -> Double.parseDouble(placeholderValue) < Double.parseDouble(compareValue);
            case ">" -> Double.parseDouble(placeholderValue) > Double.parseDouble(compareValue);
            default -> false;
        };
    }
}
