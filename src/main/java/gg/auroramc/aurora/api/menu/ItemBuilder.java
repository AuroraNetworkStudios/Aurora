package gg.auroramc.aurora.api.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.auroramc.aurora.Aurora;
import gg.auroramc.aurora.api.config.premade.*;
import gg.auroramc.aurora.api.item.TypeId;
import gg.auroramc.aurora.api.message.Placeholder;
import gg.auroramc.aurora.api.message.Text;
import gg.auroramc.aurora.api.util.BukkitPotionType;
import gg.auroramc.aurora.api.util.Version;
import gg.auroramc.aurora.expansions.item.ItemExpansion;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

public class ItemBuilder {
    @Getter
    private final ItemConfig config;

    @Getter
    private final List<Placeholder<?>> placeholders = new ArrayList<>();
    private Supplier<List<Component>> loreBuilder = null;
    private final Collection<PotionEffect> potionEffects = new ArrayList<>();
    private Color potionColor = null;
    private ItemStack item = null;
    private PlayerProfile playerProfile;
    private static final Map<String, String> skinCache = new HashMap<>();
    private static final Map<String, com.destroystokyo.paper.profile.PlayerProfile> profileCache = new HashMap<>();

    private ItemBuilder(ItemConfig config) {
        this.config = new ItemConfig(config);
    }

    private ItemBuilder(ItemConfig config, ItemStack item) {
        this.config = new ItemConfig(config);
        this.item = item.clone();
    }

    public static ItemBuilder of(ItemConfig config) {
        return new ItemBuilder(config);
    }

    public static ItemBuilder close(ItemConfig config) {
        return new ItemBuilder(config).defaultMaterial(Material.BARRIER).defaultSlot(53);
    }

    public static ItemBuilder back(ItemConfig config) {
        return new ItemBuilder(config).defaultMaterial(Material.ARROW).defaultSlot(45);
    }

    public static ItemBuilder item(ItemStack item) {
        return new ItemBuilder(new ItemConfig(), item);
    }

    public static ItemStack filler(Material material, String name) {
        if (material == Material.AIR) return new ItemStack(Material.AIR);
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Text.component(name));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler(Material material) {
        return filler(material, " ");
    }

    public static ItemStack filler() {
        return filler(Material.BLACK_STAINED_GLASS_PANE);
    }

    public static ItemStack fromType(TypeId typeId, @Nullable Player player) {
        return Aurora.getExpansionManager().getExpansion(ItemExpansion.class).getItemManager().resolveItem(typeId, player);
    }

    public ItemBuilder enableRefreshing() {
        config.setRefresh(true);
        return this;
    }

    public ItemBuilder disableRefreshing() {
        config.setRefresh(false);
        return this;
    }

    public ItemBuilder defaultMaterial(Material material) {
        if (config.getMaterial() == null) {
            config.setMaterial(material.name());
        }
        return this;
    }

    public ItemBuilder potionColor(Color color) {
        this.potionColor = color;
        return this;
    }

    public ItemBuilder addPotionEffect(PotionEffect effect) {
        this.potionEffects.add(effect);
        return this;
    }

    public ItemBuilder potionEffect(Collection<PotionEffect> effects) {
        this.potionEffects.addAll(effects);
        return this;
    }

    public ItemBuilder material(Material material) {
        config.setMaterial(material.name());
        return this;
    }

    public ItemBuilder defaultAmount(int amount) {
        if (config.getAmount() == null) {
            config.setAmount(amount);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        config.setAmount(amount);
        return this;
    }

    public ItemBuilder setName(String name) {
        config.setName(name);
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        config.setLore(lore);
        return this;
    }

    public ItemBuilder loreCompute(Supplier<List<Component>> builder) {
        loreBuilder = builder;
        return this;
    }

    public ItemBuilder defaultSlot(int slot) {
        if (config.getSlot() == null || config.getSlot() < 0) {
            config.setSlot(slot);
        }
        return this;
    }

    public ItemBuilder slot(int slot) {
        config.setSlot(slot);
        return this;
    }

    public ItemBuilder flag(ItemFlag flag) {
        config.getFlags().add(flag.name());
        return this;
    }

    public ItemBuilder flag(ItemFlag... flags) {
        for (var flag : flags) {
            config.getFlags().add(flag.name());
        }
        return this;
    }

    public ItemBuilder flag(String flag) {
        config.getFlags().add(flag);
        return this;
    }

    public ItemBuilder flag(String... flags) {
        for (var flag : flags) {
            config.getFlags().add(flag);
        }
        return this;
    }

    public ItemBuilder setPlayerHead(Player player) {
        config.setMaterial(Material.PLAYER_HEAD.name());
        playerProfile = player.getPlayerProfile();
        return this;
    }

    public ItemBuilder skullUrl(String url) {
        config.setMaterial(Material.PLAYER_HEAD.name());
        if (config.getSkull() == null) {
            config.setSkull(new SkullConfig());
        }
        config.getSkull().setUrl(url);
        return this;
    }

    public ItemBuilder skullBase64(String base64) {
        config.setMaterial(Material.PLAYER_HEAD.name());
        if (config.getSkull() == null) {
            config.setSkull(new SkullConfig());
        }
        config.getSkull().setBase64(base64);
        return this;
    }

    public ItemBuilder placeholder(Placeholder<?> placeholder) {
        this.placeholders.add(placeholder);
        return this;
    }

    public ItemBuilder placeholder(List<Placeholder<?>> placeholders) {
        this.placeholders.addAll(placeholders);
        return this;
    }

    public MenuItem build(Player player) {
        if (item == null) {
            return new MenuItem(player, this, toItemStack(player));
        } else {
            return new MenuItem(player, this, toItemStack(item, player));
        }
    }

    public ItemStack toItemStack(Player player) {
        ItemStack item;

        if (config.getMaterial().contains(":")) {
            item = fromType(TypeId.fromDefault(config.getMaterial()), player);
        } else if (config.getMaterial().equalsIgnoreCase("self_head")) {
            setPlayerHead(player);
            item = new ItemStack(Material.PLAYER_HEAD);
        } else if (config.getMaterial().equalsIgnoreCase("main_hand")) {
            item = player.getInventory().getItemInMainHand().clone();
        } else {
            item = new ItemStack(Material.valueOf(config.getMaterial().toUpperCase()));
        }

        if (item.getType() == Material.AIR) {
            return item;
        }

        return toItemStack(item, player);
    }

    public ItemStack toItemStack(ItemStack item, Player player) {
        item.setAmount(Math.max(config.getAmount(), 1));

        var meta = item.getItemMeta();

        if (Version.isAtLeastVersion(20, 5)) {
            if (config.getHideTooltip() != null && config.getHideTooltip()) {
                meta.setHideTooltip(true);
            }
        }

        var placeholders = this.placeholders.toArray(Placeholder[]::new);

        if (config.getName() != null) {
            meta.displayName(Text.component(player, config.getName(), placeholders));
        }

        if (!config.getLore().isEmpty()) {
            meta.lore(config.getLore().stream().map(l -> Text.component(player, l, placeholders)).toList());
        }

        if (loreBuilder != null) {
            meta.lore(loreBuilder.get());
        }

        if (config.getAppendLore() != null && !config.getAppendLore().isEmpty()) {
            var lore = meta.lore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.addAll(config.getAppendLore().stream().map(l -> Text.component(player, l, placeholders)).toList());
            meta.lore(lore);
        }

        if (config.getDurability() != null && meta instanceof Damageable damageable) {
            damageable.setDamage(config.getDurability());
        }

        if (config.getCustomModelData() != null) {
            meta.setCustomModelData(config.getCustomModelData());
        }

        for (var enchant : config.getEnchantments().entrySet()) {
            var key = NamespacedKey.fromString(enchant.getKey());
            if (key == null) continue;

            var enchantment = Registry.ENCHANTMENT.get(key);

            if (enchantment != null) {
                meta.addEnchant(enchantment, enchant.getValue(), true);
            }
        }

        if (config.getFlags() != null) {
            for (var flag : config.getFlags()) {
                var pFlag = ItemFlag.valueOf(flag.toUpperCase(Locale.ROOT));

                if (pFlag == ItemFlag.HIDE_ATTRIBUTES && Version.isAtLeastVersion(20, 5)) {
                    meta.setAttributeModifiers(Material.IRON_SWORD.getDefaultAttributeModifiers(EquipmentSlot.HAND));
                }

                meta.addItemFlags(pFlag);
            }
        }

        if (meta instanceof SkullMeta skullMeta) {
            String url = config.getTexture();

            if (config.getSkull() != null) {
                if (config.getSkull().getUrl() != null) {
                    url = config.getSkull().getUrl();
                } else if (config.getSkull().getBase64() != null) {
                    url = decodeSkinUrl(config.getSkull().getBase64());
                }
            }

            if (url != null) {
                if (profileCache.containsKey(url)) {
                    skullMeta.setPlayerProfile(profileCache.get(url));
                } else {
                    var profile = Bukkit.createProfile(UUID.randomUUID());
                    try {
                        var textures = profile.getTextures();
                        textures.setSkin(URI.create(url).toURL());
                        profile.setTextures(textures);
                        skullMeta.setPlayerProfile(profile);
                        profileCache.put(url, profile);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (playerProfile != null) {
                skullMeta.setOwnerProfile(playerProfile);
            }
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (config.getPotion() != null) {
                var potion = new BukkitPotionType(PotionType.valueOf(config.getPotion().getType().toUpperCase()), config.getPotion().getExtended(), config.getPotion().getUpgraded());
                potion.applyToMeta(potionMeta);
            }
            if (!potionEffects.isEmpty()) {
                potionMeta.clearCustomEffects();
                for (PotionEffect effect : potionEffects) {
                    potionMeta.addCustomEffect(effect, true);
                }
            }
            if (potionColor != null) {
                potionMeta.setColor(potionColor);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    public static String decodeSkinUrl(String base64Texture) {
        if (skinCache.containsKey(base64Texture)) {
            return skinCache.get(base64Texture);
        }
        String decoded = new String(Base64.getDecoder().decode(base64Texture));
        JsonObject object = JsonParser.parseString(decoded).getAsJsonObject();
        JsonElement textures = object.get("textures");
        if (textures == null)
            return null;
        JsonElement skin = textures.getAsJsonObject().get("SKIN");
        if (skin == null)
            return null;

        JsonElement url = skin.getAsJsonObject().get("url");

        if (url != null) {
            skinCache.put(base64Texture, url.getAsString());
            return url.getAsString();
        }

        return null;
    }
}
