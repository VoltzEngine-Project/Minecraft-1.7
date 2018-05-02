package com.builtbroken.mc.seven.client;

import com.builtbroken.mc.client.ExplosiveRegistryClient;
import com.builtbroken.mc.client.effects.VisualEffectRegistry;
import com.builtbroken.mc.client.effects.providers.*;
import com.builtbroken.mc.client.gui.GuiQuickAccess;
import com.builtbroken.mc.client.helpers.ItemTextureBaker;
import com.builtbroken.mc.client.json.ClientDataHandler;
import com.builtbroken.mc.client.json.IJsonRenderStateProvider;
import com.builtbroken.mc.client.json.audio.AudioJsonProcessor;
import com.builtbroken.mc.client.json.effects.EffectJsonProcessor;
import com.builtbroken.mc.client.json.effects.EffectListJsonProcessor;
import com.builtbroken.mc.client.json.models.ModelJsonProcessor;
import com.builtbroken.mc.client.json.models.cube.JsonConverterModelCube;
import com.builtbroken.mc.client.json.render.RenderData;
import com.builtbroken.mc.client.json.render.item.ItemJsonRenderer;
import com.builtbroken.mc.client.json.render.processor.RenderJsonProcessor;
import com.builtbroken.mc.client.json.render.tile.TileRenderData;
import com.builtbroken.mc.client.json.texture.TextureJsonProcessor;
import com.builtbroken.mc.core.ConfigValues;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.core.commands.CommandVE;
import com.builtbroken.mc.core.commands.json.visuals.CommandJsonRender;
import com.builtbroken.mc.core.content.blast.emp.ExEmp;
import com.builtbroken.mc.core.content.blast.holiday.ExGiftClient;
import com.builtbroken.mc.core.content.entity.bat.ex.EntityExBat;
import com.builtbroken.mc.core.content.entity.bat.ex.RenderExBat;
import com.builtbroken.mc.core.content.entity.creeper.EntityExCreeper;
import com.builtbroken.mc.core.content.entity.creeper.RenderExCreeper;
import com.builtbroken.mc.core.handler.PlayerKeyHandler;
import com.builtbroken.mc.core.handler.RenderSelection;
import com.builtbroken.mc.core.network.packet.callback.chunk.PacketRequestData;
import com.builtbroken.mc.core.registry.ClientRegistryProxy;
import com.builtbroken.mc.core.registry.ModManager;
import com.builtbroken.mc.framework.access.global.GlobalAccessSystem;
import com.builtbroken.mc.framework.block.imp.ITileEventListener;
import com.builtbroken.mc.framework.explosive.ExplosiveRegistry;
import com.builtbroken.mc.framework.guide.GuideBookModule;
import com.builtbroken.mc.framework.guide.GuideEntry;
import com.builtbroken.mc.framework.json.JsonContentLoader;
import com.builtbroken.mc.framework.json.imp.IJsonGenObject;
import com.builtbroken.mc.framework.json.imp.JsonLoadPhase;
import com.builtbroken.mc.framework.json.loading.JsonLoader;
import com.builtbroken.mc.framework.multiblock.MultiBlockRenderHelper;
import com.builtbroken.mc.lib.render.block.BlockRenderHandler;
import com.builtbroken.mc.lib.world.map.block.ExtendedBlockDataManager;
import com.builtbroken.mc.lib.world.map.data.ChunkData;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.seven.CommonProxy;
import com.builtbroken.mc.seven.EngineLoaderMod;
import com.builtbroken.mc.seven.abstraction.MinecraftWrapper;
import com.builtbroken.mc.seven.abstraction.MinecraftWrapperClient;
import com.builtbroken.mc.seven.client.json.tile.TileRenderHandler;
import com.builtbroken.mc.seven.client.listeners.blocks.JsonIconListener;
import com.builtbroken.mc.seven.client.listeners.blocks.RotatableIconListener;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.builtbroken.mc.seven.framework.block.json.JsonBlockListenerProcessor;
import com.builtbroken.mc.seven.framework.block.listeners.RotatableMCListener;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Voltz Engine client proxy
 */
public class ClientProxy extends CommonProxy
{
    @Override
    public void onLoad()
    {
        ModManager.proxy = new ClientRegistryProxy();
        Engine.minecraft = MinecraftWrapper.INSTANCE = new MinecraftWrapperClient();
        EngineLoaderMod.instance.loader.applyModule(GuideBookModule.INSTANCE);
    }

    @Override
    public void preInit()
    {
        if (Engine.runningAsDev)
        {
            showDebugWindow();
        }
        FMLCommonHandler.instance().bus().register(new ExplosiveRegistryClient());
        MinecraftForge.EVENT_BUS.register(new ExplosiveRegistryClient());

        RenderingRegistry.registerBlockHandler(new BlockRenderHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerKeyHandler());
        MinecraftForge.EVENT_BUS.register(new RenderSelection());

        //Load in processors for client side json
        JsonContentLoader.INSTANCE.add(new TextureJsonProcessor());
        JsonContentLoader.INSTANCE.add(new ModelJsonProcessor());
        JsonContentLoader.INSTANCE.add(new RenderJsonProcessor());
        JsonContentLoader.INSTANCE.add(new AudioJsonProcessor());
        JsonContentLoader.INSTANCE.add(EffectJsonProcessor.INSTANCE);
        JsonContentLoader.INSTANCE.add(new EffectListJsonProcessor());

        //Textures have to be loaded in pre-init or will fail
        JsonContentLoader.INSTANCE.process("texture");
        JsonContentLoader.INSTANCE.triggerPhase(JsonLoadPhase.ASSETS);
        MinecraftForge.EVENT_BUS.register(ClientDataHandler.INSTANCE);

        //Register icons for explosives
        ExplosiveRegistryClient.registerIcon(new ItemStack(Items.gunpowder), References.PREFIX + "ex.icon.gunpowder");
        ExplosiveRegistryClient.registerIcon(new ItemStack(Items.skull, 1, 4), References.PREFIX + "ex.icon.creeper_head");
        ExplosiveRegistryClient.registerIcon(new ItemStack(Blocks.tnt), References.PREFIX + "ex.icon.tnt");

        VisualEffectRegistry.addEffectProvider(new VEProviderShockWave());
        VisualEffectRegistry.addEffectProvider(new VEProviderLaserBeam());
        VisualEffectRegistry.addEffectProvider(new VEProviderSmokeStream());
        VisualEffectRegistry.addEffectProvider(new VEProviderRocketTrail());
        VisualEffectRegistry.addEffectProvider(new VEProviderBlood());

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void loadJsonContentHandlers()
    {
        super.loadJsonContentHandlers();
        JsonBlockListenerProcessor.addBuilder(new RotatableIconListener.Builder());
        JsonBlockListenerProcessor.addBuilder(new JsonIconListener.Builder());
        JsonLoader.addConverter(new JsonConverterModelCube());

        if (Engine.runningAsDev)
        {
            CommandVE.INSTANCE.addToDebugCommand(new CommandJsonRender());
        }
    }

    @Override
    public void init()
    {
        super.init();

        //Register client side version of blasts
        ExplosiveRegistry.registerOrGetExplosive(References.DOMAIN, "Emp", new ExEmp());
        ExplosiveRegistry.registerOrGetExplosive(References.DOMAIN, "Gift", new ExGiftClient());

        //Register graphics
        RenderingRegistry.registerEntityRenderingHandler(EntityExCreeper.class, new RenderExCreeper());
        RenderingRegistry.registerEntityRenderingHandler(EntityExBat.class, new RenderExBat());

        //Register graphics handlers
        if (Engine.multiBlock != null)
        {
            RenderingRegistry.registerBlockHandler(MultiBlockRenderHelper.INSTANCE);
        }

        //Register tile renders
        TileRenderHandler tileRenderHandler = new TileRenderHandler();
        for (RenderData data : ClientDataHandler.INSTANCE.renderData.values())
        {
            if (data instanceof TileRenderData && ((TileRenderData) data).tileClass != null)
            {
                ClientRegistry.bindTileEntitySpecialRenderer(((TileRenderData) data).tileClass, tileRenderHandler);
            }
        }
    }

    @Override
    public void postInit()
    {
        super.postInit();
        //Item that uses a model for all states
        registerItemJsonRenders(new ItemJsonRenderer(), "VE-Item", "item", "tile", "block");

        List<IJsonGenObject> objects = JsonContentLoader.INSTANCE.generatedObjects.get(References.JSON_ITEM_KEY);
        if (objects != null && !objects.isEmpty())
        {
            for (IJsonGenObject object : objects)
            {
                if (object instanceof BlockBase)
                {
                    List<ITileEventListener> listeners = ((BlockBase) object).listeners.get("placement");
                    if (listeners != null && !listeners.isEmpty())
                    {
                        for (ITileEventListener listener : listeners)
                        {
                            if (listener instanceof RotatableMCListener)
                            {
                                ((BlockBase) object).addListener(new RotatableIconListener((BlockBase) object));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Called to loop through all registered json content to register
     * items to item renders.
     *
     * @param keys     - keys for the render type supported
     * @param renderer - render handler
     */
    public static int registerItemJsonRenders(IItemRenderer renderer, String... keys)
    {
        int count = 0;
        for (List<IJsonGenObject> list : JsonContentLoader.INSTANCE.generatedObjects.values())
        {
            if (list != null && !list.isEmpty())
            {
                for (IJsonGenObject obj : list)
                {
                    Item item = null;
                    if (obj instanceof Item)
                    {
                        item = (Item) obj;
                    }
                    else if (obj instanceof Block)
                    {
                        item = Item.getItemFromBlock((Block) obj);
                    }
                    if (item != null)
                    {
                        if (registerItemJsonRender(renderer, item, keys))
                        {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public static boolean registerItemJsonRender(IItemRenderer renderer, Item item, String... keys)
    {
        if (item instanceof IJsonRenderStateProvider)
        {
            List<String> ids = ((IJsonRenderStateProvider) item).getRenderContentIDs();
            for (String id : ids)
            {
                RenderData data = ClientDataHandler.INSTANCE.getRenderData(id);
                if (data != null)
                {
                    for (String key : keys)
                    {
                        if (data.renderType.equalsIgnoreCase(key))
                        {
                            MinecraftForgeClient.registerItemRenderer(item, renderer);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void bakeItemTextures()
    {
        //ItemTextureBaker.saveTextureToFolder("renders/test", new ItemStack(Engine.itemWrench));

        InventoryUtility.mapItems();
        for (Map.Entry<String, List<Item>> entry : InventoryUtility.getModToItems().entrySet())
        {
            if (entry.getValue() != null && entry.getValue() != null)
            {
                for (Item item : entry.getValue())
                {
                    List list = new ArrayList();
                    for (CreativeTabs tab : item.getCreativeTabs())
                    {
                        item.getSubItems(item, tab, list);
                    }

                    //TODO remove duplications

                    for (Object object : list)
                    {
                        if (object instanceof ItemStack)
                        {
                            ItemTextureBaker.saveTextureToFolder("renders/" + entry.getKey(), (ItemStack) object);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void keyHandler(InputEvent.KeyInputEvent e)
    {
        final int key = Keyboard.getEventKey();
        if (Minecraft.getMinecraft() != null) //Prevent key bind from working on loading screen and main menu
        {
            boolean crtl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
            if (Minecraft.getMinecraft().theWorld != null && Minecraft.getMinecraft().thePlayer != null)
            {
                //TODO add config for key binding
                if (key == Keyboard.KEY_GRAVE && crtl)
                {
                    openQuickAccessGUI();
                }
                else if (key == Keyboard.KEY_HOME && crtl)
                {
                    showDebugWindow();
                }
                else if (key == Keyboard.KEY_END && crtl)
                {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("Starting item render baking");
                    bakeItemTextures();
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("Done baking item renders");
                }
            }
        }
    }

    public static void openQuickAccessGUI()
    {
        if (!(Minecraft.getMinecraft().currentScreen instanceof GuiQuickAccess)) //TODO check previous GUI to prevent bugs (e.g. prevent opening on death screen)
        {
            //Close previous
            if (Minecraft.getMinecraft().currentScreen != null)
            {
                Minecraft.getMinecraft().currentScreen.onGuiClosed();
            }

            //Open
            Minecraft.getMinecraft().displayGuiScreen(new GuiQuickAccess());
        }
    }

    @Override
    public void openPermissionGUI(String profileID)
    {
        GlobalAccessSystem.openGUI(profileID);
    }

    @Override
    public void openHelpGUI(GuideEntry entry)
    {
        //TODO figure out what type of entry it is (book, chapter, section, page)
        //TODO open page if exists
        //TODO if doesn't exist, showing missing page and note the pages are community generated. Try to include links to matching book.
    }

    @SubscribeEvent
    public void clientUpdate(TickEvent.WorldTickEvent event)
    {
        if (ConfigValues.enableExtendedMetaPacketSync) ///TODO reduce check to every 10 ticks
        {
            try
            {
                if (event.side == Side.CLIENT)
                {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (mc != null)
                    {
                        EntityPlayer player = mc.thePlayer;
                        World world = mc.theWorld;
                        if (player != null && world != null)
                        {
                            if (ExtendedBlockDataManager.CLIENT.dimID != world.provider.dimensionId)
                            {
                                ExtendedBlockDataManager.CLIENT.clear();
                                ExtendedBlockDataManager.CLIENT.dimID = world.provider.dimensionId;
                            }
                            int renderDistance = mc.gameSettings.renderDistanceChunks + 2;
                            int centerX = ((int) Math.floor(player.posX)) >> 4;
                            int centerZ = ((int) Math.floor(player.posZ)) >> 4;

                            //Clear out chunks outside of render distance
                            List<ChunkData> chunksToRemove = new ArrayList();
                            for (ChunkData data : ExtendedBlockDataManager.CLIENT.chunks.values())
                            {
                                if (Math.abs(data.position.chunkXPos - centerX) > renderDistance || Math.abs(data.position.chunkZPos - centerZ) > renderDistance)
                                {
                                    chunksToRemove.add(data);
                                }
                            }

                            for (ChunkData data : chunksToRemove)
                            {
                                ExtendedBlockDataManager.CLIENT.chunks.remove(data.position);
                            }

                            renderDistance = mc.gameSettings.renderDistanceChunks;
                            for (int x = centerX - renderDistance; x < centerX + renderDistance; x++)
                            {
                                for (int z = centerZ - renderDistance; z < centerZ + renderDistance; z++)
                                {
                                    ChunkData chunkData = ExtendedBlockDataManager.CLIENT.getChunk(x, z);
                                    if (chunkData == null)
                                    {
                                        Engine.packetHandler.sendToServer(new PacketRequestData(world.provider.dimensionId, x, z, 0));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Engine.logger().error("Unexpected error while updating client chunk data state", e);
            }
        }
    }

    public boolean isPaused()
    {
        if (FMLClientHandler.instance().getClient().isSingleplayer() && !FMLClientHandler.instance().getClient().getIntegratedServer().getPublic())
        {
            GuiScreen screen = FMLClientHandler.instance().getClient().currentScreen;

            if (screen != null)
            {
                if (screen.doesGuiPauseGame())
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
    {
        return null;
    }

    @Override
    public EntityClientPlayerMP getClientPlayer()
    {
        return Minecraft.getMinecraft().thePlayer;
    }
}
