package com.builtbroken.mc.seven.framework.json.recipe.smelting;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.framework.json.data.JsonItemEntry;
import com.builtbroken.mc.seven.framework.block.IJsonBlockSubProcessor;
import com.builtbroken.mc.seven.framework.json.recipe.JsonRecipeProcessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Loads smelting recipes from a json for {@link net.minecraft.item.crafting.FurnaceRecipes}
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public class JsonFurnaceRecipeProcessor extends JsonRecipeProcessor<JsonFurnaceRecipeData> implements IJsonBlockSubProcessor
{
    @Override
    public String getMod()
    {
        return References.DOMAIN;
    }

    @Override
    public String getJsonKey()
    {
        return References.JSON_FURNACE_RECIPE_KEY;
    }

    @Override
    public String getLoadOrder()
    {
        return "after:" + References.JSON_ORENAME_KEY;
    }

    @Override
    public JsonFurnaceRecipeData process(final Object out, final JsonElement element)
    {
        final JsonObject recipeData = element.getAsJsonObject();

        ensureValuesExist(recipeData, "input");

        //Get output if it doesn't already exist
        Object output = out;
        if (output == null)
        {
            ensureValuesExist(recipeData, "output");
            output = getItemFromJson(recipeData.get("output"));
        }

        //Get input
        Object input = getItemFromJson(recipeData.get("input"));

        if (input instanceof JsonItemEntry && ((JsonItemEntry) input).nbt != null)
        {
            Engine.logger().error("JsonFurnaceRecipeProcessor: NBT is not supported for smelting recipe input, recipe: '" + input + "' -> '" + output + "'");
        }

        //Get XP if present
        float xp = 0;
        if (recipeData.has("xp"))
        {
            xp = recipeData.getAsJsonPrimitive("xp").getAsFloat();
            if (xp < 0)
            {
                throw new IllegalArgumentException("JsonFurnaceRecipeProcessor: xp for recipe must be positive, recipe: '" + input + "' -> '" + output + "'");
            }
        }

        //Make recipe
        return new JsonFurnaceRecipeData(this, input, output, xp);
    }
}
