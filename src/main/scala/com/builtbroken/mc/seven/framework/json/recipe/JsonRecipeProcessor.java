package com.builtbroken.mc.seven.framework.json.recipe;

import com.builtbroken.mc.framework.json.imp.IJsonGenObject;
import com.builtbroken.mc.framework.json.processors.JsonProcessor;
import com.builtbroken.mc.framework.json.struct.JsonForLoop;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.builtbroken.mc.seven.framework.block.IJsonBlockSubProcessor;
import com.builtbroken.mc.seven.framework.block.meta.MetaData;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Prefab for any processor that uses item/block based recipes
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 3/10/2017.
 */
public abstract class JsonRecipeProcessor<D extends IJsonGenObject> extends JsonProcessor<D> implements IJsonBlockSubProcessor
{
    @Override
    public boolean process(JsonElement element, List<IJsonGenObject> objects)
    {
        handle(null, element, objects);
        return true;
    }

    protected void handle(final Object out, JsonElement element, List<IJsonGenObject> objects)
    {
        JsonObject jsonObject = element.getAsJsonObject();
        if (jsonObject.has("for"))
        {
            List<JsonObject> elements = new ArrayList();
            JsonForLoop.generateDataForLoop(jsonObject.getAsJsonObject("for"), elements, new HashMap(), 0);

            for(JsonObject object : elements)
            {
                D data = process(out, object);
                if (data != null)
                {
                    objects.add(data);
                }
            }
        }
        else if (jsonObject.has("forEach"))
        {
            List<JsonObject> elements = new ArrayList();
            JsonForLoop.generateDataForEachLoop(jsonObject.getAsJsonObject("forEach"), elements, new HashMap(), 0);

            for(JsonObject object : elements)
            {
                D data = process(out, object);
                if (data != null)
                {
                    objects.add(data);
                }
            }
        }
        else
        {
            D data = process(out, element);
            if (data != null)
            {
                objects.add(data);
            }
        }
    }

    /**
     * Called to process a recipe
     *
     * @param out     - optional, output item - if provided will not require output from recipe json
     * @param element - data containing the recipe
     * @return recipe data
     */
    public abstract D process(final Object out, final JsonElement element);

    @Override
    public void process(BlockBase block, JsonElement element, List<IJsonGenObject> objects)
    {
        handle(block, element, objects);
    }

    @Override
    public void process(MetaData data, BlockBase block, JsonElement element, List<IJsonGenObject> objects)
    {
        handle(new ItemStack(block, 1, data.index), element, objects);
    }
}
