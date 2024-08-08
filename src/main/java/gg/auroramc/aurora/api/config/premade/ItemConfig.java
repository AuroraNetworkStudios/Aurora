package gg.auroramc.aurora.api.config.premade;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class ItemConfig {
    private boolean refresh = false;
    private Boolean hideTooltip;
    private String name;
    private List<String> lore;
    private String material;
    private Integer customModelData;
    private String texture;
    private Integer slot;
    private List<Integer> slots;
    private Integer amount = 1;
    private Integer durability;
    private SkullConfig skull;
    private Set<String> flags;
    private PotionConfig potion;
    private Map<String, Integer> enchantments;
    private List<String> onClick;
    private List<String> onLeftClick;
    private List<String> onRightClick;

    public ItemConfig() {
    }

    public ItemConfig merge(ItemConfig other) {
        if (other == null) return this;
        var ret = new ItemConfig(this);

        ret.refresh = other.refresh;
        if (other.name != null) ret.name = other.name;

        if (other.lore != null && !other.lore.isEmpty()) {
            ret.lore = new ArrayList<>(other.lore);
        }

        if (other.hideTooltip != null) ret.hideTooltip = other.hideTooltip;

        if (other.material != null) ret.material = other.material;
        if (other.customModelData != null) ret.customModelData = other.customModelData;
        if (other.texture != null) ret.texture = other.texture;
        if (other.slot != null) ret.slot = other.slot;
        if (other.slots != null && !other.slots.isEmpty()) {
            ret.slots = new ArrayList<>(other.slots);
        }
        if (other.amount != null) ret.amount = other.amount;
        if (other.durability != null) ret.durability = other.durability;

        if (other.skull != null) {
            ret.skull = new SkullConfig(other.skull);
        }

        if (other.flags != null && !other.flags.isEmpty()) {
            ret.flags = new HashSet<>(other.flags);
        }

        if (other.potion != null) {
            ret.potion = new PotionConfig(other.potion);
        }

        if (other.enchantments != null && !other.enchantments.isEmpty()) {
            ret.enchantments = new HashMap<>(other.enchantments);
        }

        if (other.onClick != null && !other.onClick.isEmpty()) {
            ret.onClick = new ArrayList<>(other.onClick);
        }

        if (other.onLeftClick != null && !other.onLeftClick.isEmpty()) {
            ret.onLeftClick = new ArrayList<>(other.onLeftClick);
        }

        if (other.onRightClick != null && !other.onRightClick.isEmpty()) {
            ret.onRightClick = new ArrayList<>(other.onRightClick);
        }

        return ret;
    }

    public ItemConfig(ItemConfig other) {
        if (other == null) {
            this.enchantments = new HashMap<>();
            this.flags = new HashSet<>();
            this.lore = new ArrayList<>();
            this.onClick = new ArrayList<>();
            this.onLeftClick = new ArrayList<>();
            this.onRightClick = new ArrayList<>();
            return;
        }
        this.refresh = other.refresh;
        this.name = other.name;

        if (other.lore != null) {
            this.lore = new ArrayList<>(other.lore);
        } else {
            this.lore = new ArrayList<>();
        }

        this.hideTooltip = other.hideTooltip;

        this.material = other.material;
        this.customModelData = other.customModelData;
        this.texture = other.texture;
        this.slot = other.slot;
        if (other.slots != null) {
            this.slots = new ArrayList<>(other.slots);
        } else {
            this.slots = new ArrayList<>();
        }
        this.amount = other.amount;
        this.durability = other.durability;

        if (other.skull != null) {
            this.skull = new SkullConfig(other.skull);
        }

        if (other.flags != null) {
            this.flags = new HashSet<>(other.flags);
        } else {
            this.flags = new HashSet<>();
        }

        if (other.potion != null) {
            this.potion = new PotionConfig(other.potion);
        }

        if (other.enchantments != null) {
            this.enchantments = new HashMap<>(other.enchantments);
        } else {
            this.enchantments = new HashMap<>();
        }

        if (other.onClick != null) {
            this.onClick = new ArrayList<>(other.onClick);
        } else {
            this.onClick = new ArrayList<>();
        }

        if (other.onLeftClick != null) {
            this.onLeftClick = new ArrayList<>(other.onLeftClick);
        } else {
            this.onLeftClick = new ArrayList<>();
        }

        if (other.onRightClick != null) {
            this.onRightClick = new ArrayList<>(other.onRightClick);
        } else {
            this.onRightClick = new ArrayList<>();
        }
    }
}
