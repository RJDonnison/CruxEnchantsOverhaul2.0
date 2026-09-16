package killercreepr.cruxenchantsoverhaul.tag;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import killercreepr.crux.api.text.format.FormatPrefix;
import killercreepr.crux.api.text.hook.ObjectTag;
import killercreepr.crux.api.text.resolver.StringResolver;
import killercreepr.crux.api.text.tags.TagParser;
import killercreepr.crux.api.text.tags.container.TagContainer;
import killercreepr.crux.core.text.resolver.Tag;
import killercreepr.crux.core.util.CruxString;
import killercreepr.cruxenchants.api.enchant.CruxEnchant;
import killercreepr.cruxenchants.core.registries.CruxEnchantRegistries;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EnchDataTag implements ObjectTag<EnchDataTag.Data> {
    public static class Data{
        protected final Key enchant;

        public Data(Key enchant) {
            this.enchant = enchant;
        }

        public Enchantment ench(){
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                .get(enchant);
        }

        public String name(){
            CruxEnchant ee = CruxEnchantRegistries.CRUX_ENCHANT.get(enchant);
            if(ee == null) return CruxString.toTitleCase(enchant.value());
            return ee.displayName();
        }
    }

    @Override
    public @NotNull Class<Data> getObjectType() {
        return Data.class;
    }

    @Override
    public @NotNull FormatPrefix defaultPrefix() {
        return FormatPrefix.simple("ench_data_");
    }


    @Override
    public @Nullable TagContainer<StringResolver> requestStrings(@NotNull Data object, @NotNull TagParser tags) {
        return TagContainer.string(tags)
            .add(Tag.string("name", (args, ctx) -> object.name()))
            ;
    }
}
