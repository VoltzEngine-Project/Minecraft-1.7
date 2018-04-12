package com.builtbroken.mc.seven.framework.block.meta;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.framework.json.imp.IJSONMetaConvert;
import com.builtbroken.mc.framework.json.imp.JsonLoadPhase;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.builtbroken.mc.seven.framework.block.BlockPropertyData;
import com.builtbroken.mc.seven.framework.block.tile.ITileProvider;
import com.builtbroken.mc.seven.framework.block.tile.TileProviderMeta;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public class BlockMeta extends BlockBase implements IJSONMetaConvert
{
    public static final String META_INDEX_LOCALIZATION_KEY = "${meta}";
    public static final String META_NAME_LOCALIZATION_KEY = "${metaLocalization}";

    public MetaData[] metaDataValues = new MetaData[16];

    public BlockMeta(BlockPropertyData data)
    {
        super(data);
        ITileProvider provider = data.tileEntityProvider;
        data.tileEntityProvider = new TileProviderMeta();
        ((TileProviderMeta) data.tileEntityProvider).backupProvider = provider;
    }

    @Override
    public String getUnlocalizedName()
    {
        //Fix for some mods using unlocalized name for IDs
        return "tile." + InventoryUtility.getRegistryName(this);
    }

    @Override
    protected Class<? extends ItemBlock> getItemBlockClass()
    {
        return ItemBlockMeta.class;
    }

    @Override
    public void onPhase(JsonLoadPhase phase)
    {
        if (phase == JsonLoadPhase.LOAD_PHASE_TWO)
        {
            registerOreNames();
        }
    }

    public void registerOreNames()
    {
        //Register main ore name
        if (data.oreName != null)
        {
            if (!data.oreName.contains("$"))
            {
                OreDictionary.registerOre(data.oreName, new ItemStack(this));
            }
            else
            {
                //Option for automatic ore name selection using a format key with replacement entries
                for (int i = 0; i < metaDataValues.length; i++)
                {
                    MetaData data = getMetaData(i);
                    if (data != null)
                    {
                        String oreName = this.data.oreName.replace("${metaLocalization}", data.localization);
                        OreDictionary.registerOre(oreName, new ItemStack(this, 1, i));
                    }
                }
            }
        }

        //Load meta exclusive ore names
        for (int i = 0; i < metaDataValues.length; i++)
        {
            MetaData data = getMetaData(i);
            if (data != null && data.oreNames != null)
            {
                for (String s : data.oreNames)
                {
                    if (s != null && !s.isEmpty())
                    {
                        //TODO impalement formatting replacement
                        OreDictionary.registerOre(s, new ItemStack(this, 1, i));
                    }
                }
            }
        }
    }

    @Override
    protected void getRenderStates(Stack<String> stack, int side, int meta)
    {
        super.getRenderStates(stack, side, meta);
        MetaData data = getMetaData(meta);
        if (data != null)
        {
            stack.push(data.ID);
            stack.push("tile." + data.ID);
        }
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list)
    {
        super.getSubBlocks(item, creativeTabs, list);
        for (MetaData meta : metaDataValues)
        {
            if (meta != null && meta.index != 0)
            {
                list.add(new ItemStack(item, 1, meta.index));
            }
        }
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z)
    {
        return world.getBlockMetadata(x, y, z); //TODO add override for pickblock, as this was changed to fix it
    }

    @Override
    public int damageDropped(int meta)
    {
        MetaData data = getMetaData(meta);
        if (data != null)
        {
            if (data.getItemToDrop() != null)
            {
                return data.getItemToDrop().getItemDamage();
            }
            else if (data.dropIndex >= 0)
            {
                return data.dropIndex;
            }
        }
        return meta;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune)
    {
        MetaData data = getMetaData(meta);
        if (data != null && data.getItemToDrop() != null)
        {
            return data.getItemToDrop().getItem();
        }
        return super.getItemDropped(meta, random, fortune);
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random)
    {
        MetaData data = getMetaData(meta);
        if (data != null && data.getItemToDrop() != null)
        {
            final int count = Math.max(1, data.getItemToDrop().stackSize);
            if ((data.randomDropBonus > 0 || data.dropFortuneBonus) && Item.getItemFromBlock(this) != this.getItemDropped(0, random, fortune))
            {
                int randomBonus = data.randomDropBonus > 0 ? random.nextInt(data.randomDropBonus) : 0;
                return (count + randomBonus) * (data.dropFortuneBonus ? Math.max(1, fortune) : 1);
            }
            return count;
        }
        return super.quantityDropped(meta, fortune, random);
    }

    @Override
    public int getMetaForValue(String value)
    {
        for (MetaData data : metaDataValues)
        {
            if (data != null && data.ID.equalsIgnoreCase(value))
            {
                return data.index;
            }
        }
        return -1;
    }

    @Override
    protected int getBlockHarvestLevel(int metadata)
    {
        int level = super.getBlockHarvestLevel(metadata);
        MetaData data = getMetaData(metadata);
        if (data != null && data.harvestLevel > level)
        {
            level = data.harvestLevel;
        }
        return level;
    }

    @Override
    protected String getBlockHarvestTool(int metadata)
    {
        MetaData data = getMetaData(metadata);
        if (data != null && data.harvestTool != null)
        {
            return data.harvestTool;
        }
        return super.getBlockHarvestTool(metadata);
    }

    @Override
    public String toString()
    {
        return "BlockMeta[" + data.name + "]";
    }

    public MetaData getMetaData(int meta)
    {
        if (meta >= 0 && meta < metaDataValues.length)
        {
            return metaDataValues[meta];
        }
        else if (Engine.runningAsDev)
        {
            Engine.logger().error("BlockMeta#getMeta(" + meta + ") passed in an invalid meta value", new RuntimeException("stack"));
        }
        return null;
    }
}
