package com.builtbroken.mc.seven.framework.block.json;

import com.builtbroken.mc.framework.json.imp.IJsonGenObject;
import com.builtbroken.mc.framework.json.settings.JsonSettingData;
import com.builtbroken.mc.framework.json.settings.JsonSettingsProcessor;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.List;

/**
 * Wrapper for {@link JsonSettingsProcessor} to handle loading settings for blocks
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 9/6/2017.
 */
public class JsonBlockSubProcessorSettings extends JsonBlockSubProcessor
{
    //TODO implement for meta values?
    @Override
    public void process(BlockBase block, JsonElement element, List<IJsonGenObject> objectList)
    {
        JsonArray array = element.getAsJsonArray();
        for (JsonElement e : array)
        {
            try
            {
                JsonSettingData data = JsonSettingsProcessor.INSTANCE.process(e);
                if (data != null)
                {
                    objectList.add(data);
                }
            }
            catch (Exception ex)
            {
                throw new RuntimeException("Failed to process setting entry for " + block + ", entry = " + e, ex);
            }
        }
    }
}
