package com.minecolonies.coremod.items;

import com.minecolonies.coremod.blocks.ModBlocks;
import com.minecolonies.coremod.blocks.decorative.BlockShingleNew;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * Class handling the registering of the mod items.
 * <p>
 * We disabled the following finals since we are neither able to mark the items as final, nor do we want to provide public accessors.
 */
@SuppressWarnings({"squid:ClassVariableVisibilityCheck", "squid:S2444", "squid:S1444"})
public final class ModItems
{
    public static Item supplyChest;
    public static Item buildTool;
    public static Item scanTool;
    public static Item permTool;
    public static Item caliper;
    public static Item scepterGuard;
    public static Item supplyCamp;
    public static Item ancientTome;
    public static Item chiefSword;
    public static Item clipboard;
    public static Item itemCactusDoor;
    public static Item itemAchievementProxySettlement;
    public static Item itemAchievementProxyTown;
    public static Item itemAchievementProxyCity;
    public static Item itemAchievementProxyMetropolis;

    public static ItemBlock itemBlockShinglesNewTop;
    public static ItemBlock itemBlockShinglesNewBottom;

    /**
     * Private constructor to hide the implicit public one.
     */
    private ModItems()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Initates all the blocks. At the correct time.
     */
    public static void init(final IForgeRegistry<Item> registry)
    {
        supplyChest = new ItemSupplyChestDeployer();
        buildTool = new ItemBuildTool();
        scanTool = new ItemScanTool();
        permTool = new ItemScepterPermission();
        caliper = new ItemCaliper();
        scepterGuard = new ItemScepterGuard();
        supplyCamp = new ItemSupplyCampDeployer();
        ancientTome = new ItemAncientTome();
        chiefSword = new ItemChiefSword();
        clipboard = new ItemClipBoard();
        itemCactusDoor = new ItemCactusDoor(ModBlocks.blockCactusDoor, "cactusdoor");
        itemAchievementProxySettlement = new ItemAchievementProxy("sizeSettlement");
        itemAchievementProxyTown = new ItemAchievementProxy("sizeTown");
        itemAchievementProxyCity = new ItemAchievementProxy("sizeCity");
        itemAchievementProxyMetropolis = new ItemAchievementProxy("sizeMetropolis");

        itemBlockShinglesNewTop = new ItemBlockShingleNew(ModBlocks.blockShingleTop);
        itemBlockShinglesNewBottom = new ItemBlockShingleNew(ModBlocks.blockShingleBottom);

        registry.register(supplyChest);
        registry.register(buildTool);
        registry.register(scanTool);
        registry.register(permTool);
        registry.register(caliper);
        registry.register(scepterGuard);
        registry.register(supplyCamp);
        registry.register(ancientTome);
        registry.register(chiefSword);
        registry.register(itemAchievementProxySettlement);
        registry.register(itemAchievementProxyTown);
        registry.register(itemAchievementProxyCity);
        registry.register(itemAchievementProxyMetropolis);
        registry.register(clipboard);
        registry.register(itemCactusDoor);

        registry.register(itemBlockShinglesNewTop);
        registry.register(itemBlockShinglesNewBottom);
    }
}