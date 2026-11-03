package gg.auroramc.aurora.api.command;

import com.google.common.collect.Maps;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.AuroraAPI;
import gg.auroramc.aurora.api.dependency.Dep;
import gg.auroramc.aurora.api.dependency.DependencyManager;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.ActionBar;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.util.ItemUtils;
import gg.auroramc.aurora.expansions.economy.AuroraEconomy;
import gg.auroramc.aurora.expansions.economy.EconomyExpansion;
import gg.auroramc.aurora.expansions.gui.GuiExpansion;
import gg.auroramc.aurora.hooks.LuckPermsHook;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CommandDispatcher {
    private static final Map<String, BiConsumer<Player, String>> actions = Maps.newConcurrentMap();
    // Actions that take something from the player, the rest of an action list only runs if they could
    private static final Map<String, BiPredicate<Player, String>> costActions = Maps.newConcurrentMap();
    // [delay: 20], [delay:20], [delay 20] or [delay] 20
    private static final Pattern DELAY_PATTERN = Pattern.compile("^\\[delay(?:\\s*:\\s*|\\s+|(?=\\]))([^\\]]*)\\]\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    static {
        registerActionHandler("default", CommandDispatcher::runConsoleCommand);
        registerActionHandler("console", CommandDispatcher::runConsoleCommand);
        registerActionHandler("player", CommandDispatcher::runPlayerCommand);

        registerActionHandler("message", (player, message) -> player.sendMessage(Text.component(player, message)));
        registerActionHandler("actionbar", ActionBar::send);
        registerActionHandler("sound", CommandDispatcher::playSound);

        registerActionHandler("permission", (player, input) -> {
            if (DependencyManager.hasDep("LuckPerms")) {
                var args = ArgumentParser.parseString(input);
                LuckPermsHook.grantPermission(player, args.get("prefix"), args);
            }
        });

        registerActionHandler("close", (player, command) -> player.getScheduler().run(Aurora.getInstance(), task -> player.closeInventory(), null));
        registerActionHandler("open-gui", (player, input) -> {
            var args = ArgumentParser.parseString(input);
            Aurora.getExpansionManager().getExpansion(GuiExpansion.class).openGui(args.get("prefix"), player, args);
        });

        registerCostAction("take-items", CommandDispatcher::takeItems);
        registerActionHandler("give-item", CommandDispatcher::giveItem);

        registerActionHandler("placeholder", (player, placeholder) -> {
            if (DependencyManager.hasDep(Dep.PAPI)) PlaceholderAPI.setPlaceholders(player, placeholder);
        });

        registerActionHandler("give-money", (player, input) -> useEconomy(input, (econ, currency, amount) -> {
            econ.deposit(player, currency, amount);
            return true;
        }));
        registerCostAction("take-money", (player, input) -> useEconomy(input, (econ, currency, amount) -> econ.tryWithdraw(player, currency, amount)));
    }

    public static void registerActionHandler(String id, BiConsumer<Player, String> handler) {
        // Replacing a built-in cost action makes it a normal action
        costActions.remove(id);
        actions.put(id, handler);
    }

    private static void registerCostAction(String id, BiPredicate<Player, String> cost) {
        actions.put(id, cost::test);
        costActions.put(id, cost);
    }

    public static Collection<String> getActions() {
        return actions.keySet();
    }

    private static Map.Entry<String, String> extractActionAndContent(String input) {
        if (input.charAt(0) == '[') {
            int end = input.indexOf(']');
            if (end != -1) {
                String action = input.substring(1, end);
                String content = removeFirstSpace(input.substring(end + 1));

                return new AbstractMap.SimpleEntry<>(action, content);
            }
        }
        return new AbstractMap.SimpleEntry<>("default", input);
    }

    public record MetaRecord(String action, String key, String value) {
    }

    public static void dispatch(Player player, String command) {
        run(player, command);
    }

    /**
     * @return false if this was a cost action (like [take-items]) that couldn't take everything, true otherwise
     */
    private static boolean run(Player player, String command) {
        if (command.startsWith("[meta")) {
            var data = Aurora.getUserManager().getUser(player).getMetaData();
            var meta = parseMetaString(command);

            if (meta.action == null) return true;
            if (meta.key == null) return true;

            switch (meta.action) {
                case "set" -> {
                    try {
                        var value = Double.parseDouble(meta.value);
                        data.setMeta(meta.key, value);
                    } catch (NumberFormatException e) {
                        data.setMeta(meta.key, meta.value);
                    }
                }
                case "remove" -> data.removeMeta(meta.key);
                case "increment" ->
                        data.incrementMeta(meta.key, meta.value == null ? 1 : Double.parseDouble(meta.value));
                case "decrement" ->
                        data.decrementMeta(meta.key, meta.value == null ? 1 : Double.parseDouble(meta.value));
            }
            return true;
        }

        if (parseDelay(command, null, false) != null) {
            Aurora.logger().warning("[delay] only works in action lists (menu actions, open/close actions, deny-actions, command rewards), ignoring: " + command);
            return true;
        }

        var action = extractActionAndContent(command);
        var cost = costActions.get(action.getKey());

        if (cost != null) {
            try {
                return cost.test(player, action.getValue());
            } catch (Exception e) {
                Aurora.logger().severe("Failed to execute action: " + action.getKey() + " with content: " + action.getValue() + " for player: " + player.getName() + " with error: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }

        var handler = actions.get(action.getKey());

        if (handler != null) {
            try {
                handler.accept(player, action.getValue());
            } catch (Exception e) {
                Aurora.logger().severe("Failed to execute action: " + action.getKey() + " with content: " + action.getValue() + " for player: " + player.getName() + " with error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            Aurora.logger().warning("Invalid dispatcher action: " + action.getKey());
        }
        return true;
    }

    public static void dispatch(Player player, String command, List<Placeholder<?>> placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    public static void dispatch(Player player, String command, Placeholder<?>... placeholders) {
        dispatch(player, Placeholder.execute(command, placeholders));
    }

    /**
     * Dispatches the actions in order. A "[delay: ticks]" line pauses the list and the actions
     * below it run after that many ticks. If the player leaves before that, they are skipped and logged.
     * If a cost action like [take-items] or [take-money] can't take everything, the actions below it are skipped.
     */
    public static void dispatchAll(Player player, List<String> commands, List<Placeholder<?>> placeholders) {
        dispatchAll(player, commands, placeholders, null);
    }

    /**
     * Same as {@link #dispatchAll(Player, List, List)}, but the requirements are checked again after every delay
     * and the actions below it only run if they still pass. Otherwise they could be bypassed while waiting,
     * e.g. by giving the money away before it's taken.
     *
     * @param requirements checks the requirements that allowed these actions (and runs their deny actions)
     */
    public static void dispatchAll(Player player, List<String> commands, List<Placeholder<?>> placeholders, @Nullable BooleanSupplier requirements) {
        if (commands == null) return;
        for (int i = 0; i < commands.size(); i++) {
            var command = commands.get(i);
            var delay = parseDelay(command, placeholders, true);

            if (delay == null) {
                if (!run(player, Placeholder.execute(command, placeholders))) {
                    Aurora.logger().debug(player.getName() + " couldn't pay for: " + Placeholder.execute(command, placeholders) + ", skipping the actions after it");
                    return;
                }
                continue;
            }

            // A delay at the end of the list has nothing to wait for
            if (delay == 0 || i == commands.size() - 1) continue;

            var rest = new ArrayList<>(commands.subList(i + 1, commands.size()));
            var task = player.getScheduler().runDelayed(Aurora.getInstance(), (t) -> {
                if (requirements == null || requirements.getAsBoolean()) {
                    dispatchAll(player, rest, placeholders, requirements);
                }
            }, () -> logSkipped(player, rest, placeholders), delay);
            // null means the player is already gone
            if (task == null) logSkipped(player, rest, placeholders);
            return;
        }
    }

    /**
     * @return the sum of the [delay] lines in ticks, 0 if there are none
     */
    public static long getTotalDelay(List<String> commands, List<Placeholder<?>> placeholders) {
        if (commands == null) return 0;
        long total = 0;
        for (var command : commands) {
            var delay = parseDelay(command, placeholders, false);
            if (delay != null) total += delay;
        }
        return total;
    }

    private static void logSkipped(Player player, List<String> rest, List<Placeholder<?>> placeholders) {
        // Runs while the player is being removed, it must not throw
        try {
            var skipped = new ArrayList<String>();
            for (var command : rest) {
                if (command != null && parseDelay(command, placeholders, false) == null) {
                    skipped.add(Placeholder.execute(command, placeholders));
                }
            }
            if (!skipped.isEmpty()) {
                Aurora.logger().warning(player.getName() + " left before delayed actions could run, skipped: "
                        + Aurora.getMiniMessage().escapeTags(String.join(" | ", skipped)));
            }
        } catch (Exception e) {
            Aurora.logger().warning(player.getName() + " left before delayed actions could run, skipped " + rest.size() + " lines");
        }
    }

    /**
     * @return null if the line isn't a delay, otherwise the delay in ticks. An invalid amount counts as 0.
     */
    private static @Nullable Integer parseDelay(String command, @Nullable List<Placeholder<?>> placeholders, boolean warn) {
        // A "delay" action registered through the API keeps working like before
        if (command == null || actions.containsKey("delay")) return null;
        var line = command.strip();
        if (!line.regionMatches(true, 0, "[delay", 0, 6)) return null;

        var matcher = DELAY_PATTERN.matcher(Placeholder.execute(line, placeholders).strip());
        if (!matcher.matches()) return null;

        var inside = matcher.group(1).strip();
        var after = matcher.group(2).strip();
        try {
            if (inside.isEmpty() == after.isEmpty()) throw new NumberFormatException();
            var ticks = Integer.parseInt(inside.isEmpty() ? after : inside);
            if (ticks < 0) throw new NumberFormatException();
            return ticks;
        } catch (NumberFormatException e) {
            if (warn) {
                Aurora.logger().warning("Invalid delay: " + line + ", it has to be a number of ticks like [delay: 20]. Running the next actions without delay.");
            }
            return 0;
        }
    }

    private static void runConsoleCommand(Player player, String command) {
        if (!DependencyManager.hasDep(Dep.PAPI)) {
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
            return;
        }
        // Placeholders read the player, on Folia only from their own thread. The console command runs on the global
        // thread, the only one it may run on there.
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            // Already there (rewards, menu clicks): one hop like before, so it keeps its order with [player] lines
            var cmd = PlaceholderAPI.setPlaceholders(player, command);
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (task) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            return;
        }
        Runnable onGlobal = () -> Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(),
                (task) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(player, command)));
        var scheduled = player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var cmd = PlaceholderAPI.setPlaceholders(player, command);
            Bukkit.getGlobalRegionScheduler().run(Aurora.getInstance(), (t) -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }, onGlobal);
        // Null if they left already, then neither runs
        if (scheduled == null) onGlobal.run();
    }

    private static void runPlayerCommand(Player player, String command) {
        player.getScheduler().run(Aurora.getInstance(), (task) -> {
            var cmd = DependencyManager.hasDep(Dep.PAPI) ? PlaceholderAPI.setPlaceholders(player, command) : command;
            player.performCommand(cmd);
        }, null);
    }

    private static void playSound(Player player, String cmd) {
        String[] args = cmd.split(" ");
        if (args.length == 0) return;
        var sound = Registry.SOUNDS.get(NamespacedKey.fromString(args[0]));
        if (sound == null) {
            Aurora.logger().warning("Invalid sound: " + args[0]);
            return;
        }
        if (args.length == 1) {
            player.playSound(player, sound, 1, 1);
        } else if (args.length == 2) {
            player.playSound(player.getLocation(), sound, Float.parseFloat(args[1]), 1);
        } else if (args.length == 3) {
            player.playSound(player.getLocation(), sound, Float.parseFloat(args[1]), Float.parseFloat(args[2]));
        }
    }

    private static MetaRecord parseMetaString(String input) {
        Pattern pattern = Pattern.compile("\\[meta:(set|remove|increment|decrement):([a-zA-Z0-9_-]+)]\\s*(.*)");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            String action = matcher.group(1);
            String key = matcher.group(2);
            String value = matcher.group(3).isEmpty() ? null : matcher.group(3);

            return new MetaRecord(action, key, value);
        } else {
            return new MetaRecord(null, null, null);
        }
    }

    private static String removeFirstSpace(String text) {
        if (text.startsWith(" ")) {
            return text.substring(1);
        }
        return text;
    }

    private static boolean useEconomy(String input, EconomyAction action) {
        var expansion = Aurora.getExpansionManager().getExpansion(EconomyExpansion.class);
        if (expansion == null) {
            Aurora.logger().warning("There is no economy plugin installed, can't run: " + input);
            return false;
        }
        var econ = expansion.getDefaultEconomy();
        var args = ArgumentParser.parseString(input);
        if (args.containsKey("economy")) {
            econ = expansion.getEconomy(args.get("economy"));
        }
        if (econ == null) {
            Aurora.logger().warning("Invalid economy provider: " + args.get("economy"));
            return false;
        }
        if (args.containsKey("currency")) {
            if (!econ.supportsCurrency() || !econ.validateCurrency(args.get("currency"))) {
                Aurora.logger().warning("Currency " + args.get("currency") + " is not supported by economy provider " + args.get("economy") + ". Please check your configuration.");
                return false;
            }
        }
        var amount = Double.parseDouble(args.get("prefix"));
        // A negative amount would turn taking into giving
        if (!Double.isFinite(amount) || amount < 0) {
            Aurora.logger().warning("Invalid money amount: " + args.get("prefix") + ", it has to be a positive number.");
            return false;
        }
        return action.apply(econ, args.getOrDefault("currency", "default"), amount);
    }

    @FunctionalInterface
    private interface EconomyAction {
        boolean apply(AuroraEconomy economy, String currency, double amount);
    }

    private static void giveItem(Player player, String input) {
        var args = input.split(" ");
        var split = args[0].split("/");
        var stash = args.length > 1 && args[1].equalsIgnoreCase("true");
        var typeId = TypeId.fromDefault(split[0]);
        var amount = Integer.parseInt(split[1]);
        var item = AuroraAPI.getItemManager().resolveItem(typeId);

        if (item == null || item.isEmpty()) {
            Aurora.logger().warning("Failed to resolve item: " + typeId);
            return;
        }

        var stacks = ItemUtils.createStacksFromAmount(item, amount);

        var failed = player.getInventory().addItem(stacks);

        if (!failed.isEmpty()) {
            if (stash) {
                ItemUtils.addToStash(player, failed.values());
            } else {
                Bukkit.getRegionScheduler().run(Aurora.getInstance(), player.getLocation(), (t) -> {
                    failed.forEach((index, itemStack) -> player.getWorld().dropItem(player.getLocation(), itemStack));
                });
            }
        }
    }

    private static boolean takeItems(Player player, String input) {
        // oraxen:example/45 oraxen:example2/32

        // Matched the same way as [has-items], so it only takes what that requirement counted.
        // The same item listed twice has to be there twice.
        var wanted = new LinkedHashMap<TypeId, Integer>();
        for (var id : input.split(" ")) {
            if (id.isBlank()) continue;
            var split = id.split("/");
            wanted.merge(TypeId.fromDefault(split[0]), Integer.parseInt(split[1]), Integer::sum);
        }

        var inv = player.getInventory();
        // Item to take, with the amount. Two ids can resolve to the same item, then both amounts are needed from the same stacks.
        var toTake = new LinkedHashMap<ItemStack, Integer>();

        for (var entry : wanted.entrySet()) {
            var item = AuroraAPI.getItemManager().resolveItem(entry.getKey());
            if (item == null || item.isEmpty()) {
                Aurora.logger().warning("Failed to resolve item: " + entry.getKey());
                return false;
            }
            if (entry.getValue() <= 0) continue;
            var same = toTake.keySet().stream().filter(key -> key.isSimilar(item)).findFirst();
            toTake.merge(same.orElse(item), entry.getValue(), Integer::sum);
        }

        var stacks = new ArrayList<ItemStack>();
        for (var entry : toTake.entrySet()) {
            if (!inv.containsAtLeast(entry.getKey(), entry.getValue())) {
                return false;
            }
            stacks.addAll(List.of(ItemUtils.createStacksFromAmount(entry.getKey(), entry.getValue())));
        }

        // Only the storage contents, like containsAtLeast, so armor and the offhand are never taken
        var left = inv.removeItem(stacks.toArray(ItemStack[]::new));
        if (!left.isEmpty()) {
            Aurora.logger().severe("Couldn't take all items from " + player.getName() + " for: " + input + ", missing: " + left.values());
            return false;
        }
        return true;
    }

    public static class ItemData {
        private final TypeId typeId;
        private int amount;
        private final ItemStack item;

        public ItemData(ItemStack item, TypeId typeId, int amount) {
            this.typeId = typeId;
            this.amount = amount;
            this.item = item;
        }

        public TypeId typeId() {
            return typeId;
        }

        public int amount() {
            return amount;
        }

        public void decrement(int amount) {
            this.amount -= amount;
        }

        public ItemStack item() {
            return item;
        }
    }
}
