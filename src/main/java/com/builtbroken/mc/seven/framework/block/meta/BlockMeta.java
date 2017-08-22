package com.builtbroken.mc.seven.framework.block.meta;

import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.builtbroken.mc.seven.framework.block.BlockPropertyData;
import com.builtbroken.mc.seven.framework.block.tile.ITileProvider;
import com.builtbroken.mc.seven.framework.block.tile.TileProviderMeta;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public class BlockMeta extends BlockBase
{
    public static final String META_LOCAL_KEY = "${meta}";
    public MetaData[] metaDataValues = new MetaData[16];

    public BlockMeta(BlockPropertyData data)
    {
        super(data);
        ITileProvider provider = data.tileEntityProvider;
        data.tileEntityProvider = new TileProviderMeta();
        ((TileProviderMeta) data.tileEntityProvider).backupProvider = provider;
    }

    @Override
    protected Class<? extends ItemBlock> getItemBlockClass()
    {
        return ItemBlockMeta.class;
    }

    @Override
    public void onRegistered()
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
                    if (metaDataValues[i] != null)
                    {
                        String oreName = data.oreName.replace("${metaLocalization}", metaDataValues[i].localization);
                        OreDictionary.registerOre(oreName, new ItemStack(this, 1, i));
                    }
                }
            }
        }

        //Load meta exclusive ore names
        for (int i = 0; i < metaDataValues.length; i++)
        {
            if (metaDataValues[i] != null && metaDataValues[i].oreNames != null)
            {
                for (String s : metaDataValues[i].oreNames)
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
    public int damageDropped(int meta)
    {
        return meta;
    }

    @Override
    public String toString()
    {
        return "BlockMeta[" + data.name + "]";
    }
}
