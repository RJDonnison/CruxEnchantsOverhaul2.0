package killercreepr.cruxenchantsoverhaul.block.active;

import com.google.common.collect.AbstractIterator;
import killercreepr.crux.api.data.DataExchange;
import killercreepr.crux.api.data.tick.ManagedTicked;
import killercreepr.crux.api.item.CruxItem;
import killercreepr.crux.api.math.Pos2D;
import killercreepr.crux.core.Crux;
import killercreepr.crux.core.math.BlockPos;
import killercreepr.cruxblocks.api.block.CruxBlock;
import killercreepr.cruxblocks.api.block.active.ActiveCruxInteractable;
import killercreepr.cruxblocks.api.block.context.BlockContext;
import killercreepr.cruxblocks.api.mining.user.Miner;
import killercreepr.cruxblocks.core.block.active.SimpleActiveCruxBlock;
import killercreepr.cruxblocks.core.block.data.CustomBlockData;
import killercreepr.cruxcore.CruxCore;
import killercreepr.cruxenchantsoverhaul.CruxEnchantsOverhaul;
import killercreepr.cruxenchantsoverhaul.enchanting.BlockColumn;
import killercreepr.cruxenchantsoverhaul.enchanting.BlockGrid;
import killercreepr.cruxenchantsoverhaul.enchanting.EnchantData;
import killercreepr.cruxenchantsoverhaul.menu.enchanting.EnchantingMenu;
import killercreepr.cruxmenus.core.registries.Menus;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ChiseledBookshelf;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EnchantTableBlock extends SimpleActiveCruxBlock implements ManagedTicked, ActiveCruxInteractable {
    public static final List<BlockPos> BOOKSHELF_OFFSETS = betweenClosedStream(-2, 0, -2, 2, 1, 2)
        .filter(blockPos -> Math.abs(blockPos.x()) == 2 || Math.abs(blockPos.z()) == 2)
        .toList();

    public static Stream<BlockPos> betweenClosedStream(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return StreamSupport.stream(betweenClosed(minX, minY, minZ, maxX, maxY, maxZ).spliterator(), false);
    }

    public static Iterable<BlockPos> betweenClosed(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dx = x2 - x1 + 1;
        int dy = y2 - y1 + 1;
        int dz = z2 - z1 + 1;
        int total = dx * dy * dz;

        return () -> new AbstractIterator<>() {
            private int index = 0;

            @Override
            protected BlockPos computeNext() {
                if (index >= total) {
                    return endOfData();
                }

                int xOffset = index % dx;
                int yOffset = (index / dx) % dy;
                int zOffset = (index / (dx * dy));

                index++;
                return BlockPos.at(x1 + xOffset, y1 + yOffset, z1 + zOffset);
            }
        };
    }


    protected int power;
    protected final CustomBlockData data;
    protected final EnchantData enchantData = new EnchantData();
    public EnchantTableBlock(@NotNull Block block, @NotNull CruxBlock cruxBlock) {
        super(block, cruxBlock);
        this.data = new CustomBlockData(block);
        load();
    }

    public int maxPower() {
        return CruxEnchantsOverhaul.inst().values().MAX_ENCHANTING_TABLE_POWER.getInt();
    }

    public void load(){

    }

    public void save(){
    }

    public void stop(){

    }

    public @NotNull EnchantData getEnchantData() {
        return enchantData;
    }

    protected boolean broken = false;
    @Override
    public void broken(@NotNull BlockContext ctx) {
        super.broken(ctx);
        broken = true;
        new HashSet<>(Menus.OPENED.values()).forEach(menu ->{
            if(!(menu instanceof EnchantingMenu m)) return;
            if(this.getBlock().equals(m.getBlock().getBlock())){
                menu.close();
            }
        });

        /*Location center = block.getLocation().toCenterLocation().subtract(0, .4, 0);
        enchanterProgress.forEach((uuid, progress) ->{
            ItemStack toDrop = progress.getResult() == null ? progress.getItemToEnchant() : progress.getResult();
            if(CruxMath.testChance(35D)){
                if(CruxMath.testChance(5D)){
                    CreateSound.sound(Sound.ENTITY_ITEM_BREAK).playAt(center);
                    new ParticleBuilder(Particle.ITEM)
                        .location(block.getLocation().toCenterLocation())
                        .offset(.1, .1, .1)
                        .extra(.05)
                        .count(10)
                        .data(toDrop)
                        .spawn()
                    ;
                    return;
                }
                int maxDamage = CruxItem.getMaxDurability(toDrop);
                if(maxDamage < 1) return;
                toDrop.editMeta(Damageable.class, meta ->{
                    int durability = maxDamage - meta.getDamage();
                    int deplete = (int) (durability * CruxMath.random(.25f, .40));
                    if(deplete < 1) return;
                    meta.setDamage(meta.getDamage() + deplete);
                });

                CreateSound.sound(Sound.ENTITY_ITEM_BREAK, 2f).playAt(center);
                new ParticleBuilder(Particle.ITEM)
                    .location(block.getLocation().toCenterLocation())
                    .offset(.1, .1, .1)
                    .extra(.05)
                    .count(3)
                    .data(toDrop)
                    .spawn()
                ;
            }
            center.getWorld().dropItem(center, toDrop);
            //todo maybe set the item drop owner to the player so other
            // players can't pick it up?
        });*/
    }

    @Override
    public @NotNull Collection<ItemStack> getDrops(@NotNull Miner miner) {
        return List.of(new ItemStack(Material.ENCHANTING_TABLE));
    }

    @NotNull
    @Override
    public Event.Result interact(@NotNull PlayerInteractEvent event) {
        if(event.useInteractedBlock() == Event.Result.DENY) return event.useInteractedBlock();
        Player p = event.getPlayer();

        var permission = CruxEnchantsOverhaul.inst().values().CUSTOM_ENCHANTING_TABLE_PERMISSION.value();
        if(permission != null && !p.hasPermission(permission)){
            return event.useInteractedBlock();
        }

        event.setCancelled(true);
        updateTable();

        /*EnchanterProgress progress = getProgress(p.getUniqueId());
        if(progress!=null){
            progress.getMenu().open(p);
            return Event.Result.ALLOW;
        }*/

        var holder = CruxCore.inst().cruxMenus().menuRegistry().menuHolders().get(Crux.key("enchanting_table"));
        if(holder == null){
            p.sendMessage("An error has occurred. Contact an administrator.");
            throw new IllegalStateException(
              "A custom enchanting table menu has not been setup! One should be setup in the CruxEnchantsOverhaul/menus folder, named 'enchanting_table'"
            );
        }
        holder.open(p,
            DataExchange.builder()
                .put(EnchantTableBlock.this)
                .put(CruxEnchantsOverhaul.inst().getEnchanter())
                .build());
        return Event.Result.ALLOW;
    }

    public @NotNull Collection<Block> getCheckNearBlocks(){
        Collection<Block> list = new ArrayList<>();
        for(BlockPos pos : BOOKSHELF_OFFSETS){
            Block b = block.getRelative(pos.blockX(), pos.blockY(), pos.blockZ());
            list.add(b);
        }
        return list;
    }

    public void updateTable(){
        int maxPower = maxPower();
        int newPower = 0;
        for(int i : getValidNearBlocks().values()){
            newPower += i;
            if(newPower >= maxPower) break;
        }
        power = Math.min(newPower, maxPower);
    }

    //block to power
    public @NotNull Map<Block, Integer> getValidNearBlocks(){
        Map<Block, Integer> map = new HashMap<>();
        for (Block b : getCheckNearBlocks()) {
            int power = getPower(b);
            if(power == 0) continue;
            map.put(b, power);
        }
        return map;
    }

    public int getPower(@NotNull Block b){
        //todo
        switch (b.getType()){
            case BOOKSHELF -> {
                return CruxEnchantsOverhaul.inst().values().POWER_PER_BOOKSHELF.getInt();
            }
            case CHISELED_BOOKSHELF -> {
                ChiseledBookshelf data = (ChiseledBookshelf) b.getState();
                int power = 0;
                for (Map.Entry<ItemStack, Integer> entry : getValidBookItems(data.getInventory()).entrySet()){
                    power += entry.getValue();
                }
                return power;
            }
        }
        return 0;
    }

    /**
     * Duplicate items will not be stored.
     * Only will get the highest power.
     */
    public @NotNull Map<ItemStack, Integer> getValidBookItems(){
        Map<ItemStack, Integer> map = new HashMap<>();

        for(Block b : getValidNearBlocks().keySet()){
            if(!(b.getState() instanceof ChiseledBookshelf state)) continue;
            getValidBookItems(state.getInventory()).forEach((item, power) ->{
                Integer previous = map.get(item);
                if(previous != null && power < previous) return;
                map.put(item, power);
            });
        }

        return map;
    }

    public @NotNull Map<ItemStack, Integer> getValidBookItems(@NotNull Inventory inventory){
        Map<ItemStack, Integer> map = new HashMap<>();
        for(ItemStack item : inventory){
            if(CruxItem.isEmpty(item)) continue;
            int power = getItemPower(item);
            if(power==0) continue;
            map.compute(item, (it, pow) ->{
                if(pow==null) pow = power;
                else pow += power;
                return pow;
            });
        }
        return map;
    }

    public int getItemPower(@NotNull ItemStack item){
        var cfg = CruxEnchantsOverhaul.inst().values();
        switch (item.getType()){
            case BOOK -> {
                return cfg.POWER_PER_BOOK_IN_CHISELED_BOOKSHELF.getInt();
            }
            case ENCHANTED_BOOK -> {
                AtomicInteger power = new AtomicInteger(cfg.POWER_PER_ENCHANTED_BOOK_IN_CHISELED_BOOKSHELF.getInt());
                if(item.getItemMeta() instanceof EnchantmentStorageMeta meta){
                    var multiplier = cfg.POWER_PER_ENCHANT_ON_ENCHANTED_BOOK_PROGRESS.getFloat();
                    meta.getStoredEnchants().forEach((ench, level) ->{
                        int maxLevel = ench.getMaxLevel();
                        float progress = (float) level / (float) maxLevel;

                        int addon = Math.round(multiplier * progress);
                        power.addAndGet(addon);
                    });
                }
                return power.get();
            }
        }
        return 0;
    }

    public @NotNull Map<Enchantment, Integer> getSelectableEnchants(){
        Map<Enchantment, Integer> map = new HashMap<>();

        getValidBookItems().forEach((item, power) ->{
            if(!(item.getItemMeta() instanceof EnchantmentStorageMeta meta)) return;
            meta.getStoredEnchants().forEach((ench, level) ->{
                Integer previous = map.get(ench);
                if(previous != null && level < previous) return;
                map.put(ench, level);
            });
        });

        return map;
    }

    public int getPower() {
        return power;
    }

    public int getMaxPower() {
        return maxPower();
    }

    public CustomBlockData getData() {
        return data;
    }

    @Override
    public void stopped() {
        if(!isValid()) return;
        save();

        /*if(!broken){
            Location l = block.getLocation().toCenterLocation().add(0,0.4,0);
            enchanterProgress.values().forEach(prog ->{
                block.getWorld().dropItem(l, prog.getItemToEnchant());
            });
        }*/
    }

    @Override
    public void tick() {
        /*enchanterProgress.values().removeIf(progress ->{
            if(progress.shouldRemove()) return true;
            progress.tick();
            return false;
        });*/
    }

    public @NotNull BlockGrid buildBookGrid(){
        BlockGrid.Builder builder = BlockGrid.builder();

        Map<Pos2D, List<Block>> yToBlocks = new HashMap<>();
        getValidNearBlocks().keySet().forEach(b ->{
            yToBlocks.computeIfAbsent(Pos2D.at(block.getX()-b.getX(), block.getZ()-b.getZ()), i-> new ArrayList<>()).add(b);
        });

        yToBlocks.forEach((pos, blocks) ->{
            blocks.sort(Comparator.comparingInt(Block::getY));

            BlockColumn column = new BlockColumn(blocks);

            builder.put(pos, column);
        });

        return builder.build();
    }
}
