package gg.auroramc.aurora.api.placeholder;

import org.bukkit.entity.Player;

import java.util.*;

public class PlaceholderHandlerRegistry {
    private static final Map<String, PlaceholderHandler> handlers = new HashMap<>();

    public static void addHandler(PlaceholderHandler handler) {
        if(handler == null) return;
        handlers.put(handler.getIdentifier(), handler);
    }

    public static void removeHandler(PlaceholderHandler handler) {
        if(handler == null) return;
        handlers.remove(handler.getIdentifier());
    }

    public static String fillPlaceholderRequest(Player player, String params) {
        var splitParams = params.split("_");
        if(splitParams.length == 0) return null;

        var id = splitParams[0];
        var handler = handlers.get(id);
        if(handler == null) return null;

        return handler.onPlaceholderRequest(player, Arrays.copyOfRange(splitParams, 1, splitParams.length));
    }

    public static String getGlobalId() {
        return "aurora";
    }

    public static List<String> getPatterns() {
        var list = new ArrayList<String>();

        for(var handler : handlers.values()) {
            if(handler.getPatterns() == null) {
                list.add("%" + getGlobalId() + "_" + handler.getIdentifier() + "%");
            } else {
                list.addAll(handler.getPatterns().stream().map(p -> p.isEmpty() ? "%" + getGlobalId() + "_" + handler.getIdentifier() + "%" : "%" + getGlobalId() + "_" + handler.getIdentifier() + "_" + p + "%").toList());
            }
        }

        return list;
    }
}
