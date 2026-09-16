package killercreepr.cruxenchantsoverhaul.config.handler;

import killercreepr.crux.api.component.parser.hybrid.PersistTextParser;
import killercreepr.crux.api.item.predicate.ItemPredicate;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.crux.core.component.parser.TextDataComponentDecoder;
import killercreepr.cruxconfig.config.common.FileContext;
import killercreepr.cruxconfig.config.common.FileRegistry;
import killercreepr.cruxconfig.config.common.element.FileElement;
import killercreepr.cruxconfig.config.common.element.FileObject;
import killercreepr.cruxconfig.config.common.handler.FileObjectHandler;
import killercreepr.cruxenchantsoverhaul.anvil.recipe.AnvilIngredient;
import killercreepr.cruxenchantsoverhaul.anvil.recipe.EntityTickableApplyAnvilIngredient;
import killercreepr.cruxenchantsoverhaul.anvil.recipe.SimpleAnvilIngredient;
import killercreepr.cruxtickables.api.entity.tickable.EntityTickableModifier;
import killercreepr.cruxtickables.core.component.CruxTickableCompParsers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class FileAnvilRepairIngredient implements FileObjectHandler<AnvilIngredient> {
    @Override
    public @NotNull FileElement serializeToFile(@NotNull FileContext<?> fileContext, @NotNull AnvilIngredient anvilIngredient) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable AnvilIngredient deserializeFromFile(@NotNull FileContext<?> ctx, @NotNull FileElement e) {
        if(!(e instanceof FileObject o)) return null;
        FileRegistry registry = ctx.getRegistry();

        String type = o.getOrDefaultObject(String.class, "type", "simple");
        switch (type.toLowerCase()){
            case "simple" ->{
                ItemPredicate ingredient = registry.deserializeFromFile(ItemPredicate.class, o.get("ingredient"));
                if(ingredient==null) return null;
                NumberProvider repairAmount = registry.deserializeFromFile(NumberProvider.class, o.get("repair_amount"));
                if(repairAmount==null) return null;
                return new SimpleAnvilIngredient(ingredient, repairAmount);
            }
            case "entity_tickable_apply" ->{
                ItemPredicate ingredient = registry.deserializeFromFile(ItemPredicate.class, o.get("ingredient"));
                if(ingredient==null) return null;
                NumberProvider repairAmount = registry.deserializeFromFile(NumberProvider.class, o.get("repair_amount"));
                if(repairAmount==null) return null;

                Collection<EntityTickableModifier> modifiers = PersistTextParser.collectionList(CruxTickableCompParsers.ENTITY_TICKABLE_MODIFIER)
                    .attemptDecodeObject(TextDataComponentDecoder.parseObject(
                        o.get("tickable_modifiers").getAsString()
                    ));

                return new EntityTickableApplyAnvilIngredient(ingredient, repairAmount, modifiers);
            }
        }
        return null;
    }
}
