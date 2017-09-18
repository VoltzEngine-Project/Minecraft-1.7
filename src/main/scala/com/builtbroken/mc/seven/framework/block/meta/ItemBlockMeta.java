package com.builtbroken.mc.seven.framework.block.meta;

import com.builtbroken.mc.client.json.render.RenderData;
import com.builtbroken.mc.framework.json.imp.IJSONMetaConvert;
import com.builtbroken.mc.seven.framework.block.ItemBlockBase;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/26/2016.
 */
public class ItemBlockMeta extends ItemBlockBase implements IJSONMetaConvert
{
    /** Quick cache of localization by meta data to provide a small speed bonus */
    public String[] localizationCache = new String[16];

    public ItemBlockMeta(Block block)
    {
        super(block);
        this.setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage)
    {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack itemstack)
    {
        int damage = itemstack.getItemDamage();
        if (damage >= 0 && damage < 16)
        {
            if (localizationCache[damage] != null)
            {
                return localizationCache[damage];
            }

            //Assemble lang key, only called once per run
            String lang = getUnlocalizedName();
            lang = lang.replace(BlockMeta.META_INDEX_LOCALIZATION_KEY, "" + damage);
            if (getBlockJson().metaDataValues[damage] != null && getBlockJson().metaDataValues[damage].localization != null)
            {
                lang = lang.replace(BlockMeta.META_NAME_LOCALIZATION_KEY, getBlockJson().metaDataValues[damage].localization);
            }

            //Cache and return
            localizationCache[damage] = lang;
            return lang;
        }
        return getUnlocalizedName();
    }

    protected List<String> getIconStateKeys(RenderData data, int meta, int pass)
    {
        List<String> keys = new ArrayList();
        if (meta >= 0 && meta < 16 && getBlockJson().metaDataValues[meta] != null)
        {
            keys.add(RenderData.INVENTORY_RENDER_KEY + "." + getBlockJson().metaDataValues[meta].ID + "." + pass);
            keys.add(RenderData.INVENTORY_RENDER_KEY + "." + getBlockJson().metaDataValues[meta].ID);
            keys.add(getBlockJson().metaDataValues[meta].ID);
        }
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + meta + "." + pass);
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + meta);
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + pass);
        keys.add(RenderData.INVENTORY_RENDER_KEY);

        return keys;
    }

    @Override
    public String getUnlocalizedName()
    {
        return getBlockJson().getUnlocalizedName();
    }

    public BlockMeta getBlockJson()
    {
        return (BlockMeta) this.field_150939_a;
    }

    @Override
    public String toString()
    {
        return "ItemBlock[" + getBlockJson() + "]";
    }

    @Override
    public int getMetaForValue(String value)
    {
        for (MetaData data : getBlockJson().metaDataValues) //TODO cache in hashmap to improve lookup
        {
            if (data.ID.equalsIgnoreCase(value))
            {
                return data.index;
            }
        }
        return -1;
    }
}
