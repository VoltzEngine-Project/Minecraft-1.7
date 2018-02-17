package com.builtbroken.mc.seven;

import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.content.entity.bat.ex.EntityExBat;
import com.builtbroken.mc.core.content.entity.creeper.EntityExCreeper;
import com.builtbroken.mc.core.registry.CommonRegistryProxy;
import com.builtbroken.mc.core.registry.ModManager;
import com.builtbroken.mc.debug.gui.FrameDebug;
import com.builtbroken.mc.framework.guide.GuideEntry;
import com.builtbroken.mc.framework.mod.AbstractProxy;
import com.builtbroken.mc.seven.abstraction.MinecraftWrapper;
import cpw.mods.fml.common.registry.EntityRegistry;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;

/**
 * Shared loading functionality
 */
public class CommonProxy extends AbstractProxy
{
    public FrameDebug debugWindow;

    public void showDebugWindow()
    {
        if (Engine.runningAsDev && !GraphicsEnvironment.isHeadless())
        {
            if (debugWindow == null)
            {
                debugWindow = new FrameDebug();
            }
            debugWindow.setVisible(true);
        }
    }

    @Deprecated
    public EntityPlayer getClientPlayer()
    {
        return null;
    }

    public void onLoad()
    {
        ModManager.proxy = new CommonRegistryProxy();
        Engine.minecraft = MinecraftWrapper.INSTANCE = new MinecraftWrapper();
    }

    @Override
    public void init()
    {
        //EntityRegistry.registerGlobalEntityID(EntityExCreeper.class, "ExCreeper", EntityRegistry.findGlobalUniqueEntityId());

        //TODO move to JSON
        EntityRegistry.registerModEntity(EntityExCreeper.class, "ExCreeper", 55, Engine.loaderInstance, 100, 1, true);
        EntityRegistry.registerModEntity(EntityExBat.class, "ExBat", 56, Engine.loaderInstance, 100, 1, true);

    }

    /**
     * Opens the global access profile manager GUI
     *
     * @param profileID
     */
    public void openPermissionGUI(String profileID)
    {
        //TODO send packet
    }

    @Deprecated //was never implement, left in to avoid crashes
    public void openHelpGUI(String page_id)
    {

    }

    public void openHelpGUI(GuideEntry entry)
    {
        //TODO send packet
    }
}
