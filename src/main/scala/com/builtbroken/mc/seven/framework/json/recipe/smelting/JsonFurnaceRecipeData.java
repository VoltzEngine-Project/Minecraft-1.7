package com.builtbroken.mc.seven.framework.json.recipe.smelting;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.registry.implement.IPostInit;
import com.builtbroken.mc.framework.json.data.JsonRecipeData;
import com.builtbroken.mc.debug.IJsonDebugDisplay;
import com.builtbroken.mc.framework.json.imp.IJsonGenObject;
import com.builtbroken.mc.framework.json.imp.IJsonProcessor;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.item.ItemStack;

import java.util.List;

/**
 * Object used to temp hold data about a smelting recipe while we wait on blocks to be registered
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/26/2016.
 */
public class JsonFurnaceRecipeData extends JsonRecipeData implements IJsonGenObject, IPostInit, IJsonDebugDisplay
{
    /** Input for the recipe */
    public final Object input;
    /** XP recipe of the recipe */
    public float xp;

    public JsonFurnaceRecipeData(IJsonProcessor processor, Object input, Object output, float xp)
    {
        super(processor, output);
        this.input = input;
        this.xp = xp;
    }

    @Override
    public void onPostInit()
    {
        ItemStack outputStack = toStack(output);
        ItemStack inputStack = toStack(input);
        if (outputStack != null && outputStack.getItem() != null)
        {
            if (inputStack != null && inputStack.getItem() != null)
            {
                GameRegistry.addSmelting(inputStack, outputStack, xp);
            }
            else
            {
                Engine.logger().error("JsonSmeltingRecipe: Failed to parse input for " + this);
            }
        }
        else

        {
            Engine.logger().error("JsonSmeltingRecipe: Failed to parse output for " + this);
        }
    }

    @Override
    public String toString()
    {
        return "JsonFurnaceRecipe[" + input + " -> " + output + "]";
    }

    @Override
    public String getContentID()
    {
        return input.toString();
    }

    @Override
    public String getDisplayName()
    {
        return getContentID();
    }

    @Override
    public void addDebugLines(List<String> lines)
    {
        lines.add("Input: " + toStack(input));
        lines.add("Output: " + toStack(output));
        lines.add("Xp: " + xp);
    }
}
