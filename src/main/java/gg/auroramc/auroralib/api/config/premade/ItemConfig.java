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
        this.refresh = other.refresh;
        this.name = other.name;
        this.lore = new ArrayList<>(other.lore);
        this.material = other.material;
        this.customModelData = other.customModelData;
        this.texture = other.texture;
        this.slot = other.slot;
        this.amount = other.amount;
        this.durability = other.durability;
        this.skull = new SkullConfig(other.skull);
        this.flags = new HashSet<>(other.flags);
        this.potion = new PotionConfig(other.potion);
        this.enchantments = new HashMap<>(other.enchantments);
        this.onClick = new ArrayList<>(other.onClick);
        this.onLeftClick = new ArrayList<>(other.onLeftClick);
        this.onRightClick = new ArrayList<>(other.onRightClick);
    }
}
