package com.builtbroken.mc.seven.framework.block;

import com.builtbroken.mc.client.json.ClientDataHandler;
import com.builtbroken.mc.framework.json.imp.IJsonProcessor;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.framework.json.processors.JsonGenData;
import com.builtbroken.mc.framework.json.settings.JsonSettingData;
import com.builtbroken.mc.imp.transform.region.Cube;
import com.builtbroken.mc.lib.helper.MaterialDict;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.seven.framework.block.tile.ITileProvider;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

import java.util.HashMap;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/3/2017.
 */
public class BlockPropertyData extends JsonGenData
{
    /** Unique id to register the block with */
    public final String registryKey;
    /** Mod that owners the block */
    public final String MOD;
    /** Name of the block, used for localizations */
    public final String name;

    /** Localization of the block */
    public String localization = "${mod}:${name}";
    /** Global ore dict name of the block */
    public String oreName;

    /** Handles supplying the tile entity for the block */
    public ITileProvider tileEntityProvider;

    /** Block hosting this data */
    public BlockBase block;

    //Block data
    private Material material = Material.clay;
    private String materialName = "clay";
    private String itemToDrop = null;
    private String harvestTool = null;
    private boolean isOpaqueCube;
    private boolean renderAsNormalBlock = true;
    private boolean supportsRedstone = false;
    private boolean hasComparatorInputOverride = false;
    private boolean isAlpha = false;
    private boolean isSolid = false;
    private boolean isNormalCube = false;
    private boolean canSilkHarvest = false;
    private boolean useAllRenderPasses = false;
    private float hardness = 5;
    private float resistance = 5;
    private int renderType = 0;
    private int color = -1;
    private int lightValue;
    private int itemDropCount = 1;
    private int harvestLevel = -1;

    private Cube renderBounds = Cube.FULL;
    private Cube blockBounds = Cube.FULL;
    private Cube selectionBounds = Cube.FULL;

    private HashMap<String, JsonSettingData> settings = new HashMap();


    public BlockPropertyData(IJsonProcessor processor, String registryKey, String MOD, String name)
    {
        super(processor);
        this.registryKey = registryKey;
        this.MOD = MOD;
        this.name = name;
    }

    @Override
    public String getMod()
    {
        return MOD;
    }

    @Override
    public String getContentID()
    {
        return registryKey;
    }

    public Material getMaterial()
    {
        return material;
    }

    @JsonProcessorData("material")
    public void setMaterial(String matName)
    {
        this.materialName = matName;
        this.material = MaterialDict.get(matName);
    }

    public float getHardness()
    {
        return hardness;
    }

    @JsonProcessorData(value = "hardness", type = "float", allowRuntimeChanges = true)
    public void setHardness(float hardness)
    {
        this.hardness = hardness;
    }

    public float getResistance()
    {
        return resistance;
    }

    @JsonProcessorData(value = "resistance", type = "float", allowRuntimeChanges = true)
    public void setResistance(float resistance)
    {
        this.resistance = resistance;
    }

    public int getRenderType()
    {
        return renderType;
    }

    @JsonProcessorData(value = "renderType", type = "int", loadForServer = false)  //TODO move to render data
    @SideOnly(Side.CLIENT)
    public void setRenderType(int renderType)
    {
        this.renderType = renderType;
    }

    @JsonProcessorData(value = "renderTypeName", loadForServer = false)  //TODO move to render data
    @SideOnly(Side.CLIENT)
    public void setRenderTypeName(String renderType)
    {
        ISimpleBlockRenderingHandler handler = ClientDataHandler.INSTANCE.getBlockRender(renderType);
        if (handler != null)
        {
            setRenderType(handler.getRenderId());
        }
        else
        {
            throw new IllegalArgumentException("RenderType[" + renderType + "] was not registered and thus can't be used.");
        }
    }

    public int getColor()
    {
        return color;
    }

    @JsonProcessorData(value = "renderColor", type = "int")  //TODO move to render data
    public void setColor(int color)
    {
        this.color = color;
    }

    public boolean isOpaqueCube()
    {
        return isOpaqueCube;
    }

    @JsonProcessorData(value = {"isOpaqueCube", "isOpaque"})
    public void setOpaqueCube(boolean opaqueCube)
    {
        this.isOpaqueCube = opaqueCube;
    }

    public boolean isSupportsRedstone()
    {
        return supportsRedstone;
    }

    @JsonProcessorData("supportsRedstone")
    public void setSupportsRedstone(boolean supportsRedstone)
    {
        this.supportsRedstone = supportsRedstone;
    }

    public boolean isAlpha()
    {
        return isAlpha;
    }

    @JsonProcessorData(value = "hasAlphaTextures", loadForServer = false) //TODO move to render data
    public void setAlpha(boolean alpha)
    {
        isAlpha = alpha;
    }

    public int getLightValue()
    {
        return lightValue;
    }

    @JsonProcessorData(value = "lightOutput", type = "int", allowRuntimeChanges = true)
    public void setLightValue(int lightValue)
    {
        this.lightValue = lightValue;
    }

    @JsonProcessorData(value = "renderAsNormalBlock")
    public void setRenderAsNormalBlock(boolean b)
    {
        this.renderAsNormalBlock = b;
    }

    public boolean renderAsNormalBlock()
    {
        return renderAsNormalBlock;
    }

    @JsonProcessorData(value = "hasComparatorInputOverride")
    public void setHasComparatorInputOverride(boolean b)
    {
        this.hasComparatorInputOverride = b;
    }

    public boolean hasComparatorInputOverride()
    {
        return hasComparatorInputOverride;
    }

    public Cube getRenderBounds()
    {
        return renderBounds;
    }

    @JsonProcessorData(value = "renderBounds", type = "cube")
    public void setRenderBounds(Cube renderBounds)
    {
        this.renderBounds = renderBounds;
    }

    public Cube getBlockBounds()
    {
        return blockBounds;
    }

    @JsonProcessorData(value = "blockBounds", type = "cube")
    public void setBlockBounds(Cube blockBounds)
    {
        this.blockBounds = blockBounds;
        if (block != null)
        {
            block.setBlockBounds(blockBounds.min().xf(), blockBounds.min().yf(), blockBounds.min().zf(), blockBounds.max().xf(), blockBounds.max().yf(), blockBounds.max().zf());
        }
    }

    public Cube getSelectionBounds()
    {
        return selectionBounds;
    }

    @JsonProcessorData(value = "selectionBounds", type = "cube")
    public void setSelectionBounds(Cube blockBounds)
    {
        this.selectionBounds = blockBounds;
    }

    public boolean isCanSilkHarvest()
    {
        return canSilkHarvest;
    }

    @JsonProcessorData(value = "canSilkHarvest")
    public void setCanSilkHarvest(boolean canSilkHarvest)
    {
        this.canSilkHarvest = canSilkHarvest;
    }

    public int getItemDropCount()
    {
        return itemDropCount;
    }

    @JsonProcessorData(value = "itemDropCount", type = "int", allowRuntimeChanges = true)
    public void setItemDropCount(int count)
    {
        this.itemDropCount = count;
    }

    public Item getItemToDrop()
    {
        return itemToDrop != null ? InventoryUtility.getItem(itemToDrop) : null; //TODO cache?
    }

    public String getItemToDropString()
    {
        return itemToDrop;
    }

    @JsonProcessorData(value = "itemToDrop")
    public void setItemToDrop(String item)
    {
        this.itemToDrop = item;
    }

    public boolean isSolid()
    {
        return isSolid;
    }

    @JsonProcessorData(value = "isSolid")
    public void setSolid(boolean solid)
    {
        isSolid = solid;
    }

    public boolean isNormalCube()
    {
        return isNormalCube;
    }

    @JsonProcessorData(value = "isNormalCube")
    public void setNormalCube(boolean normalCube)
    {
        isNormalCube = normalCube;
    }

    public int getHarvestLevel()
    {
        return harvestLevel;
    }

    @JsonProcessorData(value = "harvestToolLevel", type = "int", allowRuntimeChanges = true)
    public void setHarvestLevel(int harvestLevel)
    {
        this.harvestLevel = harvestLevel;
    }

    public String getHarvestTool()
    {
        return harvestTool;
    }

    @JsonProcessorData(value = "harvestTool", allowRuntimeChanges = true)
    public void setHarvestTool(String harvestTool)
    {
        this.harvestTool = harvestTool;
    }

    public boolean useAllRenderPasses()
    {
        return useAllRenderPasses;
    }

    @JsonProcessorData(value = "useAllRenderPasses")
    public void setUseAllRenderPasses(boolean useAllRenderPasses)
    {
        this.useAllRenderPasses = useAllRenderPasses;
    }

    //=============================================
    //========== Settings Handling ================
    //=============================================

    /**
     * Get settings data for modifying internal logic
     * <p>
     * Settings are independent of normal block functions
     * and are normally used for tile logic.
     *
     * @return map of settings [key : data]
     */
    public HashMap<String, JsonSettingData> getSettings()
    {
        return settings;
    }

    /**
     * Gets the setting for the key
     *
     * @param key - key
     * @return settings data object
     */
    public JsonSettingData getSetting(String key)
    {
        return settings.get(key);
    }

    /**
     * Used to check if a setting exists
     *
     * @param key
     * @return true if it exists
     */
    public boolean hasSetting(String key)
    {
        return getSetting(key) != null;
    }

    /**
     * Gets the value for the given setting as an int
     * <p>
     * Use {@link #hasSetting(String)} or {@link #getSetting(String)} to
     * ensure the value exists so not to use the default return.
     * <p>
     * As well if your unsure of the data type best to check
     * by using {@link #getSetting(String)} and doing an instance
     * check for {@link com.builtbroken.mc.framework.json.settings.data.JsonSettingInteger}
     * to be sure
     *
     * @param key
     * @return value or zero if can't be found
     */
    public int getSettingAsInt(String key)
    {
        JsonSettingData data = getSetting(key);
        if (data != null)
        {
            return data.getInt();
        }
        return 0;
    }
}
