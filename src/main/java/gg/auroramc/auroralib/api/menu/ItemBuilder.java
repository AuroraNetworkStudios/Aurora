package gg.auroramc.auroralib.api.menu;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.auroramc.auroralib.api.config.premade.*;
import gg.auroramc.auroralib.api.message.Placeholder;
import gg.auroramc.auroralib.api.message.Text;
import gg.auroramc.auroralib.api.util.BukkitPotionType;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.net.URL;
import java.util.*;
import java.util.function.Function;

public class ItemBuilder {
    @Getter
    private final ItemConfig config;

    private final List<Placeholder> placeholders = new ArrayList<>();
    private Function<List<Placeholder>, List<Component>> loreBuilder = null;
    private final Collection<PotionEffect> potionEffects = new ArrayList<>();
    private Color potionColor = null;

    private ItemBuilder(ItemConfig config) {
        this.config = new ItemConfig(config);
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

    public ItemBuilder loreCompute(Function<List<Placeholder>, List<Component>> builder) {
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

    public ItemBuilder setPlayerHead(Player player) {
        config.setMaterial(Material.PLAYER_HEAD.name());
        if (config.getSkull() == null) {
            config.setSkull(new SkullConfig());
        }
        if (player.getPlayerProfile().getTextures().getSkin() == null) return this;
        config.getSkull().setUrl(player.getPlayerProfile().getTextures().getSkin().toString());
        return this;
    }

    public ItemBuilder placeholder(Placeholder placeholder) {
        this.placeholders.add(placeholder);
        return this;
    }

    public ItemBuilder placeholder(List<Placeholder> placeholders) {
        this.placeholders.addAll(placeholders);
        return this;
    }

    public MenuItem build(Player player) {
        var item = new ItemStack(Material.valueOf(config.getMaterial()));
        item.setAmount(Math.max(config.getAmount(), 1));

        var meta = item.getItemMeta();

        var placeholders = this.placeholders.toArray(Placeholder[]::new);

        if (config.getName() != null) {
            meta.displayName(Text.component(player, config.getName(), placeholders));
        }

        if (!config.getLore().isEmpty()) {
            meta.lore(config.getLore().stream().map(l -> Text.component(player, l, placeholders)).toList());
        }

        if (loreBuilder != null) {
            meta.lore(loreBuilder.apply(this.placeholders));
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
                meta.addItemFlags(ItemFlag.valueOf(flag));
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
                var profile = Bukkit.createProfile(UUID.randomUUID());
                try {
                    profile.getTextures().setSkin(new URL(url));
                    skullMeta.setPlayerProfile(profile);
                } catch (Exception ignored) {
                }
            }
        }

        if (meta instanceof PotionMeta potionMeta) {
            if (config.getPotion() != null) {
                var potion = new BukkitPotionType(PotionType.valueOf(config.getPotion().getType()), config.getPotion().getExtended(), config.getPotion().getUpgraded());
                potion.applyToMeta(potionMeta);
            }
            if (!potionEffects.isEmpty()) {
                potionMeta.clearCustomEffects();
                for (PotionEffect effect : potionEffects) {
                    potionMeta.addCustomEffect(effect, true);
                }
            }
            if(potionColor != null) {
                potionMeta.setColor(potionColor);
            }
        }

        item.setItemMeta(meta);

        return new MenuItem(player, this, item, config.getSlot());
    }

    public static String decodeSkinUrl(String base64Texture) {
        String decoded = new String(Base64.getDecoder().decode(base64Texture));
        JsonObject object = JsonParser.parseString(decoded).getAsJsonObject();
        JsonElement textures = object.get("textures");
        if (textures == null)
            return null;
        JsonElement skin = textures.getAsJsonObject().get("SKIN");
        if (skin == null)
            return null;
        JsonElement url = skin.getAsJsonObject().get("url");
        return (url == null) ? null : url.getAsString();
    }
}
