package gg.auroramc.aurora.api.menu;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.config.premade.RequirementConfig;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
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

    public static boolean isAllMet(Player player, List<String> requirements) {
        if (requirements == null) return true;
        for (var requirement : requirements) {
            if (!isMet(player, requirement)) return false;
        }
        return true;
    }

    public static boolean passes(Player player, List<RequirementConfig> requirements) {
        if (requirements == null) return true;
        for (var requirement : requirements) {
            if (!isMet(player, requirement.getRequirement())) {
                if (requirement.getDenyActions() != null) {
                    requirement.getDenyActions().forEach(action -> CommandDispatcher.dispatch(player, action));
                }
                return false;
            }
        }
        return true;
    }

    public static boolean isMet(Player player, String requirement) {
        if (requirement == null || requirement.isEmpty()) return true;

        if (requirement.startsWith("[permission]")) {
            return player.hasPermission(requirement.substring(13));
        }

        var args = requirement.split(" ");

        if (args[0].equalsIgnoreCase("[money]")) {
            var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
            var economy = args.length > 1 ? expansion.getEconomy(args[2]) : expansion.getDefaultEconomy();
            return economy.hasBalance(player, Double.parseDouble(args[1]));
        }

        if (args[0].equalsIgnoreCase("[exp-level]")) {
            return player.getLevel() >= Integer.parseInt(args[1]);
        }

        if (args[0].equalsIgnoreCase("[placeholder]")) {
            if (!DependencyManager.hasDep(Dep.PAPI)) return false;
            var placeholderValue = PlaceholderAPI.setPlaceholders(player, args[1]);
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

        var customResolver = resolvers.get(args[0]);

        if (customResolver != null) {
            return customResolver.apply(args);
        }

        return false;
    }
}
