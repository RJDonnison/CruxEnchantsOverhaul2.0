package killercreepr.cruxenchantsoverhaul.anvil.recipe;

import killercreepr.crux.api.item.CruxItem;
import killercreepr.crux.api.item.predicate.ItemPredicate;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.cruxtickables.api.entity.tickable.EntityTickableModifier;
import killercreepr.cruxtickables.api.entity.tickable.EntityTickablesContainer;
import killercreepr.cruxtickables.core.component.CruxTickableComponents;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class EntityTickableApplyAnvilIngredient extends SimpleAnvilIngredient {
    protected final Collection<EntityTickableModifier> modifiers;
    public EntityTickableApplyAnvilIngredient(@NotNull ItemPredicate ingredient, @NotNull NumberProvider repairAmount, Collection<EntityTickableModifier> modifiers) {
        super(ingredient, repairAmount);
        this.modifiers = modifiers;
    }

    public boolean hasAnyModifiers(Collection<EntityTickableModifier> container){
        for(EntityTickableModifier check : container){
            for(EntityTickableModifier set : modifiers){
                if(check.key().equals(set.key())) return true;
            }
        }
        return false;
    }

    @Override
    public boolean canApplyTo(ItemStack first) {
        if(CruxItem.isEmpty(first)) return false;
        CruxItem crux = CruxItem.wrap(first);
        EntityTickablesContainer container = crux.getOrDefaultData(CruxTickableComponents.ENTITY_TICKABLES);
        if(container == null) return true;
        return !hasAnyModifiers(container.getTickableModifiers());
    }

    @Override
    public CruxItem applyToResult(CruxItem result) {
        EntityTickablesContainer container = result.getOrDefaultData(CruxTickableComponents.ENTITY_TICKABLES);
        Collection<EntityTickableModifier> modifiers = new ArrayList<>(container == null ? Set.of() : container.getTickableModifiers());
        modifiers.addAll(this.modifiers);

        result.set(CruxTickableComponents.ENTITY_TICKABLES, EntityTickablesContainer.container(modifiers));

        return super.applyToResult(result);
    }
}
