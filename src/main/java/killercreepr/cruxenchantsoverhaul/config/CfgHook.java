package killercreepr.cruxenchantsoverhaul.config;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import killercreepr.crux.api.data.Holder;
import killercreepr.crux.api.registry.KeyedRegistry;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.crux.core.Crux;
import killercreepr.cruxadvancements.api.advancement.objective.AdvancementObjective;
import killercreepr.cruxadvancements.core.advancement.objective.ObjectiveCommonData;
import killercreepr.cruxadvancements.core.config.CruxAdvanceCfgData;
import killercreepr.cruxadvancements.core.config.handler.FileAdvancementObjective;
import killercreepr.cruxadvancements.core.config.handler.FileSimpleAdvanceObjective;
import killercreepr.cruxconfig.config.common.FileContext;
import killercreepr.cruxconfig.config.common.element.FileObject;
import killercreepr.cruxconfig.config.registry.CfgRegistries;
import killercreepr.cruxcore.CruxCore;
import killercreepr.cruxenchantsoverhaul.CruxEnchantsOverhaul;
import killercreepr.cruxenchantsoverhaul.advancement.objective.TakeEnchantedItemObjective;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEIngredientCalculator;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEnchant;
import killercreepr.cruxenchantsoverhaul.config.handler.FileEEIngredientCalculator;
import killercreepr.cruxenchantsoverhaul.config.handler.FileEEnchant;
import killercreepr.cruxenchantsoverhaul.tag.EnchDataTag;
import killercreepr.cruxmenus.CruxMenusModule;
import killercreepr.cruxmenus.api.menu.CfgMenu;
import killercreepr.cruxmenus.api.menu.config.handler.FileMenuHolder;
import killercreepr.cruxmenus.api.menu.holder.MenuItems;
import killercreepr.cruxmenus.api.menu.module.MenuModule;
import killercreepr.cruxmenus.api.menu.module.config.MenuModuleBuilder;
import killercreepr.cruxmenus.core.menu.module.standard.SimpleFilePagedCfg;
import killercreepr.cruxmenus.core.menu.module.standard.SimplePagedMenuModule;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CfgHook {
    private static final FileAdvancementObjective fileAdvancementObjective = CruxAdvanceCfgData.fileAdvancementObjective();
    public static void load() {
        CfgRegistries.SIMPLE_REGISTRY.forEach(reg ->{
            reg.registerFileHandler(EEnchant.class, new FileEEnchant());
            reg.registerFileHandler(EEIngredientCalculator.class, new FileEEIngredientCalculator());
        });

        fileAdvancementObjective.registerCustomHandler(new FileSimpleAdvanceObjective<>(CruxEnchantsOverhaul.inst().key("take_enchanted_item")) {

            @Override
            public @Nullable AdvancementObjective deserializeFromFile(@NotNull FileContext<?> ctx, @NotNull FileObject e, @NotNull ObjectiveCommonData data) {
                Integer maxProgress = e.getObject(Integer.class, "amount");
                if (maxProgress == null) maxProgress = 1;
                return new TakeEnchantedItemObjective(data, maxProgress);
            }
        });
        CruxMenusModule menusModule = CruxCore.inst().cruxMenus();
        registerMenuModules(menusModule.menuRegistry().menuModule(), menusModule.menuModuleRegistry());
    }

    public static void registerMenuModules(@NotNull FileMenuHolder<?> fileMenuHolder, @NotNull KeyedRegistry<MenuModuleBuilder> registry) {
        registry.register(new SimpleFilePagedCfg(fileMenuHolder, Crux.key("paged/enchant_overhaul/data")) {
            @NotNull
            @Override
            public MenuModule parsePaged(@NotNull String id,
                                         @NotNull NumberProvider indexes,
                                         @Nullable String valuesFilter,
                                         @Nullable MenuItems valueItems,
                                         @Nullable MenuItems emptyItems) {
                return new SimplePagedMenuModule<EnchDataTag.Data>(id, indexes, valuesFilter, valueItems, emptyItems, this) {
                    @Override
                    public @NotNull Holder<List<EnchDataTag.Data>> getValues(@NotNull CfgMenu cfgMenu) {
                        return () -> {
                            List<EnchDataTag.Data> list = new ArrayList<>();
                            for(Enchantment ench : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)){
                                list.add(new EnchDataTag.Data(ench.key()));
                            }
                            return list;
                        };
                    }
                };
            }
        });
    }
}
