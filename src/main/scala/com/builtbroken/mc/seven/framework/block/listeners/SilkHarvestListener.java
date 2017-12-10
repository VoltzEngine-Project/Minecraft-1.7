package com.builtbroken.mc.seven.framework.block.listeners;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.framework.block.imp.IBlockStackListener;
import com.builtbroken.mc.framework.block.imp.IDestroyedListener;
import com.builtbroken.mc.framework.block.imp.ITileEventListener;
import com.builtbroken.mc.framework.block.imp.ITileEventListenerBuilder;
import com.builtbroken.mc.framework.json.data.JsonItemEntry;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 12/6/2017.
 */
public class SilkHarvestListener extends TileListener implements IDestroyedListener, IBlockStackListener
{
    @JsonProcessorData("canHarvestNormal")
    public boolean canHarvestNormal = true;

    @JsonProcessorData(value = "silkDrop", type = "item")
    public JsonItemEntry itemEntry;

    private boolean error = false;
    private ItemStack cache = null;

    @Override
    public boolean canHarvest(EntityPlayer player, int meta)
    {
        return canHarvestNormal;
    }

    @Override
    public boolean collectSilkHarvestDrops(List<ItemStack> drops, EntityPlayer player, int meta)
    {
        if (cache != null)
        {
            drops.add(cache);
            return true;
        }
        else if (itemEntry != null && !error)
        {
            ItemStack stack = null;

            //Try to get stack, try-catch is used to due to get being able to produce errors
            try
            {
                stack = itemEntry.get();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if (stack != null)
            {
                cache = stack;
                drops.add(stack);
                return true;
            }
            else
            {
                Engine.logger().error("SilkHarvestListener: Failed to convert JSON item entry into ItemStack for Silk Harvest drops, disabling functionality to prevent spam. Item = " + itemEntry);
            }
        }
        return false;
    }

    @Override
    public boolean canSilkHarvest(EntityPlayer player, int meta)
    {
        return true;
    }

    @Override
    public List<String> getListenerKeys()
    {
        List<String> list = new ArrayList();
        list.add("break");
        list.add("blockStack");
        return list;
    }

    public static class Builder implements ITileEventListenerBuilder
    {
        @Override
        public ITileEventListener createListener(Block block)
        {
            return new SilkHarvestListener();
        }

        @Override
        public String getListenerKey()
        {
            return "silkHarvest";
        }
    }
}
