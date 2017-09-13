package com.builtbroken.mc.seven.framework.json.world;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.registry.implement.IPostInit;
import com.builtbroken.mc.framework.json.debug.IJsonDebugDisplay;
import com.builtbroken.mc.framework.json.imp.IJsonProcessor;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.framework.json.processors.JsonGenData;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.lib.world.generator.OreGenReplace;
import com.builtbroken.mc.lib.world.generator.OreGeneratorSettings;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/10/2017.
 */
public class JsonWorldOreGenData extends JsonGenData implements IPostInit, IJsonDebugDisplay
{
    public Object block;

    public String key;

    @JsonProcessorData("type")
    public String type;

    public int min_y;
    public int max_y;

    public int branchSize;
    public int chunkLimit;

    public JsonWorldOreGenData(IJsonProcessor processor, Object block, String key, int min, int max, int branch, int chunk)
    {
        super(processor);
        this.block = block;
        this.key = key;
        this.min_y = min;
        this.max_y = max;
        this.branchSize = branch;
        this.chunkLimit = chunk;
    }

    @Override
    public void onPostInit()
    {
        ItemStack stack = toStack(block);
        if (stack != null && stack.getItem() instanceof ItemBlock)
        {
            if (Engine.loaderInstance.getConfig().getBoolean((modName != null ? modName + ":" : "") + LanguageUtility.capitalizeFirst(key) + "_Ore", "WorldGen", true, "Enables generation of the ore in the world"))
            {
                GameRegistry.registerWorldGenerator(new OreGenReplace(((ItemBlock) stack.getItem()).field_150939_a, stack.getItemDamage(), new OreGeneratorSettings(min_y, max_y, chunkLimit, branchSize), "pickaxe", 1), 1);
            }
        }
        else
        {
            Engine.logger().error("JsonWorldOreGenData: stack '" + stack + "' can not be used for world generation for generator '" + key + "'.");
        }
    }

    @Override
    public String getContentID()
    {
        return key;
    }

    @Override
    public void addDebugLines(List<String> lines)
    {
        lines.add("Block: " + block);
        lines.add("Type: " + type);
        lines.add("Y level: " + min_y + " - " + max_y);
        lines.add("Branch Size: " + branchSize);
        lines.add("Chunk Limit: " + chunkLimit);
    }
}
