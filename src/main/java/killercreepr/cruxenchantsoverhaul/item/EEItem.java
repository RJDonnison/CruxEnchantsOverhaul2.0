package killercreepr.cruxenchantsoverhaul.item;

import killercreepr.crux.api.text.format.FormatSerializer;
import killercreepr.cruxitems.core.item.SimpleCruxedItem;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

public class EEItem extends SimpleCruxedItem {
    public EEItem(@NotNull Material material) {
        super(material);
    }

    public EEItem(@NotNull ItemStack item) {
        super(item);
    }

    public EEItem(@NotNull FormatSerializer format, @NotNull Material material) {
        super(format, material);
    }

    public EEItem(@NotNull FormatSerializer format, @NotNull ItemStack item) {
        super(format, item);
    }

    public EEItem enchant(@NotNull Enchantment enchant, int level){
        enchant(enchant, level, true);
        return this;
    }

    public EEItem storageEnchant(@NotNull Enchantment enchant, int level){
        editMeta(EnchantmentStorageMeta.class, meta ->{
            meta.addStoredEnchant(enchant, level, true);
        });
        return this;
    }
}
