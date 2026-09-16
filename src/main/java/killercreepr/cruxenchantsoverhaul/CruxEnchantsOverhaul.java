package killercreepr.cruxenchantsoverhaul;

import com.google.common.reflect.TypeToken;
import killercreepr.crux.api.data.DataExchange;
import killercreepr.crux.api.text.tags.container.MergedTagContainer;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.crux.core.Crux;
import killercreepr.crux.core.plugin.CruxPlugin;
import killercreepr.crux.core.plugin.module.StandardModules;
import killercreepr.crux.core.registries.CruxRegistries;
import killercreepr.cruxconfig.config.bukkit.file.BukkitDataFile;
import killercreepr.cruxconfig.config.bukkit.file.CruxFolder;
import killercreepr.cruxconfig.config.common.FileContext;
import killercreepr.cruxconfig.config.common.base.parsed.FileParsedObjectHandler;
import killercreepr.cruxconfig.config.common.element.FileElement;
import killercreepr.cruxconfig.config.common.file.DataFile;
import killercreepr.cruxconfig.config.registry.CfgRegistries;
import killercreepr.cruxcore.CruxCore;
import killercreepr.cruxenchantsoverhaul.anvil.recipe.AnvilIngredient;
import killercreepr.cruxenchantsoverhaul.anvil.recipe.AnvilRecipe;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEnchant;
import killercreepr.cruxenchantsoverhaul.block.CustomBlocks;
import killercreepr.cruxenchantsoverhaul.config.CfgHook;
import killercreepr.cruxenchantsoverhaul.config.Config;
import killercreepr.cruxenchantsoverhaul.config.handler.FileAnvilRecipe;
import killercreepr.cruxenchantsoverhaul.config.handler.FileAnvilRepairIngredient;
import killercreepr.cruxenchantsoverhaul.config.handler.FileEEnchant;
import killercreepr.cruxenchantsoverhaul.enchanting.EEnchanter;
import killercreepr.cruxenchantsoverhaul.enchanting.Enchanter;
import killercreepr.cruxenchantsoverhaul.listener.AnvilListener;
import killercreepr.cruxenchantsoverhaul.listener.CustomObjectiveListener;
import killercreepr.cruxenchantsoverhaul.menu.enchanting.EnchantTableMenu;
import killercreepr.cruxenchantsoverhaul.registries.EnchantsRegistries;
import killercreepr.cruxenchantsoverhaul.tag.EnchDataTag;
import killercreepr.cruxmenus.api.menu.holder.MenuHolder;
import killercreepr.cruxmenus.core.menu.ConfigMenu;
import killercreepr.cruxmenus.core.menu.holder.SimpleMenuHolder;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CruxEnchantsOverhaul extends CruxPlugin {
    private static CruxEnchantsOverhaul instance;
    public static CruxEnchantsOverhaul inst(){ return instance; }

    protected Config values;

    public Config values() {
        return values;
    }

    public void values(@NotNull Config values) {
        this.values = values;
    }

    protected Enchanter enchanter;

    public Enchanter getEnchanter() {
        return enchanter;
    }

    public void setEnchanter(Enchanter enchanter) {
        this.enchanter = enchanter;
    }

    @Override
    public void onLoad() {
        instance = this;
        super.onLoad();
        Crux.tags().register(
            new EnchDataTag()
        );
        if(CruxRegistries.MODULES.containsKey(StandardModules.CRUX_CONFIGS)){
            CfgRegistries.SIMPLE_REGISTRY.forEach(registry ->{
                registry.registerFileHandler(AnvilRecipe.class, new FileAnvilRecipe());
                registry.registerFileHandler(AnvilIngredient.class, new FileAnvilRepairIngredient());
            });
        }

        CfgRegistries.SIMPLE_REGISTRY.forEach(registry ->{
            registry.getParsedObjectRegistry().register(new FileParsedObjectHandler<MenuHolder>() {
                @Override
                public @NotNull Key key() {
                    return Crux.key("enchanting_table_menu");
                }

                @Override
                public int getPriority() {
                    return 0;
                }

                @Override
                public @NotNull Class<MenuHolder> getTargetType() {
                    return MenuHolder.class;
                }

                @Override
                public @Nullable MenuHolder parse(@NotNull FileElement element, @NotNull FileContext<?> ctx,
                                                  @NotNull MenuHolder original, @Nullable MenuHolder current) {
                    if(current==null || !current.key().namespace().equals(Crux.NAMESPACE)) return null;

                    switch (current.key().value()){
                        case "enchanting_table" ->{
                            return new SimpleMenuHolder(current.key(), current.getTitle(), current.getSize(), current.getItems(), current.info(), current.getModules()){
                                @Override
                                public @NotNull ConfigMenu createMenu(@NotNull DataExchange data, @Nullable MergedTagContainer tags) {
                                    return new EnchantTableMenu(this, data, tags);
                                }
                            };
                        }
                    }
                    return current;
                }
            });
        });
        if(CruxRegistries.MODULES.containsKey(StandardModules.CRUX_CONFIGS)){
            CfgHook.load();
        }

        CustomBlocks.register();
    }

    @Override
    public void enabled() {

        registerListeners(
            new AnvilListener()
        );
        if(CruxRegistries.MODULES.containsKey(StandardModules.CRUX_ADVANCEMENTS)){
            registerListeners(
                new CustomObjectiveListener()
            );
        }

        saveResource("anvil.yml");
        saveResource("config.yml");
        saveResource("eenchants.json");
        saveResourceFolder("menus");
        if(CruxRegistries.MODULES.containsKey(StandardModules.CRUX_CONFIGS)){
            values(new Config(this, "config"));
        }else{
            throw new IllegalStateException("CruxConfigs needs to be installed");
            //values(new DefaultValues());
        }
        setEnchanter(new EEnchanter());

        super.enabled();
    }

    @Override
    public void reload() {
        super.reload();
        values.reload(this);
        var anvil = BukkitDataFile.parseFromGeneralPath(
          CruxFolder.file(this, "anvil"), false
        );
        if(anvil != null){
            Collection<AnvilRecipe> recipes = anvil.deserializeOrDefault("anvil_recipes", new TypeToken<Collection<AnvilRecipe>>(){}.getType(), List.of());
            recipes.forEach(recipe ->{
                EnchantsRegistries.ANVIL_RECIPES.register(recipe);
                log("Registered anvil recipe: " + recipe.key());
            });
            anvil.close();
        }
        CruxCore.inst().cruxMenus().menuRegistry().loadConfiguration(
            new CruxFolder(this, "menus").file()
        );

        DataFile eeCfg = BukkitDataFile.parseFromGeneralPath(CruxFolder.file(this, "eenchants"));
        if(eeCfg != null){
            FileEEnchant.defaultRequiredLevel = eeCfg.deserialize("default_required_level", NumberProvider.class);
            FileEEnchant.defaultRequiredPower = eeCfg.deserialize("default_required_power", NumberProvider.class);
            FileEEnchant.defaultRequiredExp = eeCfg.deserialize("default_required_exp", NumberProvider.class);
            FileEEnchant.defaultRequiredLapis = eeCfg.deserialize("default_required_lapis", NumberProvider.class);
            FileEEnchant.defaultIngredientAmount = eeCfg.deserialize("default_ingredient_amount", NumberProvider.class);

            Collection<EEnchant> list = eeCfg.deserialize("values", new TypeToken<Collection<EEnchant>>(){}.getType());
            if(list != null){
                list.forEach(ee ->{
                    EnchantsRegistries.EENCHANT.register(ee);
                    log("Registered EEnchant: " + ee.key());
                });
            }
            eeCfg.close();
        }
    }
}
