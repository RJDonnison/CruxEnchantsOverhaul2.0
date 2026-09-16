package killercreepr.cruxenchantsoverhaul.api.enchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.cruxenchants.api.enchant.CruxEnchant;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

public interface EEnchant extends CruxEnchant {
    ItemStack getIcon();
    boolean canEnchantItem(ItemStack item);
    boolean conflictsWith(EEnchant enchant);
    boolean isDiscoverable();

    NumberProvider requiredPower();
    NumberProvider requiredLevel();
    NumberProvider requiredExp();
    NumberProvider requiredLapis();

    @ApiStatus.Experimental
    double quality();

    EEIngredientCalculator ingredientCalculator();

    Collection<Enchantment> conflictingEnchants();

    default Enchantment enchantment(){
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key());
    }
}
