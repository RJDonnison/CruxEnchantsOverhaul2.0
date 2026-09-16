package killercreepr.cruxenchantsoverhaul.enchanting;

import killercreepr.crux.core.data.util.EnchantPair;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEnchant;
import killercreepr.cruxenchantsoverhaul.registries.EnchantsRegistries;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class EEnchanter implements Enchanter {
    public EEnchanter() {
    }

    private boolean canApply(ItemStack item, Enchantment ench, int level){
        EEnchant ee = EnchantsRegistries.EENCHANT.get(ench.key());
        int max = (ee != null) ? ee.maxLevel() : ench.getMaxLevel();
        if(level > max) return false;

        boolean book = item.getItemMeta() instanceof EnchantmentStorageMeta;
        if(!book && !ench.canEnchantItem(item)) return false;

        for(Map.Entry<Enchantment, Integer> existing : item.getEnchantments().entrySet()){
            Enchantment ex = existing.getKey();
            if(!ex.equals(ench) && ench.conflictsWith(ex)) return false;
        }
        return true;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item, @NotNull Map<Enchantment, Integer> enchants) {
        for(var entry : enchants.entrySet()){
            if(!canApply(item, entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item, @NotNull Collection<EnchantPair> enchants) {
        for(EnchantPair pair : enchants){
            if(!canApply(item, pair.getEnchant(), pair.getLevel())) return false;
        }
        return true;
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item, @NotNull EnchantPair[] enchants) {
        return canEnchantItem(item, Arrays.asList(enchants));
    }

    @Override
    public boolean canEnchantItem(@NotNull ItemStack item, @NotNull Enchantment enchant, int level) {
        return canApply(item, enchant, level);
    }

    @Override
    public boolean canSetEnchantments(@NotNull ItemStack item, @NotNull Map<Enchantment, Integer> enchants) {
        for(var entry : enchants.entrySet()){
            if(!canApply(item, entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }
}
