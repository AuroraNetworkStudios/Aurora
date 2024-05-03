package gg.auroramc.auroralib.api.config.premade;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.*;

@Setter
@Getter
public class ItemConfig {
    private Boolean refresh = false;
    private String name;
    private List<String> lore;
    private String material;
    private Integer customModelData;
    private String texture;
    private Integer slot;
    private Integer amount = 1;
    private Integer durability;
    private SkullConfig skull;
    private Set<String> flags;
    private PotionConfig potion;
    private Map<String, Integer> enchantments;
    private List<String> onClick;
    private List<String> onLeftClick;
    private List<String> onRightClick;

    public ItemConfig() {}

    public ItemConfig(ItemConfig other) {
        if(other == null) {
            this.enchantments = new HashMap<>();
            this.flags = new HashSet<>();
            this.lore = new ArrayList<>();
            this.onClick = new ArrayList<>();
            this.onLeftClick = new ArrayList<>();
            this.onRightClick = new ArrayList<>();
            return;
        };
        this.refresh = other.refresh;
        this.name = other.name;

        if(other.lore != null) {
            this.lore = new ArrayList<>(other.lore);
        } else {
            this.lore = new ArrayList<>();
        }

        this.material = other.material;
        this.customModelData = other.customModelData;
        this.texture = other.texture;
        this.slot = other.slot;
        this.amount = other.amount;
        this.durability = other.durability;

        if(skull != null) {
            this.skull = new SkullConfig(other.skull);
        }

        if(other.flags != null) {
            this.flags = new HashSet<>(other.flags);
        } else {
            this.flags = new HashSet<>();
        }

        if(other.potion != null) {
            this.potion = new PotionConfig(other.potion);
        }

        if(other.enchantments != null) {
            this.enchantments = new HashMap<>(other.enchantments);
        } else {
            this.enchantments = new HashMap<>();
        }

        if(other.onClick != null) {
            this.onClick = new ArrayList<>(other.onClick);
        } else {
            this.onClick = new ArrayList<>();
        }

        if(other.onLeftClick != null) {
            this.onLeftClick = new ArrayList<>(other.onLeftClick);
        } else {
            this.onLeftClick = new ArrayList<>();
        }

        if(other.onRightClick != null) {
            this.onRightClick = new ArrayList<>(other.onRightClick);
        } else {
            this.onRightClick = new ArrayList<>();
        }
    }
}
