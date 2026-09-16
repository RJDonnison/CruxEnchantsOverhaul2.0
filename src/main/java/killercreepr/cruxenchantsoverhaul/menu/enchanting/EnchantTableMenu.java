package killercreepr.cruxenchantsoverhaul.menu.enchanting;

import killercreepr.crux.api.communication.CreateSound;
import killercreepr.crux.api.data.DataExchange;
import killercreepr.crux.api.item.CruxItem;
import killercreepr.crux.api.text.context.InputContext;
import killercreepr.crux.api.text.context.TextParserContext;
import killercreepr.crux.api.text.tags.container.MergedTagContainer;
import killercreepr.crux.api.text.tags.container.TagContainer;
import killercreepr.crux.api.valueproviders.number.NumberProvider;
import killercreepr.crux.core.Crux;
import killercreepr.crux.core.text.resolver.Tag;
import killercreepr.crux.core.util.*;
import killercreepr.cruxcrafting.api.crafting.context.CruxIngredientContext;
import killercreepr.cruxcrafting.api.crafting.ingredient.CruxRecipeIngredient;
import killercreepr.cruxenchantsoverhaul.api.enchant.EEnchant;
import killercreepr.cruxenchantsoverhaul.block.active.EnchantTableBlock;
import killercreepr.cruxenchantsoverhaul.enchanting.EnchantData;
import killercreepr.cruxenchantsoverhaul.enchanting.EnchantRequirements;
import killercreepr.cruxenchantsoverhaul.enchanting.Enchanter;
import killercreepr.cruxenchantsoverhaul.item.EItems;
import killercreepr.cruxenchantsoverhaul.registries.EnchantsRegistries;
import killercreepr.cruxmenus.api.menu.container.MenuContainer;
import killercreepr.cruxmenus.api.menu.contex.SlotContext;
import killercreepr.cruxmenus.api.menu.holder.MenuHolder;
import killercreepr.cruxmenus.api.menu.holder.MenuItemHolder;
import killercreepr.cruxmenus.api.menu.slot.Slot;
import killercreepr.cruxmenus.api.menu.slot.TempSlotted;
import killercreepr.cruxmenus.core.menu.ConfigMenu;
import killercreepr.cruxmenus.core.menu.slot.SimpleFixedSlot;
import killercreepr.cruxmenus.core.menu.slot.SimpleTempStoredSlot;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnchantTableMenu extends ConfigMenu implements EnchantingMenu, TempSlotted {
    protected final MenuContainer container = MenuContainer.createNew();
    public final Slot INPUT;
    public final Slot LAPIS;
    public final Slot BACK_PAGE;
    public final Slot NEXT_PAGE;
    public final Slot RESULT;

    public Slot buildIngredientSlot(int slot){
        return new SimpleTempStoredSlot(this, slot){
            @Override
            public boolean mayPlace(@NotNull HumanEntity p, @Nullable ItemStack item) {
                if(CruxItem.isEmpty(item)) return true;
                if(selectedEnchant == null) return false;
                if(selectedIngredients == null) return false;
                var ctx = CruxIngredientContext.ingredientContext(item);
                for(var ingredient : selectedIngredients){
                    if(ingredient.test(ctx)) return true;
                }
                return false;
            }
        };
    }

    protected final @NotNull EnchantTableBlock block;
    protected final @NotNull EnchantData enchantData;
    protected final @NotNull Enchanter enchanter;
    protected int page = 0;

    protected Map<EEnchant, Integer> bookEnchants;

    public EnchantTableMenu(@NotNull MenuHolder holder, @NotNull DataExchange info) {
        this(holder, info, null);
    }

    public EnchantTableMenu(@NotNull MenuHolder holder, @NotNull DataExchange info, @Nullable MergedTagContainer tags) {
        super(holder, info, tags);
        this.block = info().getOrThrow(EnchantTableBlock.class);
        this.enchantData = block.getEnchantData();
        this.enchanter = info().getOrThrow(Enchanter.class);

        INPUT = new SimpleTempStoredSlot(this, getInputSlot()) {
            @Override
            public @NotNull Integer getMaxStackSize() {
                return 1;
            }

            @Override
            public boolean mayPlace(@NotNull HumanEntity p, @Nullable ItemStack item) {
                if(CruxItem.isEmpty(item)) return true;
                return !LAPIS.mayPlace(p, item) && isEnchantable(item);
            }

            @Override
            public void onChanged(@NotNull SlotContext ctx) {
                super.onChanged(ctx);
                if(CruxItem.isEmpty(ctx.getNewItem()) || !ctx.getNewItem().isSimilar(ctx.getOldItem())){
                    if(bookEnchants != null){
                        ItemStack book = LAPIS.getItem();
                        if(!CruxItem.isEmpty(book)){
                            CruxEntityUtil.giveOrDrop(ctx.getWhoClicked(), book.clone());
                            LAPIS.setItem(LAPIS.getSlottedItemReplacement(), true);
                            Crux.scheduler().runTask(() ->{
                                changeView(false);
                                update();
                            });
                            return;
                        }
                    }
                }

                if(!CruxItem.isEmpty(ctx.getNewItem()) && !ctx.getNewItem().isSimilar(ctx.getOldItem())){
                    setSelectedEnchant(null);
                    Crux.scheduler().runTask(() ->{
                        changeView(false);
                        update();
                    });
                }
            }
        };
        RESULT = new SimpleFixedSlot(this, getResultSlot()) {
            @Override
            public void onClick(@NotNull HumanEntity p, @NotNull InventoryClickEvent event) {
                super.onClick(p, event);

                ItemStack item = event.getCurrentItem();

                if(isBlank(item)) return;
                if(CruxTag.has(item, "menu_fixed")) return;;
                if(selectedEnchant == null && bookEnchants == null) return;
                if(enchantRequirements == null) return;
                if(INPUT.isBlank(INPUT.getItem())) return;

                if(selectedEnchant != null){
                    CanUpgradeEnchant status = canUpgradeLevel(p, selectedEnchant, getCurrentEnchantLevel(selectedEnchant));
                    if(status == CanUpgradeEnchant.MAX_LEVEL || status == CanUpgradeEnchant.HAS_CONFLICTS){
                        setSelectedEnchant(null);
                        update();
                        return;
                    }
                }else{
                    Map<EEnchant, Integer> validated = new HashMap<>();
                    ItemStack input = INPUT.getItem();
                    bookEnchants.forEach((ench, level) ->{
                        if(level > getMaxEnchantLevel(p, input, ench)) return;
                        if(hasConflictions(input, ench)) return;
                        validated.put(ench, level);
                    });
                    if(validated.isEmpty()){
                        update();
                        return;
                    }
                    bookEnchants = validated;
                }

                var requirementResult = enchantRequirements.hasRequirements(p);
                if(requirementResult != EnchantRequirements.RequirementResult.SUCCESS) return;

                removeCosts(p, enchantRequirements);

                ItemStack result = item.clone();
                INPUT.setItem(result, true);

                if(selectedEnchant != null){
                    if(canUpgradeLevel(p, selectedEnchant, getCurrentEnchantLevel(selectedEnchant)) == CanUpgradeEnchant.YES){
                        setSelectedEnchant(selectedEnchant);
                    }else setSelectedEnchant(null);
                }else{
                    if(isLapisEnchantedBook()){
                        ItemStack lapis = LAPIS.getItem();
                        lapis.setAmount(lapis.getAmount()-1);
                        if(CruxItem.isEmpty(lapis)){
                            LAPIS.setItem(LAPIS.getSlottedItemReplacement(), true);
                        }
                    }
                }
                update();

                CreateSound.sound(Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                    net.kyori.adventure.sound.Sound.Source.BLOCK, 0.4f, 1f)
                    .playAt(block.getBlock().getLocation().toCenterLocation());

                if(p instanceof Player player){
                    player.incrementStatistic(Statistic.ITEM_ENCHANTED);
                    Advancement advance = Crux.getServer().getAdvancement(NamespacedKey.fromString("minecraft:story/enchant_item"));
                    if(advance != null){
                        var prog = player.getAdvancementProgress(advance);
                        if(prog != null && !prog.isDone()){
                            prog.getRemainingCriteria().forEach(prog::awardCriteria);
                        }
                    }
                }
            }
        };
        LAPIS = new SimpleTempStoredSlot(this, getLapisSlot()) {
            @Override
            public boolean mayPlace(@NotNull HumanEntity p, @Nullable ItemStack item) {
                if(CruxItem.isEmpty(item)) return true;

                if(isEnchantedBook(item)){
                    ItemStack input = INPUT.getItem();
                    if(INPUT.isBlank(input)) return false;
                    return true;
                }

                return Crux.handlers().item().getType(item).equals(Material.LAPIS_LAZULI.key());
            }
            @Override
            public @NotNull ItemStack getSlottedItemReplacement() {
                return EItems.EMPTY_SLOT_LAPIS.buildItem();
            }

            @Override
            public boolean isBlank(@Nullable ItemStack item) {
                return super.isBlank(item) || CruxTag.has(item, "menu_fixed");
            }
        };
        NEXT_PAGE = new SimpleFixedSlot(this, getNextPageSlot()) {
            @Override
            public void onClick(@NotNull HumanEntity p, @NotNull InventoryClickEvent event) {
                super.onClick(p, event);
                if(page >= getMaxPage()) return;
                page = CruxMath.wrap(page+1, 0, getMaxPage());
                updateEnchantList();
            }
        };
        BACK_PAGE = new SimpleFixedSlot(this, getBackPageSlot()) {
            @Override
            public void onClick(@NotNull HumanEntity p, @NotNull InventoryClickEvent event) {
                super.onClick(p, event);
                if(page < 1) return;
                page = CruxMath.wrap(page-1, 0, getMaxPage());
                updateEnchantList();
            }
        };

        addSlot(LAPIS);
        addSlot(INPUT);
        addSlot(BACK_PAGE);
        addSlot(NEXT_PAGE);

        getIngredientInputSlots().forEach(slot ->{
            addSlot(buildIngredientSlot(slot));
        });
        //update();

        //setGeneralInvClickAction((p,e) -> e.setCancelled(false));

        container.addOpenedMenu(this);
    }

    public EnchantRequirements buildFullRequirements(ItemStack item, EEnchant enchant, int startLevel, int level){
        EnchantRequirements requirements = buildEnchantRequirements(item, enchant, level);
        for(int i = startLevel; i < level; i++){
            EnchantRequirements add = buildEnchantRequirements(item, enchant, i);
            requirements.exp += add.exp;
            requirements.lapis += add.lapis;
            requirements.addIngredientAmount(add.ingredients);
        }
        return requirements;
    }

    public EnchantRequirements buildEnchantRequirements(ItemStack item, Map<EEnchant, Integer> enchants) {
        EnchantRequirements requirements = new EnchantRequirements(this);
        if (enchants.isEmpty()) return requirements;

        enchants.forEach((ench, level) -> {
            int currentLevel = item.getEnchantmentLevel(ench.enchantment());
            int levelDifference = level - currentLevel;

            if (levelDifference < 0) return; // Skip if not an upgrade
            if(levelDifference == 0){
                currentLevel++;
                level++;
            }

            // Build base requirements for the target level
            EnchantRequirements require = buildFullRequirements(item, ench, Math.max(currentLevel, 1), level);
            if(require.requiredLevel > requirements.requiredLevel){
                requirements.requiredLevel = require.requiredLevel;
            }
            if(require.requiredPower > requirements.requiredPower){
                requirements.requiredPower = require.requiredPower;
            }
            requirements.exp += require.exp;
            requirements.lapis += require.lapis;
        });

        return requirements;
    }


    public boolean isApplicableEnchant(Entity e, ItemStack item, EEnchant enchant, int level){
        if(!enchant.canEnchantItem(item)) return false;
        return getCurrentEnchantLevel(item, enchant) < getMaxEnchantLevel(e, item, enchant);
    }

    public Map<EEnchant, Integer> filterApplicableEnchants(Entity e, ItemStack item, Map<Enchantment, Integer> map){
        Map<EEnchant, Integer> newMap = new HashMap<>();
        map.forEach((ench, level) ->{
            if(item.getEnchantmentLevel(ench) > level) return;

            EEnchant eEnchant = EnchantsRegistries.EENCHANT.get(ench.key());
            if(eEnchant == null) return;
            if(isApplicableEnchant(e, item, eEnchant, level)){
                newMap.put(eEnchant, level);
            }
        });

        newMap.keySet().removeIf(check ->{
            for (EEnchant apply : newMap.keySet()) {
                if(apply.key().equals(check.key())) continue;
                if(check.conflictsWith(apply)) return true;
            }
            for (Enchantment apply : item.getEnchantments().keySet()) {
                if(apply.key().equals(check.key())) continue;
                if(check.enchantment().conflictsWith(apply)) return true;
            }
            return false;
        });
        return newMap;
    }

    public Map<Enchantment, Integer> getEnchantedBookEnchants(ItemStack item){
        if(item.getItemMeta() instanceof EnchantmentStorageMeta meta){
            return meta.getStoredEnchants();
        }
        return Map.of();
    }

    public void updateBookEnchants(){
        if(!isLapisEnchantedBook()){
            bookEnchants = null;
            return;
        }
        ItemStack input = INPUT.getItem();
        if(INPUT.isBlank(input)){
            bookEnchants = null;
            return;
        }
        ItemStack lapis = LAPIS.getItem();
        if(LAPIS.isBlank(lapis)){
            bookEnchants = null;
            return;
        }
        Map<Enchantment, Integer> storedEnchants = getEnchantedBookEnchants(lapis);
        if(storedEnchants.isEmpty()){
            bookEnchants = Map.of();
            return;
        }
        Entity e = getViewer();
        Map<EEnchant, Integer> enchants = filterApplicableEnchants(e, input, storedEnchants);
        if(enchants.isEmpty()){
            bookEnchants = Map.of();
            return;
        }
        bookEnchants = enchants;
    }

    public boolean isEnchantedBook(ItemStack item){
        if(CruxItem.isEmpty(item)) return false;
        return item.getItemMeta() instanceof EnchantmentStorageMeta;
    }

    public boolean isLapisEnchantedBook(){
        return isEnchantedBook(LAPIS.getItem());
    }

    public CanUpgradeEnchant canUpgradeLevel(Entity e, EEnchant enchant, int level){
        return canUpgradeLevel(e, INPUT.getItem(), enchant, level);
    }

    public CanUpgradeEnchant canUpgradeLevel(Entity e, ItemStack item, EEnchant enchant, int level){
        int maxLevel = getMaxEnchantLevel(e,item, enchant);
        if(level >= maxLevel) return CanUpgradeEnchant.MAX_LEVEL;

        if(hasConflictions(item, enchant)) return CanUpgradeEnchant.HAS_CONFLICTS;

        if(CruxEntityUtil.isNonSurvival(e)) return CanUpgradeEnchant.YES;

        if(!hasEnoughPowerFor(item,enchant, level+1)) return CanUpgradeEnchant.NOT_ENOUGH_POWER;

        return CanUpgradeEnchant.YES;
    }

    public void removeCosts(Entity e, EnchantRequirements requirements){
        requirements.removeCosts(e);
    }

    public int getMaxPage(){
        return Math.max((int)Math.ceil((double)currentEnchantList.size() / (double)getEnchantmentListSlots().size()) - 1, 0);
    }

    @Override
    public void onClose(@NotNull HumanEntity p) {
        super.onClose(p);
        container.onClosed(p, this);
    }

    @Override
    public void onRefresh() {
        this.refreshReconstruct();
        this.modules.refresh();
        this.setItems(this.holder);
        Crux.getServer().getScheduler().runTask(Crux.getMainPlugin(), task -> update());
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        update();
    }

    public List<Integer> getEnchantmentListSlots(){
        return holder.info().getOrThrow("enchant_list_slots", List.class);
    }
    public List<Integer> getIngredientInputSlots(){
        return holder.info().getOrThrow("ingredient_input_slots", List.class);
    }

    public List<Integer> getSelectIngredientSlots(){
        return holder.info().getOrThrow("select_ingredients_indexes", List.class);
    }
    public int getSelectIconSlot(){
        return holder.info().getOrThrow("select_icon_index", Number.class).intValue();
    }
    public int getInputSlot(){
        return holder.info().getOrThrow("input_slot", Number.class).intValue();
    }
    public int getLapisSlot(){
        return holder.info().getOrThrow("lapis_slot", Number.class).intValue();
    }
    public int getResultSlot(){
        return holder.info().getOrThrow("result_slot", Number.class).intValue();
    }
    public int getNextPageSlot(){
        return holder.info().getOrThrow("next_page_slot", Number.class).intValue();
    }
    public int getBackPageSlot(){
        return holder.info().getOrThrow("back_page_slot", Number.class).intValue();
    }
    public int getRequiredXPSlot(){
        return getDataItems().get("required_xp").info().get("slot", NumberProvider.class).value().intValue();
        //return holder.info().getOrThrow("required_xp_slot", Number.class).intValue();
    }
    public int getRequiredLapisSlot(){
        return getDataItems().get("required_lapis").info().get("slot", NumberProvider.class).value().intValue();
        //return holder.info().getOrThrow("required_lapis_slot", Number.class).intValue();
    }
    public int getRequiredLevelSlot(){
        return getDataItems().get("required_level").info().get("slot", NumberProvider.class).value().intValue();
        //return holder.info().getOrThrow("required_level_slot", Number.class).intValue();
    }

    public ItemStack buildResult(ItemStack input){
        if(bookEnchants != null){
            ItemStack result = input.clone();
            bookEnchants.forEach((eEnchant, level) ->{
                int currentLevel = input.getEnchantmentLevel(eEnchant.enchantment());
                if(currentLevel == level) level++;
                result.addUnsafeEnchantment(eEnchant.enchantment(), level);
            });
            Crux.handlers().item().update(result, getViewer());
            return result;
        }

        if(selectedEnchant == null) return null;
        int newLevel = Math.min(getNextEnchantLevel(selectedEnchant), getMaxEnchantLevel(selectedEnchant));

        ItemStack result = input.clone();
        result.addUnsafeEnchantment(selectedEnchant.enchantment(), newLevel);
        Crux.handlers().item().update(result, getViewer());
        return result;
    }

    public boolean isEnchantable(ItemStack item){
        for (EEnchant ench : EnchantsRegistries.EENCHANT) {
            if (!ench.canEnchantItem(item)) continue;
            return true;
        }
        return false;
    }

    public List<EEnchant> getAvailableEnchants(ItemStack item) {
        List<EEnchant> list = new ArrayList<>();
        Map<Enchantment, Integer> shelfEnchants = getBlock().getSelectableEnchants();
        for (EEnchant ench : EnchantsRegistries.EENCHANT) {
            if(!ench.isDiscoverable() && !shelfEnchants.containsKey(ench.enchantment())) continue;
            if (!ench.canEnchantItem(item)) continue;

            int level = getNextEnchantLevel(item, ench);
            if(level == 1 && !hasEnoughPowerFor(item,ench, level)){
                continue;
            }

            list.add(ench);
        }

        Entity e = getViewer();
        list.sort(Comparator
            .comparing((EEnchant ench) -> getCombinedPriority(e, ench)) // Combined namespace + upgrade status
            .thenComparing(ench -> ench.key().value())); // Alphabetical by key name
        return list;
    }

    // 0 = best, 7 = worst
    private int getCombinedPriority(Entity e, EEnchant ench) {
        String namespace = ench.key().namespace();
        CanUpgradeEnchant upgradeStatus = canUpgradeLevel(e, ench, getCurrentEnchantLevel(ench));

        boolean isVanilla = namespace.equals("minecraft");

        return switch (upgradeStatus) {
            case YES -> isVanilla ? 0 : 1;
            case NOT_ENOUGH_POWER -> isVanilla ? 2 : 3;
            case MAX_LEVEL -> isVanilla ? 4 : 5;
            case HAS_CONFLICTS -> isVanilla ? 6 : 7;
        };
    }

    /*public List<EEnchant> getAvailableEnchants(ItemStack item){
        List<EEnchant> list = new ArrayList<>();
        for (EEnchant ench : EnchantsRegistries.EENCHANT) {
            if(!ench.canEnchantItem(item)) continue;
            list.add(ench);
        }

        Entity e = getViewer();
        list.sort(Comparator.comparing(enchat ->{
            int currentLevel = getCurrentEnchantLevel(enchat);
            return enchat.key().value() && canUpgradeLevel(e, enchat, currentLevel);
        }));
        return list;
    }*/

    public CruxItem buildEnchantItem(EEnchant enchant){
        var holder = getDataItems().get("enchant_item");

        int maxLevel = getMaxEnchantLevel(enchant);
        int level = Math.min(getNextEnchantLevel(enchant), maxLevel);
        String name = enchant.displayName() +
          ((level > 1 || maxLevel > 1) ? (" " + CruxMath.numeral(level) + " <gray>/</gray> " + CruxMath.numeral(maxLevel)) : "");

        if(holder != null){
            return CruxItem.wrap(enchant.getIcon())
              .editThis(crux ->{
                  holder.getItem().value().applyComponents(crux, TextParserContext.tags(
                    TagContainer.merged(this.holder.getRegistry().getFormat().tags())
                      .add(Tag.parsed("max_level", maxLevel + ""))
                      .add(Tag.parsed("level", level + ""))
                      .add(Tag.parsed("name", name))
                  ));
              });
        }
        return CruxItem.wrap(enchant.getIcon())
            .customName("<!i><yellow>" + name)
            .editThis(crux ->{
                /*if(enchantRequirements != null && CruxEntityUtil.isNonSurvival(getViewer())){
                    crux.insertLoreFromString(0, "<white>Power: " + block.getPower() + " / " + enchantRequirements.requiredPower);
                }*/
                crux.addLoreFromString(
                    ""
                );
            });
    }

    public String formatName(EEnchant enchant, int nextLevel){
        int maxLevel = getMaxEnchantLevel(enchant);
        int level = Math.min(nextLevel, maxLevel);
        String name = enchant.displayName();
        if(level > 1 || maxLevel > 1){
            name += " " + CruxMath.numeral(level) + " <gray>/</gray> " + CruxMath.numeral(maxLevel);
        }
        return name;
    }

    public int getUpgradeLevel(EEnchant enchant, int level){
        int current = getCurrentEnchantLevel(enchant);
        if(current == level) return level+1;
        return level;
    }

    public CruxItem buildBookEnchantItem(Map<EEnchant, Integer> enchants){
        var entry = CruxCollection.getFirst(enchants.entrySet());
        EEnchant enchant = entry.getKey();
        int maxLevel = getMaxEnchantLevel(enchant);
        int upgradeLevel = getUpgradeLevel(enchant, entry.getValue());

        return CruxItem.wrap(enchant.getIcon())
            .customName("<!i><yellow>" + formatName(enchant, upgradeLevel))
            .editThis(crux ->{
                if(enchants.size() != 1){
                    crux.lore(null);
                    enchants.forEach((eEnchant, eLevel) ->{
                        if(eEnchant.key().equals(enchant.key())) return;
                        crux.addLoreFromString("<yellow>" + formatName(eEnchant, getUpgradeLevel(eEnchant, eLevel)));
                    });
                }
            });
    }

    public boolean hasConflictions(ItemStack input, EEnchant enchant){
        if(INPUT.isBlank(input)) return false;
        Enchantment vanilla = enchant.enchantment();
        for(Enchantment ench : input.getEnchantments().keySet()){
            if(ench == vanilla) continue;
            if(!vanilla.conflictsWith(ench)) continue;
            return true;
        }
        return false;
    }

    public Collection<Enchantment> getConflictions(ItemStack input, EEnchant enchant){
        Collection<Enchantment> list = new HashSet<>();
        Enchantment vanilla = enchant.enchantment();
        for(Enchantment ench : input.getEnchantments().keySet()){
            if(ench == vanilla) continue;
            if(!vanilla.conflictsWith(ench)) continue;
            list.add(ench);
        }
        return list;
    }

    /*public boolean applyConflictingEnchantsList(CruxItem item, EEnchant enchant){
        Collection<Enchantment> list = enchant.conflictingEnchants();
        if(list.isEmpty()) return false;
        item.addLoreFromString("<gray>Conflicting Enchants:");
        list.forEach(ench ->{
            String name;
            EEnchant eEnchant = EnchantsRegistries.EENCHANT.get(ench.key());
            if(eEnchant == null){
                name = PlainTextComponentSerializer.plainText().serialize(ench.description());
            }else name = eEnchant.displayName();
            item.addLoreFromString("<gray>");
        });
        return true;
    }*/

    public CruxItem buildEnchantItemSelection(EEnchant enchant){
        CruxItem crux = buildEnchantItem(enchant);

        var canUpgrade = canUpgradeLevel(getViewer(), enchant, getCurrentEnchantLevel(enchant));
        List<String> list = holder.info().get("item_selection_" + canUpgrade.name().toLowerCase(), List.class);
        if(list != null){
            crux.addLoreFromString(list);
            return crux;
        }
        switch (canUpgrade){
            case YES -> {
                crux.addLoreFromString("<yellow><latinfont:Click to select>");
            }
            case MAX_LEVEL -> {
                crux.addLoreFromString("<red><latinfont:Max Level>");
            }
            case NOT_ENOUGH_POWER -> {
                crux.addLoreFromString(
                    "<red><latinfont:Not enough power>",
                    "<gray>(Requires more bookshelves",
                    "<gray>to be placed around enchanting table)"
                );
            }
            case HAS_CONFLICTS -> {
                crux.addLoreFromString("<red><latinfont:Conflicts With>");
                getConflictions(INPUT.getItem(), enchant).forEach(conflict ->{
                    String name;
                    EEnchant eEnchant = EnchantsRegistries.EENCHANT.get(conflict.key());
                    if(eEnchant == null){
                        name = PlainTextComponentSerializer.plainText().serialize(conflict.description());
                    }else name = eEnchant.displayName();
                    crux.addLoreFromString("<gray><latinfont:" + name + ">");
                });
            }
        }
        return crux;
    }

    public Entity getViewer(){
        return info.getOrThrow("viewer", Entity.class);
    }

    public int getNextEnchantLevel(EEnchant enchant){
        return getNextEnchantLevel(INPUT.getItem(), enchant);
    }
    public int getNextEnchantLevel(ItemStack input, EEnchant enchant){
        if(input == null) return 0;
        int level = input.getEnchantmentLevel(enchant.enchantment());
        return level + 1;
    }

    public int getCurrentEnchantLevel(EEnchant enchant){
        return getCurrentEnchantLevel(INPUT.getItem(), enchant);
    }

    public int getCurrentEnchantLevel(ItemStack input, EEnchant enchant){
        if(input == null) return 0;
        return input.getEnchantmentLevel(enchant.enchantment());
    }

    public int getMaxEnchantLevel(EEnchant enchant){
        return getMaxEnchantLevel(getViewer(), enchant);
    }

    public int getMaxEnchantLevel(Entity e, EEnchant enchant){
        return getMaxEnchantLevel(e, INPUT.getItem(), enchant);
    }

    public int getMaxEnchantLevel(Entity e, ItemStack input, EEnchant enchant){
        return enchantMaxLevel(enchant);
    }

    private int enchantMaxLevel(EEnchant enchant){
        int max = enchant.maxLevel();
        if(!enchant.isDiscoverable()){
            Integer shelf = getBlock().getSelectableEnchants().get(enchant.enchantment());
            if(shelf != null) max = Math.min(max, shelf);
        }
        return max;
    }

    public void setSelectedEnchant(EEnchant selectedEnchant) {
        this.selectedEnchant = selectedEnchant;
        if(selectedEnchant == null){
            this.selectedIngredients = null;
            return;
        }
        this.selectedIngredients = selectedEnchant.ingredientCalculator().calculateIngredients(getViewer(),
            Math.min(getNextEnchantLevel(selectedEnchant), getMaxEnchantLevel(selectedEnchant)),
            (float) selectedEnchant.quality());
    }

    protected final List<EEnchant> currentEnchantList = new ArrayList<>();
    protected EEnchant selectedEnchant;
    protected List<CruxRecipeIngredient> selectedIngredients;
    protected EnchantRequirements enchantRequirements;
    protected boolean selectedView = false;

    public boolean changeView(boolean selectedView){
        if(selectedView){
            addSlot(RESULT);
        }
        if(selectedView == this.selectedView) return false;
        if(selectedView){
            String id;
            if(bookEnchants != null && bookEnchants.isEmpty()){
                id = "select_invalid_title";
            }else if(enchantRequirements != null && (enchantRequirements.ingredients == null || enchantRequirements.ingredients.isEmpty())){
                id = "select_no_ingredients_title";
            }else{
                id = "select_title";
            }
            reconstruct(buildSize(), holder.getRegistry().getFormat()
                    .deserialize(holder.info().getOrThrow(id, String.class)),
                true, true);
            this.selectedView = true;
        }else{
            refreshReconstruct();
            this.selectedView = false;
            putSlot(RESULT.getIndex(), null);

            if(getViewer() instanceof HumanEntity p){
                getIngredientInputSlots().forEach(slot ->{
                    ItemStack item = getInventory().getItem(slot);
                    if(CruxItem.isEmpty(item)) return;
                    CruxEntityUtil.giveOrDrop(p, item.clone());
                    item.setAmount(0);
                });
            }
        }
        return true;
    }

    public void updateEnchantList(){
        if(bookEnchants != null){
            setSelectedEnchant(null);
            return;
        }
        if(currentEnchantList.isEmpty()){
            if(selectedEnchant != null){
                setSelectedEnchant(null);
                changeView(false);
            }

            boolean notify = false;
            boolean hasInput = !INPUT.isBlank(INPUT.getItem());
            for(Integer slot : getEnchantmentListSlots()) {
                slots.remove(slot);
                if(hasInput && !notify){
                    notify = true;
                    setItem(slot, buildEnchantSelectionNotify(), true);
                    continue;
                }

                setItem(slot, null, true);
            }
            return;
        }

        if(selectedEnchant != null){
            for(Integer slot : getEnchantmentListSlots()){
                putSlot(slot, null);
                setItem(slot, null, true);
            }
            changeView(true);

            EEnchant eEnchant = selectedEnchant;
            int selectIconSlot = getSelectIconSlot();
            setItem(selectIconSlot,
                buildEnchantItem(eEnchant)
                    .addLoreFromString("<yellow><latinfont:Click to unselect>")
                    .item(),
                new SimpleFixedSlot(this, selectIconSlot){
                @Override
                public void onClick(@NotNull HumanEntity p, @NotNull InventoryClickEvent event) {
                    super.onClick(p, event);
                    setSelectedEnchant(null);
                    update();
                    CLICK.playFor(p);
                }
            }, true);

            int level = Math.min(getNextEnchantLevel(selectedEnchant), getMaxEnchantLevel(selectedEnchant));
            List<CruxRecipeIngredient> ingredients = eEnchant.ingredientCalculator().calculateIngredients(
                getViewer(), level, (float) selectedEnchant.quality()
            );
            int index = -1;
            for(Integer slot : getSelectIngredientSlots()){
                index++;
                if(index >= ingredients.size()) break;
                CruxRecipeIngredient ingredient = ingredients.get(index);
                setItem(slot, ingredient.getItemDisplay().getFirst(), true);
            }
            return;
        }
        changeView(false);

        int index = -1;
        boolean notify = false;
        for(Integer slot : getEnchantmentListSlots()){
            slots.remove(slot);
            index++;
            if(index >= currentEnchantList.size()){
                if(!notify){
                    notify = true;
                    setItem(slot, buildEnchantSelectionNotify(), true);
                    continue;
                }
                setItem(slot, null, true);
                continue;
            }
            EEnchant eEnchant = currentEnchantList.get(index);
            setItem(slot,
                buildEnchantItemSelection(eEnchant)
                    .item(),
                new SimpleFixedSlot(this, slot){
                @Override
                public void onClick(@NotNull HumanEntity p, @NotNull InventoryClickEvent event) {
                    super.onClick(p, event);
                    if(canUpgradeLevel(p, eEnchant, getCurrentEnchantLevel(eEnchant)) != CanUpgradeEnchant.YES) return;

                    setSelectedEnchant(eEnchant);
                    update();
                    CLICK.playFor(p);
                }
            }, true);
        }
    }

    public ItemStack buildEnchantSelectionNotify(){
        var holder = getDataItems().get("enchant_selection_notify");
        if(holder != null) return holder.getItem().value().buildItem(TextParserContext.empty());
        return CruxItem.create(Material.BOOKSHELF)
            .itemModel(Crux.key("gui/question_mark"))
            .itemName("<white>Enchantments not")
            .addLoreFromString(
                "<white>showing up?",
                "<white>Each enchantment requires",
                "<white>a certain amount of power.",
                "",
                "<white>You can increase an enchanting",
                "<white>table's power by placing",
                "<white>bookshelves around it.",
                "",
                "<white>You can increase it further by",
                "<white>placing chiseled bookshelves",
                "<white>with enchanted books placed",
                "<white>inside of them!"
            )
            .item();
    }

    public InputContext buildInputContext(ItemStack input, EEnchant selectedEnchant, int level){
        Entity e = getViewer();
        return InputContext.inputContext(TagContainer.string()
            .hook(e)
            .add(Tag.parsed("level", level + ""))
            .add(Tag.parsed("power", block.getPower() + ""))
            .add(Tag.parsed("quality", selectedEnchant.quality() + ""))
            .add(Tag.parsed("level_zero", Math.max(level-1, 0) + ""))
            .add(Tag.parsed("max_level", getMaxEnchantLevel(e, input, selectedEnchant) + ""))
        );
    }

    public boolean hasEnoughPowerFor(ItemStack item, EEnchant enchant, int level){
        if(enchant.requiredPower() == null) return true;
        int needed = enchant.requiredPower().sample(buildInputContext(item, enchant, level)).intValue();
        return block.getPower() >= needed;
    }

    public EnchantRequirements buildEnchantRequirements(ItemStack input, EEnchant selectedEnchant, int level){
        InputContext ctx = buildInputContext(input, selectedEnchant, level);
        EnchantRequirements requirements = new EnchantRequirements(this);
        if(selectedEnchant.requiredLapis() != null){
            requirements.lapis = selectedEnchant.requiredLapis().sample(ctx).intValue();
        }
        if(selectedEnchant.requiredLevel() != null){
            requirements.requiredLevel = selectedEnchant.requiredLevel().sample(ctx).intValue();
        }
        if(selectedEnchant.requiredExp() != null){
            requirements.exp = selectedEnchant.requiredExp().sample(ctx).intValue();
        }
        if(selectedEnchant.requiredPower() != null){
            requirements.requiredPower = selectedEnchant.requiredPower().sample(ctx).intValue();
        }
        requirements.ingredients = selectedIngredients;
        return requirements;
    }

    public void updateRequirements(){
        ItemStack input = INPUT.getItem();
        if(CruxItem.isEmpty(input) || (bookEnchants== null && selectedEnchant == null)){
            enchantRequirements = null;

            if(selectedView){
                setItem(getRequiredXPSlot(), null, true);
                setItem(getRequiredLapisSlot(), null, true);
                setItem(getRequiredLevelSlot(), null, true);
            }
            return;
        }

        if(bookEnchants != null){
            enchantRequirements = buildEnchantRequirements(input, bookEnchants);
            enchantRequirements.lapis = 0;
            return;
        }

        this.enchantRequirements = buildEnchantRequirements(input, selectedEnchant, getNextEnchantLevel(selectedEnchant));

        setItem(getRequiredLevelSlot(), buildRequiredLevelItem(enchantRequirements), true);
        setItem(getRequiredXPSlot(), buildRequiredXPItem(enchantRequirements), true);
        setItem(getRequiredLapisSlot(), buildRequiredLapisItem(enchantRequirements), true);
    }

    public Map<String, MenuItemHolder> getDataItems(){
        return holder.info().getOrDefault("items", Map.class, Map.of());
    }

    public ItemStack buildRequiredXPItem(EnchantRequirements requirements){
        if(requirements.exp < 1) return null;
        var item = getDataItems().get("required_xp").getItem().value().buildItem(TextParserContext.tags(
          TagContainer.merged(holder.getRegistry().getFormat().tags())
            .hook(getViewer())
            .add(Tag.parsed("current_exp_points", getExperiencePoints(getViewer()) + ""))
            .add(Tag.parsed("required_exp", requirements.exp + ""))
            .add(Tag.parsed("requirement_count", Math.min(requirements.exp, 99) + ""))
            .add(Tag.parsed("requirement_text", requirementText(getExperiencePoints(getViewer()), requirements.exp))
            )));
        if(item != null){
            item.editMeta(meta -> meta.setMaxStackSize(99));
        }
        return item;
        //return holder.info().getOrThrow("required_xp_item", DynamicItem.class).buildItem(TextParserContext.empty());
        /*return CruxItem.create(Material.EXPERIENCE_BOTTLE)
            .itemModel(Crux.key("gui/exp_orb"))
            .itemName("<white>Experience Points Cost")
            .addLoreFromString(
                requirementText(getExperiencePoints(getViewer()), requirements.exp)
            )
            .amount(Math.min(requirements.exp, 99))
            .editMeta(meta ->{
                meta.setMaxStackSize(99);
            })
            .item();*/
    }

    public String requirementText(int value, int max){
        String color = value >= max ? "<green>" : "<red>";
        return color + CruxMath.format(value) + " / " + CruxMath.format(max);
    }

    public ItemStack buildRequiredLevelItem(EnchantRequirements requirements){
        if(requirements.requiredLevel < 1) return null;
        var item = getDataItems().get("required_level").getItem().value().buildItem(TextParserContext.tags(
          TagContainer.merged(holder.getRegistry().getFormat().tags())
            .hook(getViewer())
            .add(Tag.parsed("current_level", getLevel(getViewer()) + ""))
            .add(Tag.parsed("required_level", requirements.requiredLevel + ""))
            .add(Tag.parsed("requirement_count", Math.min(requirements.requiredLevel, 99) + ""))
            .add(Tag.parsed("requirement_text", requirementText(getLevel(getViewer()), requirements.requiredLevel)))
        ));
        if(item != null){
            item.editMeta(meta -> meta.setMaxStackSize(99));
        }
        return item;
        /*return CruxItem.create(Material.EXPERIENCE_BOTTLE)
            .itemName("<white>Required Level")
            .addLoreFromString(
                requirementText(getLevel(getViewer()), requirements.requiredLevel)
                //"<gray>Power: " + requirementText(block.getPower(), requirements.requiredPower)
            )
            .amount(Math.min(requirements.requiredLevel, 99))
            .item();*/
    }

    public ItemStack buildRequiredLapisItem(EnchantRequirements requirements){
        if(requirements.lapis < 1) return null;
        var item = getDataItems().get("required_lapis").getItem().value().buildItem(TextParserContext.tags(
          TagContainer.merged(holder.getRegistry().getFormat().tags())
            .hook(getViewer())
            .add(Tag.parsed("current_lapis", getLapisAmount() + ""))
            .add(Tag.parsed("required_lapis", requirements.lapis + ""))
            .add(Tag.parsed("requirement_count", Math.min(requirements.lapis, 99) + ""))
            .add(Tag.parsed("requirement_text", requirementText(getLapisAmount(), requirements.lapis)))
        ));
        if(item != null){
            item.editMeta(meta -> meta.setMaxStackSize(99));
        }
        return item;
        /*return CruxItem.create(Material.LAPIS_LAZULI)
            .itemName("<white>Lapis Lazuli Cost")
            .addLoreFromString(
                requirementText(getLapisAmount(), requirements.lapis)
            )
            .amount(Math.min(requirements.lapis, 99))
            .item();*/
    }

    public void updateInputData(){
        currentEnchantList.clear();
        if(bookEnchants != null) return;
        ItemStack input = INPUT.getItem();
        if(!CruxItem.isEmpty(input)){
            currentEnchantList.addAll(getAvailableEnchants(input));
        }
    }

    public void update(){
        updateBookEnchants();

        updateInputData();

        updateEnchantList();

        updateRequirements();
        updateBookLayout();

        updateResult();
    }

    public void updateBookLayout(){
        if(bookEnchants == null) return;
        for(Integer slot : getEnchantmentListSlots()){
            putSlot(slot, null);
            setItem(slot, null, true);
        }
        Crux.scheduler().runTask(() ->{
            changeView(true);
        });

        if(bookEnchants.isEmpty()){
            return;
        }

        int selectIconSlot = getSelectIconSlot();
        setItem(selectIconSlot,
            buildBookEnchantItem(bookEnchants)
                .item(), true);

        List<CruxRecipeIngredient> ingredients = enchantRequirements.ingredients;
        if(ingredients != null){
            int index = -1;
            for(Integer slot : getSelectIngredientSlots()){
                index++;
                if(index >= ingredients.size()) break;
                CruxRecipeIngredient ingredient = ingredients.get(index);
                setItem(slot, ingredient.getItemDisplay().getFirst(), true);
            }
        }

        setItem(getRequiredLevelSlot(), buildRequiredLevelItem(enchantRequirements), true);
        setItem(getRequiredXPSlot(), buildRequiredXPItem(enchantRequirements), true);
        setItem(getRequiredLapisSlot(), buildRequiredLapisItem(enchantRequirements), true);
    }

    public int getExperiencePoints(Entity e){
        if(e instanceof Player p){
            return p.calculateTotalExperiencePoints();
        }
        return 0;
    }

    public int getLevel(Entity e){
        if(e instanceof Player p){
            return p.getLevel();
        }
        return 0;
    }

    public int getLapisAmount(){
        ItemStack lapis = LAPIS.getItem();
        return LAPIS.isBlank(lapis) ? 0 : lapis.getAmount();
    }

    public ItemStack buildDeniedResult(EnchantRequirements requirements, EnchantRequirements.RequirementResult result){
        return CruxItem.create(Material.BARRIER)
            .editThis(crux ->{
                String name = CruxString.toTitleCase(result.toString());
                crux.itemName("<red>" + name);

                int amount;
                int maxAmount;
                switch (result){
                    case NOT_ENOUGH_LAPIS_LAZULI -> {
                        amount = getLapisAmount();
                        maxAmount = requirements.lapis;
                    }
                    case NOT_ENOUGH_EXPERIENCE_POINTS -> {
                        amount = getExperiencePoints(getViewer());
                        maxAmount = requirements.exp;
                    }
                    case NOT_REQUIRED_LEVEL -> {
                        amount = getLevel(getViewer());
                        maxAmount = requirements.requiredLevel;
                    }
                    case NOT_ENOUGH_POWER -> {
                        amount = block.getPower();
                        maxAmount = requirements.requiredPower;
                    }
                    default -> {
                        return;
                    }
                }

                crux.addLoreFromString(
                    "<white>" + CruxMath.format(amount) +
                        "<gray>/</gray>" +
                        CruxMath.format(maxAmount)
                );
                CruxTag.set(crux.item(), "menu_fixed", PersistentDataType.BOOLEAN, true);
            })
            .item();
    }

    public ItemStack buildResult(){
        ItemStack input = INPUT.getItem();
        if(enchantRequirements == null || INPUT.isBlank(input)){
            return null;
        }
        if(bookEnchants != null && bookEnchants.isEmpty()){
            return CruxItem.create(Material.BARRIER)
                .customName("<!i><red>No Valid Enchants")
                .addLoreFromString(
                    CruxString.buildDescription(
                        "No enchantments could validly be applied to this item",
                        CruxString.DEFAULT_DESCRIPTION_MAX_LENGTH,
                        e -> "<gray>" + e
                    )
                )
                .editThis(crux ->{
                    CruxTag.set(crux.item(), "menu_fixed", PersistentDataType.BOOLEAN, true);
                })
                .item();
        }

        Entity e = getViewer();
        EnchantRequirements.RequirementResult result = enchantRequirements.hasRequirements(e);
        if(result != EnchantRequirements.RequirementResult.SUCCESS){
            return buildDeniedResult(enchantRequirements, result);
        }
        return buildResult(input);
    }

    public void updateResult(){
        if(!selectedView && bookEnchants == null) return;
        RESULT.setItem(buildResult(), true);
    }

    @Override
    public @NotNull EnchantTableBlock getBlock() {
        return block;
    }

    @Override
    public boolean giveItemUponClose() {
        return !container.isOpening();
    }

    public enum CanUpgradeEnchant{
        YES,
        NOT_ENOUGH_POWER,
        MAX_LEVEL,
        HAS_CONFLICTS,
    }
}
