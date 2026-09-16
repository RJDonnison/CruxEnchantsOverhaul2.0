package killercreepr.cruxenchantsoverhaul.core.enchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import killercreepr.crux.api.component.DataComponentHandler;
import killercreepr.crux.api.item.dynamic.DynamicItem;
import killercreepr.crux.api.text.context.TextParserContext;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.crux.core.util.CruxString;
import killercreepr.cruxenchants.api.enchant.ApplicableItemGroup;
import killercreepr.cruxenchants.api.enchant.CruxEnchant;
import killercreepr.cruxenchants.core.enchant.SimpleCruxEnchant;
import killercreepr.cruxenchants.core.registries.CruxEnchantRegistries;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEIngredientCalculator;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEnchant;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashSet;

public class SimpleEEnchant extends SimpleCruxEnchant implements EEnchant {
    protected final DynamicItem icon;
    protected final EEIngredientCalculator ingredientCalculator;
    protected Collection<Enchantment> conflictingEnchants;
    protected final NumberProvider requiredPower;
    protected final NumberProvider requiredLevel;
    protected final NumberProvider requiredExp;
    protected final NumberProvider requiredLapis;
    protected final double quality;
    protected final boolean discoverable;
    public SimpleEEnchant(Key key, String description, ApplicableItemGroup applicableItemGroup, DataComponentHandler components, DynamicItem icon, EEIngredientCalculator ingredientCalculator, NumberProvider requiredPower, NumberProvider requiredLevel, NumberProvider requiredExp, NumberProvider requiredLapis, double quality, boolean discoverable) {
        super(key, description, applicableItemGroup, components);
        this.icon = icon;
        this.ingredientCalculator = ingredientCalculator;
        this.requiredPower = requiredPower;
        this.requiredLevel = requiredLevel;
        this.requiredExp = requiredExp;
        this.requiredLapis = requiredLapis;
        this.quality = quality;
        this.discoverable = discoverable;
    }

    @Override
    public String description() {
        if(description == null){
            CruxEnchant cruxEnchant = CruxEnchantRegistries.CRUX_ENCHANT.get(key);
            if(cruxEnchant != null && cruxEnchant != this) return cruxEnchant.description();
        }
        return super.description();
    }

    @Override
    public ItemStack getIcon() {
        return icon.build(TextParserContext.empty())
            .customName("<!i><white>" + displayName())
            .editThis(crux ->{
                if(description() != null){
                    crux.addLoreFromString(CruxString.buildDescription(
                        description(), CruxString.DEFAULT_DESCRIPTION_MAX_LENGTH,
                        s -> "<gray>" + s
                    ));
                }
            })
            .item();
    }

    @Override
    public boolean canEnchantItem(ItemStack item) {
        return enchantment().canEnchantItem(item);
    }

    @Override
    public boolean conflictsWith(EEnchant enchant) {
        return enchantment().conflictsWith(enchant.enchantment());
    }

    @Override
    public boolean isDiscoverable() {
        return discoverable;
    }

    @Override
    public NumberProvider requiredPower() {
        return requiredPower;
    }

    @Override
    public NumberProvider requiredLevel() {
        return requiredLevel;
    }

    @Override
    public NumberProvider requiredExp() {
        return requiredExp;
    }

    @Override
    public NumberProvider requiredLapis() {
        return requiredLapis;
    }

    @Override
    public double quality() {
        return quality;
    }

    @Override
    public EEIngredientCalculator ingredientCalculator() {
        return ingredientCalculator;
    }

    @Override
    public Collection<Enchantment> conflictingEnchants() {
        if(conflictingEnchants != null) return conflictingEnchants;
        conflictingEnchants = new HashSet<>();
        Enchantment ench = enchantment();
        for(Enchantment enchantment : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)){
            if(ench == enchantment) continue;
            if(!ench.conflictsWith(enchantment)) continue;
            conflictingEnchants.add(enchantment);
        }
        return conflictingEnchants;
    }
}
