package com.builtbroken.mc.seven.framework.block.meta;

import com.builtbroken.mc.framework.json.data.JsonItemEntry;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.seven.framework.block.tile.ITileProvider;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores data about a meta value
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/26/2016.
 */
public class MetaData
{
    /** Suffix to append to the block's localization */
    @JsonProcessorData(value = {"name", "localization"})
    public String localization;

    /** List of ore names to register after block is registered */
    public List<String> oreNames;

    /** Index in the meta array, between 0-15 */
    @JsonProcessorData(value = "meta", type = "int")
    public int index = -1;

    /** Index to drop when meta block is broken */
    @JsonProcessorData(value = "dropMeta", type = "int")
    public int dropIndex = -1;

    @JsonProcessorData(value = "randomDropBonus", type = "int")
    public int randomDropBonus = -1;

    @JsonProcessorData("useFortuneDropBonus")
    public boolean dropFortuneBonus;

    @JsonProcessorData(value = "itemToDrop", type = "item")
    public JsonItemEntry itemToDrop;
    public ItemStack _itemToDropCache;

    /** Object that creates tiles */
    public ITileProvider tileEntityProvider;

    /** Unique id of the meta value */
    public final String ID;

    public MetaData(String id)
    {
        this.ID = id;
    }


    public ItemStack getItemToDrop()
    {
        if (itemToDrop != null)
        {
            if (_itemToDropCache == null)
            {
                _itemToDropCache = itemToDrop.get();
            }
            return _itemToDropCache;
        }
        return null;
    }

    /**
     * Adds an ore name to be registered
     *
     * @param input
     */
    @JsonProcessorData(value = {"ore", "oreName"})
    public void addOreName(String input)
    {
        String name = input.trim();
        //TODO throw error if added after game has loaded
        if (oreNames == null)
        {
            oreNames = new ArrayList();
        }
        //TODO validate? order of words, camel case

        if (!oreNames.contains(name))
        {
            oreNames.add(name);
        }
    }
}
