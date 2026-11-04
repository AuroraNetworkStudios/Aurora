package bench;

import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.command.CommandDispatcher;
import gg.auroramc.aurora.api.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.*;

// Test helper for the pending changes version of Aurora
public class BenchPlugin extends JavaPlugin {
    private BenchThreads threads;

    @Override
    public void onEnable() {
        threads = new BenchThreads(this::log);
        Bukkit.getPluginManager().registerEvents(threads, this);
    }

    static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    static int diamonds(Collection<ItemStack> items) {
        return items.stream().filter(i -> i.getType() == Material.DIAMOND).mapToInt(ItemStack::getAmount).sum();
    }

    private static UserManager um() {
        return Aurora.getUserManager();
    }

    // Stacks of at most 64, a bigger stack can't be saved
    private static java.util.concurrent.CompletableFuture<Boolean> addDiamonds(java.util.UUID uuid, int amount) {
        return um().editUser(uuid, user -> {
            for (int left = amount; left > 0; left -= 64) user.getStashData().addItem(new ItemStack(Material.DIAMOND, Math.min(64, left)));
            return true;
        });
    }

    private static java.util.concurrent.CompletableFuture<Boolean> takeDiamonds(java.util.UUID uuid, int amount) {
        return um().editUser(uuid, user -> BenchTakeDiamonds.take(user, amount));
    }

    private void log(String line) {
        getLogger().info("BENCH " + line);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args[0]) {
            case "add" -> {
                addDiamonds(offlineUuid(args[1]), Integer.parseInt(args[2]));
                log("add name=" + args[1] + " amount=" + args[2] + " sent");
            }
            case "take" -> {
                var t0 = System.nanoTime();
                takeDiamonds(offlineUuid(args[1]), Integer.parseInt(args[2])).whenComplete((ok, e) ->
                        log(String.format(Locale.ROOT, "take name=%s amount=%s result=%s ms=%.1f", args[1], args[2], e != null ? "ERROR:" + e : ok, (System.nanoTime() - t0) / 1e6)));
            }
            case "transfer" -> {
                // The part that can fail first, then the part that can't
                var amount = Integer.parseInt(args[3]);
                takeDiamonds(offlineUuid(args[1]), amount).whenComplete((ok, e) -> {
                    if (Boolean.TRUE.equals(ok)) addDiamonds(offlineUuid(args[2]), amount);
                    log("transfer from=" + args[1] + " to=" + args[2] + " amount=" + amount + " result=" + (e != null ? "ERROR:" + e : ok));
                });
            }
            case "takelat" -> Bukkit.getAsyncScheduler().runNow(this, t -> {
                var times = new ArrayList<Long>();
                var results = new HashMap<Boolean, Integer>();
                for (int i = 0; i < Integer.parseInt(args[2]); i++) {
                    var t0 = System.nanoTime();
                    var ok = takeDiamonds(offlineUuid(args[1]), 1).join();
                    times.add(System.nanoTime() - t0);
                    results.merge(ok, 1, Integer::sum);
                }
                Collections.sort(times);
                log(String.format(Locale.ROOT, "takelat name=%s results=%s p50=%.1fms p95=%.1fms max=%.1fms", args[1], results, pct(times, 50), pct(times, 95), pct(times, 100)));
            });
            case "shopburst" -> {
                // A busy chest shop: deposits and withdrawals to one player at the same time
                var uuid = offlineUuid(args[1]);
                var withdrawals = new ArrayList<java.util.concurrent.CompletableFuture<Boolean>>();
                var start = System.nanoTime();
                for (int i = 0; i < Math.max(Integer.parseInt(args[2]), Integer.parseInt(args[3])); i++) {
                    if (i < Integer.parseInt(args[2])) addDiamonds(uuid, 1);
                    if (i < Integer.parseInt(args[3])) withdrawals.add(takeDiamonds(uuid, 1));
                }
                java.util.concurrent.CompletableFuture.allOf(withdrawals.toArray(new java.util.concurrent.CompletableFuture[0])).whenComplete((v, e) -> {
                    var taken = withdrawals.stream().filter(f -> Boolean.TRUE.equals(f.getNow(false))).count();
                    log(String.format(Locale.ROOT, "shopburst name=%s taken=%d refused=%d total=%.1fms", args[1], taken, withdrawals.size() - taken, (System.nanoTime() - start) / 1e6));
                });
            }
            // Like Vault calls from a plugin: right away on the main thread, n times, with how long each took
            case "nowadd", "nowtake" -> Bukkit.getGlobalRegionScheduler().run(this, t -> {
                var uuid = offlineUuid(args[1]);
                var n = Integer.parseInt(args[2]);
                var times = new ArrayList<Long>();
                int ok = 0;
                for (int i = 0; i < n; i++) {
                    var t0 = System.nanoTime();
                    var done = args[0].equals("nowadd")
                            ? um().editUserNow(uuid, user -> user.getStashData().addItem(new ItemStack(Material.DIAMOND, 1)))
                            : um().editUserNow(uuid, user -> BenchTakeDiamonds.take(user, 1));
                    times.add(System.nanoTime() - t0);
                    if (done) ok++;
                }
                var first = times.getFirst() / 1e6;
                var rest = times.subList(1, times.size()).stream().mapToLong(Long::longValue).max().orElse(0) / 1e6;
                Collections.sort(times);
                log(String.format(Locale.ROOT, "%s name=%s ok=%d of=%d first=%.2fms rest-max=%.2fms max=%.2fms", args[0], args[1], ok, n, first, rest, pct(times, 100)));
            });
            case "nowget" -> Bukkit.getGlobalRegionScheduler().run(this, t -> {
                var t0 = System.nanoTime();
                var user = um().getUserNow(offlineUuid(args[1]));
                var ms = (System.nanoTime() - t0) / 1e6;
                log(String.format(Locale.ROOT, "nowget name=%s diamonds=%s ms=%.2f", args[1], user == null ? "NULL" : diamonds(user.getStashData().getItems()), ms));
            });
            // Saves an online player's session (like the autosave), off the main thread
            case "save" -> Bukkit.getAsyncScheduler().runNow(this, t -> {
                var uuid = offlineUuid(args[1]);
                var t0 = System.nanoTime();
                var ok = um().isUserCached(uuid) && um().saveUserData(um().getUser(uuid), gg.auroramc.aurora.api.user.storage.SaveReason.AUTO_SAVE);
                log(String.format(Locale.ROOT, "save name=%s result=%s ms=%.1f", args[1], ok, (System.nanoTime() - t0) / 1e6));
            });
            // A diamond named with a 4 byte character, which MySQL's 3 byte utf8 can't store
            // Meta is stored as plain text (stash items are Base64, so they never hit the column's charset)
            case "addemoji" -> um().editUser(offlineUuid(args[1]), user -> user.getMetaData().setMeta("title", "Shiny 💎"))
                    .thenAccept(ok -> log("addemoji name=" + args[1] + " result=" + ok));
            case "meta" -> um().loadUserFromStorage(offlineUuid(args[1])).whenComplete((user, e) ->
                    log("meta name=" + args[1] + " title=" + (e != null || !user.isLoaded() ? "FAILED" : user.getMetaData().getMeta("title", "none"))));
            case "rewards" -> threads.runRewards(Bukkit.getPlayerExact(args[1]));
            case "clear" -> {
                um().editUser(offlineUuid(args[1]), user -> user.getStashData().clear());
                log("clear name=" + args[1] + " sent");
            }
            case "stored" -> um().loadUserFromStorage(offlineUuid(args[1])).whenComplete((user, e) ->
                    log("stored name=" + args[1] + " diamonds=" + (e != null || !user.isLoaded() ? "FAILED" : diamonds(user.getStashData().getItems()))));
            case "inspect" -> Bukkit.getAsyncScheduler().runNow(this, t -> {
                var uuid = offlineUuid(args[1]);
                var local = "-";
                if (um().isUserCached(uuid)) {
                    var user = um().getUser(uuid);
                    local = "loaded=" + user.isLoaded() + " diamonds=" + diamonds(user.getStashData().getItems());
                }
                log("inspect name=" + args[1] + " online=" + (Bukkit.getPlayer(uuid) != null) + " " + local
                        + " pending=0 flag=" + flag(uuid));
            });
            case "channels" -> log("channels in=" + Bukkit.getMessenger().getIncomingChannels(Aurora.getInstance())
                    + " out=" + Bukkit.getMessenger().getOutgoingChannels(Aurora.getInstance())
                    + " proxy=" + Bukkit.getServer().getServerConfig().isProxyEnabled());
            case "pending" -> Bukkit.getAsyncScheduler().runNow(this, t ->
                    log("pending users=0 flags=" + flagCount()));
            case "burst" -> burst(Integer.parseInt(args[1]), args[2], args.length > 3 && args[3].equals("same"));
            case "verify" -> verify(Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
            case "reward" -> {
                var player = Bukkit.getPlayerExact(args[1]);
                var section = new org.bukkit.configuration.MemoryConfiguration();
                section.set("type", "item");
                section.set("stash", args[2]);
                section.set("item.material", "diamond");
                section.set("item.amount", Integer.parseInt(args[3]));
                var reward = new gg.auroramc.aurora.api.reward.ItemReward();
                reward.init(section);
                player.getScheduler().run(this, t -> {
                    reward.execute(player, 0, List.of());
                    log("reward executed name=" + args[1] + " stash=" + args[2]);
                }, null);
            }
            case "give" -> {
                var player = Bukkit.getPlayerExact(args[1]);
                player.getScheduler().run(this, t -> {
                    CommandDispatcher.dispatch(player, "[give-item] diamond/" + args[2] + " true");
                    log("give executed name=" + args[1] + " inventory=" + diamonds(Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).toList()));
                }, null);
            }
            case "fill" -> {
                // Fills the inventory with dirt so items given go to the stash
                var player = Bukkit.getPlayerExact(args[1]);
                player.getScheduler().run(this, t -> {
                    player.getInventory().clear();
                    for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Material.DIRT, 64));
                    log("fill done name=" + args[1]);
                }, null);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // Changes to many offline players (or many changes to one), then waits until all of them are applied
    private void burst(int count, String prefix, boolean same) {
        var start = System.nanoTime();
        var edits = new ArrayList<java.util.concurrent.CompletableFuture<Boolean>>();
        for (int i = 0; i < count; i++) {
            edits.add(addDiamonds(offlineUuid(same ? prefix : prefix + i), 1));
        }
        var submitted = (System.nanoTime() - start) / 1e6;
        java.util.concurrent.CompletableFuture.allOf(edits.toArray(new java.util.concurrent.CompletableFuture[0])).whenComplete((v, e) ->
                log(String.format(Locale.ROOT, "burst count=%d same=%s main-thread=%.2fms applied-after=%.0fms", count, same, submitted, (System.nanoTime() - start) / 1e6)));
    }

    private void verify(int count, String prefix, int expected) {
        Bukkit.getAsyncScheduler().runNow(this, t -> {
            int ok = 0, wrong = 0;
            String example = "";
            for (int i = 0; i < count; i++) {
                var user = um().loadUserFromStorage(offlineUuid(prefix + i)).join();
                var amount = diamonds(user.getStashData().getItems());
                if (amount == expected) ok++;
                else {
                    wrong++;
                    example = prefix + i + "=" + amount;
                }
            }
            log("verify prefix=" + prefix + " ok=" + ok + " wrong=" + wrong + " " + example);
        });
    }

    private static double pct(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        var idx = Math.min(sorted.size() - 1, (int) Math.ceil(p / 100.0 * sorted.size()) - 1);
        return sorted.get(Math.max(0, idx)) / 1_000_000.0;
    }

    private javax.sql.DataSource dataSource() throws Exception {
        var storage = um().getStorage();
        var field = storage.getClass().getDeclaredField("dataSource");
        field.setAccessible(true);
        return (javax.sql.DataSource) field.get(storage);
    }

    private String flag(UUID uuid) {
        try (var c = dataSource().getConnection(); var st = c.prepareStatement("SELECT 1 FROM aurora_sync WHERE player_uuid=?")) {
            st.setString(1, uuid.toString());
            try (var rs = st.executeQuery()) {
                return rs.next() ? "yes" : "no";
            }
        } catch (NoSuchFieldException e) {
            return "n/a";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }

    private String flagCount() {
        try (var c = dataSource().getConnection(); var st = c.prepareStatement("SELECT COUNT(*) FROM aurora_sync"); var rs = st.executeQuery()) {
            rs.next();
            return String.valueOf(rs.getInt(1));
        } catch (NoSuchFieldException e) {
            return "n/a";
        } catch (Exception e) {
            return "error:" + e.getMessage();
        }
    }
}
