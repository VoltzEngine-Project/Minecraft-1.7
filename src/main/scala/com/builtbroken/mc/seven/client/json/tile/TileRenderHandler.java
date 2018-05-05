package com.builtbroken.mc.seven.client.json.tile;

import com.builtbroken.jlib.data.Colors;
import com.builtbroken.mc.api.tile.access.IRotation;
import com.builtbroken.mc.api.tile.node.ITileNode;
import com.builtbroken.mc.api.tile.node.ITileNodeHost;
import com.builtbroken.mc.client.json.ClientDataHandler;
import com.builtbroken.mc.client.json.IJsonRenderStateProvider;
import com.builtbroken.mc.client.json.imp.IModelState;
import com.builtbroken.mc.client.json.imp.IRenderState;
import com.builtbroken.mc.client.json.render.RenderData;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.framework.block.imp.BlockListenerKeys;
import com.builtbroken.mc.framework.block.imp.ITileEventListener;
import com.builtbroken.mc.lib.helper.ReflectionUtility;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import com.builtbroken.mc.seven.framework.block.listeners.ListenerIterator;
import com.builtbroken.mc.seven.framework.block.listeners.client.ITileRenderListener;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/4/2017.
 */
public class TileRenderHandler extends TileEntitySpecialRenderer
{
    private static Map<Class, String> classToNameMap = new HashMap();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float f)
    {
        GL11.glPushMatrix();
        try
        {
            GL11.glTranslated(x + 0.5, y, z + 0.5);
            RenderData data = getRenderData(tile);
            if (data != null && data.renderType.equalsIgnoreCase("tile"))
            {

                //Build key set
                List<String> keysToTry = new ArrayList();

                ForgeDirection direction = getDirection(tile);
                if (direction != null)
                {
                    if (tile instanceof IJsonRenderStateProvider)
                    {
                        String state = ((IJsonRenderStateProvider) tile).getRenderStateKey(IItemRenderer.ItemRenderType.ENTITY, "tile", tile);
                        if (state != null)
                        {
                            keysToTry.add(state);
                        }
                    }
                    else if (tile instanceof ITileNodeHost && ((ITileNodeHost) tile).getTileNode() instanceof IJsonRenderStateProvider)
                    {
                        String state = ((IJsonRenderStateProvider) ((ITileNodeHost) tile).getTileNode()).getRenderStateKey(IItemRenderer.ItemRenderType.ENTITY, "tile", tile);
                        if (state != null)
                        {
                            keysToTry.add(state);
                        }
                    }
                    keysToTry.add("tile." + direction.name().toLowerCase());
                }

                keysToTry.add("tile." + tile.getBlockMetadata());
                keysToTry.add("tile");
                keysToTry.add("entity");
                keysToTry.add("item.entity");

                //Loop keys attempt each one
                for (String key : keysToTry)
                {
                    IRenderState state = data.getState(key);
                    if (state instanceof IModelState)
                    {
                        IModelState modelState = (IModelState) state;
                        if (modelState.render(false))
                        {
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Engine.logger().error("TileRenderHandler: Error rendering " + tile, e);
        }
        GL11.glPopMatrix();

        //If BlockBase, iterate listeners
        if (tile.getBlockType() instanceof BlockBase)
        {
            ListenerIterator it = new ListenerIterator(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, (BlockBase) tile.getBlockType(), "tilerender");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof ITileRenderListener)
                {
                    GL11.glPushMatrix();
                    try
                    {
                        ((ITileRenderListener) next).renderDynamic(tile, x, y, z, f);
                    }
                    catch (Exception e)
                    {
                        Engine.logger().error("TileRenderHandler: Error calling listener[" + next + "] for  Tile[" + tile + "]", e);
                    }
                    GL11.glPopMatrix();
                }
            }
        }

        if (Engine.runningAsDev && RenderManager.debugBoundingBox)
        {
            try
            {
                Block block = tile.getBlockType();
                if (block != null)
                {
                    List list = new ArrayList();
                    AxisAlignedBB bounds = AxisAlignedBB.getBoundingBox(tile.xCoord, tile.yCoord, tile.zCoord, tile.xCoord + 1, tile.yCoord + 1, tile.zCoord + 1);
                    if (bounds != null)
                    {
                        List<AxisAlignedBB> boxes = new ArrayList();
                        for (Entity entity : new Entity[]{Minecraft.getMinecraft().thePlayer, new EntityItem(tile.getWorldObj())})
                        {
                            block.addCollisionBoxesToList(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, bounds, list, entity);

                            GL11.glPushMatrix();
                            GL11.glDepthMask(false);
                            GL11.glDisable(GL11.GL_TEXTURE_2D);
                            GL11.glDisable(GL11.GL_LIGHTING);
                            GL11.glDisable(GL11.GL_CULL_FACE);
                            GL11.glDisable(GL11.GL_BLEND);

                            for (Object object : list)
                            {
                                if (object instanceof AxisAlignedBB)
                                {
                                    AxisAlignedBB b = (AxisAlignedBB) object;
                                    boolean contains = false;
                                    for (AxisAlignedBB bb : boxes)
                                    {
                                        if (bb.minX == b.minX && bb.minY == b.minY && bb.minZ == b.minZ
                                                && bb.maxX == b.maxX && bb.maxY == b.maxY && bb.maxZ == b.maxZ)
                                        {
                                            contains = true;
                                            break;
                                        }
                                    }
                                    if (!contains)
                                    {
                                        boxes.add(b);
                                        double bx = b.minX - tile.xCoord;
                                        double by = b.minY - tile.yCoord;
                                        double bz = b.minZ - tile.zCoord;
                                        double bxx = b.maxX - tile.xCoord;
                                        double byy = b.maxY - tile.yCoord;
                                        double bzz = b.maxZ - tile.zCoord;
                                        AxisAlignedBB renderBounds = AxisAlignedBB.getBoundingBox(x + bx, y + by, z + bz, x + bxx, y + byy, z + bzz);
                                        RenderGlobal.drawOutlinedBoundingBox(renderBounds, entity instanceof EntityPlayer ? Colors.DARK_BLUE.toInt() : Colors.BRIGHT_GREEN.toInt());
                                    }
                                }
                            }

                            GL11.glEnable(GL11.GL_TEXTURE_2D);
                            GL11.glEnable(GL11.GL_LIGHTING);
                            GL11.glEnable(GL11.GL_CULL_FACE);
                            GL11.glDisable(GL11.GL_BLEND);
                            GL11.glDepthMask(true);
                            GL11.glPopMatrix();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    protected RenderData getRenderData(TileEntity tile)
    {
        if (tile instanceof IJsonRenderStateProvider)
        {
            String id = ((IJsonRenderStateProvider) tile).getRenderContentID(IItemRenderer.ItemRenderType.ENTITY, tile);
            if (id != null)
            {
                RenderData data = ClientDataHandler.INSTANCE.getRenderData(id);
                if (data != null)
                {
                    return data;
                }
            }
        }
        else if (tile.getBlockType() instanceof IJsonRenderStateProvider)
        {
            String id = ((IJsonRenderStateProvider) tile.getBlockType()).getRenderContentID(IItemRenderer.ItemRenderType.ENTITY, tile);
            if (id != null)
            {
                RenderData data = ClientDataHandler.INSTANCE.getRenderData(id);
                if (data != null)
                {
                    return data;
                }
            }
        }
        else if (tile.getBlockType() instanceof BlockBase)
        {
            ListenerIterator iterator = new ListenerIterator(tile.getWorldObj(), tile.xCoord, tile.yCoord, tile.zCoord, (BlockBase) tile.getBlockType(), BlockListenerKeys.JSON_RENDER_STATE);
            while (iterator.hasNext())
            {
                ITileEventListener listener = iterator.next();
                if (listener instanceof IJsonRenderStateProvider)
                {
                    String id = ((IJsonRenderStateProvider) listener).getRenderContentID(IItemRenderer.ItemRenderType.ENTITY, tile);
                    if (id != null)
                    {
                        RenderData data = ClientDataHandler.INSTANCE.getRenderData(id);
                        if (data != null)
                        {
                            return data;
                        }
                    }
                }
            }
        }
        if (classToNameMap.isEmpty())
        {
            try
            {
                Field field = ReflectionUtility.getMCField(TileEntity.class, "classToNameMap", "field_145853_j");
                field.setAccessible(true);
                HashMap map = (HashMap) field.get(null);

                Set<Map.Entry> set = map.entrySet();
                for (Map.Entry entry : set)
                {
                    classToNameMap.put((Class) entry.getKey(), (String) entry.getValue());
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        Class clazz = tile.getClass();
        String id = classToNameMap.get(clazz);
        if (id == null)
        {
            id = tile.getClass().getName();
        }
        return ClientDataHandler.INSTANCE.getRenderData(id);
    }

    protected ForgeDirection getDirection(TileEntity tile)
    {
        if (tile instanceof IRotation && ((IRotation) tile).getDirection() != ForgeDirection.UNKNOWN && ((IRotation) tile).getDirection() != null)
        {
            return ((IRotation) tile).getDirection();
        }
        else if (tile instanceof ITileNodeHost)
        {
            ITileNode node = ((ITileNodeHost) tile).getTileNode();
            if (node instanceof IRotation && ((IRotation) node).getDirection() != ForgeDirection.UNKNOWN && ((IRotation) node).getDirection() != null)
            {
                return ((IRotation) node).getDirection();
            }
        }
        return null;
    }
}
