package com.builtbroken.mc.seven.framework.json.recipe.crafting.shaped;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.content.tool.ItemSimpleCraftingTool;
import com.builtbroken.mc.framework.json.imp.IJsonProcessor;
import com.builtbroken.mc.framework.recipe.item.RecipeTool;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Holds onto recipe data until it can be converted.
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/9/2017.
 */
public class JsonToolRecipeData extends JsonShapedRecipeData
{
    protected static HashMap<String, Function<String, ItemStack>> toolFactories = new HashMap(); //TODO maybe move to an API for reuse

    static
    {
        //TODO load via JSON
        toolFactories.put("hammer", s -> ItemSimpleCraftingTool.getHammer());
        toolFactories.put("chisel", s -> ItemSimpleCraftingTool.getChisel());
        toolFactories.put("drill", s -> ItemSimpleCraftingTool.getDrill());
        toolFactories.put("cutter", s -> ItemSimpleCraftingTool.getCutters());
        toolFactories.put("file", s -> ItemSimpleCraftingTool.getFile());
    }

    public JsonToolRecipeData(IJsonProcessor processor, Object output, Object[] data, boolean largeGrid)
    {
        super(processor, output, data, largeGrid);
    }

    @Override
    public IRecipe getRecipe()
    {
        //Create recipe
        if (output instanceof Block)
        {
            return new RecipeTool((Block) output, data);
        }
        else if (output instanceof Item)
        {
            return new RecipeTool((Item) output, data);
        }
        else if (output instanceof ItemStack)
        {
            return new RecipeTool((ItemStack) output, data);
        }
        else
        {
            Engine.logger().error("The type of output value [" + output + "] could not be recognized for recipe creation. Recipe -> " + this);
        }
        return null;
    }

    @Override
    protected Object convert(Object in)
    {
        if (in instanceof String)
        {
            String value = (String) in;
            if (value.startsWith("tool@"))
            {
                value = value.substring(5, value.length());
                value = value.toLowerCase().trim();
                if (toolFactories.containsKey(value))
                {
                    ItemStack stack = toolFactories.get(value).apply(value);
                    if (stack != null && stack.getItem() != null)
                    {
                        return stack;
                    }
                }
            }
        }
        return convertItemEntry(in);
    }

    @Override
    public String toString()
    {
        return "JsonToolRecipeData[ out = " + output + ", data = " + data + "]";
    }

    @Override
    public String getContentID()
    {
        return null;
    }
}
