package killercreepr.cruxenchantsoverhaul.config;

import killercreepr.crux.core.plugin.CruxPlugin;
import killercreepr.cruxconfig.config.bukkit.file.Cfg;
import killercreepr.cruxconfig.config.bukkit.file.CruxConfig;
import killercreepr.cruxconfig.config.bukkit.value.CfgValue;
import killercreepr.cruxconfig.config.bukkit.value.CommonValue;
import killercreepr.cruxconfig.config.bukkit.value.NumCfgValue;
import killercreepr.cruxenchantsoverhaul.values.ValuesProvider;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Config extends Cfg implements ValuesProvider {
    public final NumCfgValue MAX_ENCHANTING_TABLE_POWER = new NumCfgValue(300);
    public final NumCfgValue POWER_PER_BOOKSHELF = new NumCfgValue(5);
    public final NumCfgValue POWER_PER_BOOK_IN_CHISELED_BOOKSHELF = new NumCfgValue(1);
    public final NumCfgValue POWER_PER_ENCHANTED_BOOK_IN_CHISELED_BOOKSHELF = new NumCfgValue(2);
    public final NumCfgValue POWER_PER_ENCHANT_ON_ENCHANTED_BOOK_PROGRESS = new NumCfgValue(3);
    public final CfgValue<String> CUSTOM_ENCHANTING_TABLE_PERMISSION = new CommonValue<>(){};

    public Config(@NotNull Plugin plugin, @NotNull String path) {
        super(plugin, path);
    }

    public Config(@NotNull File file) {
        super(file);
    }

    public Config(@NotNull CruxConfig cfg) {
        super(cfg);
    }

    @Override
    public void reload(@NotNull CruxPlugin plugin) {
        setup();
    }
}
