package com.builtbroken.mc.seven.framework.block.listeners;

import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.mc.api.IModObject;
import com.builtbroken.mc.api.abstraction.entity.IEntityData;
import com.builtbroken.mc.api.data.ActionResponse;
import com.builtbroken.mc.api.items.listeners.IItemWithListeners;
import com.builtbroken.mc.api.tile.access.IRotation;
import com.builtbroken.mc.api.tile.multiblock.IMultiTile;
import com.builtbroken.mc.api.tile.multiblock.IMultiTileHost;
import com.builtbroken.mc.api.tile.node.ITileNode;
import com.builtbroken.mc.api.tile.node.ITileNodeHost;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.framework.block.imp.*;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.framework.multiblock.BlockMultiblock;
import com.builtbroken.mc.framework.multiblock.MultiBlockHelper;
import com.builtbroken.mc.framework.multiblock.listeners.IMultiBlockLayoutListener;
import com.builtbroken.mc.framework.multiblock.listeners.IMultiBlockNodeListener;
import com.builtbroken.mc.framework.multiblock.structure.MultiBlockLayout;
import com.builtbroken.mc.framework.multiblock.structure.MultiBlockLayoutHandler;
import com.builtbroken.mc.imp.transform.vector.Location;
import com.builtbroken.mc.imp.transform.vector.Pos;
import com.builtbroken.mc.lib.helper.BlockUtility;
import com.builtbroken.mc.seven.framework.block.BlockBase;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/17/2017.
 */
public class MultiBlockListener extends TileListener implements IBlockListener, IPlacementListener, IDestroyedListener, IUpdateListener, IMultiTileHost
{
    public static final String JSON_CONTENT_ID = "%contentID%";

    private boolean _destroyingStructure = false;

    @JsonProcessorData("layoutKey")
    protected String _layoutKey;

    @JsonProcessorData("doRotation")
    protected boolean doRotation = false;

    @JsonProcessorData("useItemStack")
    protected boolean useItemStack = false;

    @JsonProcessorData("buildFirstTick")
    protected boolean buildFirstTick = true;

    @JsonProcessorData("directionOffset")
    protected boolean directionOffset = false;

    @JsonProcessorData(value = "directionMultiplier", type = "int")
    protected int directionMultiplier = 1;

    public final Block block;

    public MultiBlockListener(Block block)
    {
        this.block = block;
    }

    @Override
    public List<String> getListenerKeys()
    {
        List<String> list = new ArrayList();
        list.add("placement");
        list.add("break");
        list.add("break");
        list.add("update");
        list.add("multiblock");
        return list;
    }

    @Override
    public void update(long ticks)
    {
        if (world().isServer())
        {
            long offsetTick = (ticks + (Math.abs(this.xi() + this.yi() + this.zi())));
            if (ticks == 0 && buildFirstTick)
            {
                if (MultiBlockHelper.canBuild(world().unwrap(), getMultiTileHost(), true))
                {
                    MultiBlockHelper.buildMultiBlock(world().unwrap(), getMultiTileHost() != null ? getMultiTileHost() : this, true, true);
                }
                else
                {
                    Engine.logger().error("Can not build multiblock structure at location " + new Location(world().unwrap(), xi(), yi(), zi()) + " for " + getMultiTileHost());
                }
            }
            else if (offsetTick % 200 == 0)
            {
                MultiBlockHelper.buildMultiBlock(world().unwrap(), getMultiTileHost() != null ? getMultiTileHost() : this, true, true);
            }
        }
    }

    protected IMultiTileHost getMultiTileHost()
    {
        TileEntity tileEntity = getTileEntity();

        if (tileEntity instanceof IMultiTileHost)
        {
            return (IMultiTileHost) tileEntity;
        }
        else if (tileEntity instanceof ITileNodeHost)
        {
            ITileNode node = ((ITileNodeHost) tileEntity).getTileNode();
            if (node instanceof IMultiTileHost)
            {
                return (IMultiTileHost) node;
            }
        }
        return null;
    }

    @Override
    public void onMultiTileAdded(IMultiTile tileMulti)
    {
        Pos pos = getOffset(tileMulti);
        if (getLayoutOfMultiBlock().containsKey(pos))
        {
            tileMulti.setHost(getMultiTileHost());
            //Handle node
            TileEntity host = getTileEntity();
            if (host instanceof ITileNodeHost && ((ITileNodeHost) host).getTileNode() instanceof IMultiBlockNodeListener)
            {
                ((IMultiBlockNodeListener) ((ITileNodeHost) host).getTileNode()).onMultiTileAdded(tileMulti, pos);
            }
        }
    }

    public Pos getOffset(IMultiTile tileMulti)
    {
        return new Pos(tileMulti.xi(), tileMulti.yi(), tileMulti.zi()).sub(new Pos(this).floor());
    }

    @Override
    public boolean onMultiTileBroken(IMultiTile tileMulti, Object source, boolean harvest)
    {
        if (!_destroyingStructure && tileMulti instanceof TileEntity)
        {
            Pos pos = getOffset(tileMulti);
            HashMap<IPos3D, String> map = getLayoutOfMultiBlock();
            if (map != null && map.containsKey(pos))
            {
                _destroyingStructure = true;
                MultiBlockHelper.destroyMultiBlockStructure(getMultiTileHost() != null ? getMultiTileHost() : this, harvest, true, true);
                _destroyingStructure = false;
                return true;
            }
            else if (Engine.runningAsDev)
            {
                System.out.println("Error map was null");
            }
        }
        return false;
    }

    @Override
    public void onTileInvalidate(IMultiTile tileMulti)
    {

    }

    @Override
    public boolean onMultiTileActivated(IMultiTile tile, EntityPlayer player, int side, float xHit, float yHit, float zHit)
    {
        //Pass to handler
        Pos pos = getOffset(tile);
        TileEntity host = getTileEntity();
        if (host instanceof ITileNodeHost && ((ITileNodeHost) host).getTileNode() instanceof IMultiBlockNodeListener)
        {
            return ((IMultiBlockNodeListener) ((ITileNodeHost) host).getTileNode()).onMultiTileActivated(tile, pos, player, side, xHit, yHit, zHit);
        }

        //Pass to tile
        Object tileEntity = getMultiTileHost();
        if (tileEntity instanceof IActivationListener)
        {
            return ((IActivationListener) tileEntity).onPlayerActivated(player, side, xHit, yHit, zHit);
        }

        //Pass to block
        Block block = getBlock();
        if (!(block instanceof BlockMultiblock))
        {
            return block.onBlockActivated(world().unwrap(), xi(), yi(), zi(), player, side, xHit, yHit, zHit);
        }
        return false;
    }

    @Override
    public void onMultiTileClicked(IMultiTile tile, EntityPlayer player)
    {
        Object tileEntity = getMultiTileHost();
        if (tileEntity instanceof IActivationListener)
        {
            ((IActivationListener) tileEntity).onPlayerClicked(player);
        }
    }

    @Override
    public HashMap<IPos3D, String> getLayoutOfMultiBlock()
    {
        if (doRotation)
        {
            TileEntity tileEntity = getTileEntity();
            ForgeDirection dir = null;
            if (tileEntity instanceof IRotation)
            {
                dir = ((IRotation) tileEntity).getDirection();
            }
            else if (tileEntity instanceof ITileNodeHost && ((ITileNodeHost) tileEntity).getTileNode() instanceof IRotation)
            {
                dir = ((IRotation) ((ITileNodeHost) tileEntity).getTileNode()).getDirection();
            }
            return getLayoutOfMultiBlock(dir, null);
        }
        return MultiBlockLayoutHandler.get(getLayoutKey(null));
    }

    protected HashMap<IPos3D, String> getLayoutOfMultiBlock(ForgeDirection dir, ItemStack stack)
    {
        if (doRotation && dir != null && dir != ForgeDirection.UNKNOWN)
        {
            final String key = getLayoutKey(stack) + "." + dir.name().toLowerCase();

            HashMap<IPos3D, String> directionalMap = MultiBlockLayoutHandler.get(key);
            if (directionalMap == null && directionOffset)
            {
                HashMap<IPos3D, String> map = MultiBlockLayoutHandler.get(getLayoutKey(stack));
                if (map != null)
                {
                    directionalMap = new HashMap();
                    for (Map.Entry<IPos3D, String> entry : map.entrySet())
                    {
                        if (entry.getKey() != null)
                        {
                            Pos pos = entry.getKey() instanceof Pos ? (Pos) entry.getKey() : new Pos(entry.getKey());
                            pos = pos.add(new Pos(dir).multiply(directionMultiplier));
                            directionalMap.put(pos, entry.getValue());
                        }
                    }
                    MultiBlockLayout layout = new MultiBlockLayout(null, key);
                    layout.tiles.putAll(directionalMap);
                    MultiBlockLayoutHandler.register(layout);
                }
            }
            return directionalMap;
        }
        return MultiBlockLayoutHandler.get(getLayoutKey(stack));
    }

    @Override
    public void breakBlock(Block block, int meta)
    {
        MultiBlockHelper.destroyMultiBlockStructure(getMultiTileHost() != null ? getMultiTileHost() : this, true, true, false);
    }

    @Override
    public boolean removedByPlayer(EntityPlayer player, boolean willHarvest)
    {
        MultiBlockHelper.destroyMultiBlockStructure(getMultiTileHost() != null ? getMultiTileHost() : this, false, true, true);
        return true;
    }

    @Override
    public ActionResponse canPlaceAt()
    {
        //Due to rotation and stack checks, we have to skip or it will always fail due to missing data
        return (doRotation || useItemStack || canBuild(null, null)) ? ActionResponse.DO : ActionResponse.CANCEL;
    }

    @Override
    public ActionResponse canPlaceAt(IEntityData entity, ItemStack stack)
    {
        //If comparing with rotation or stack check layout, if not skip and let check run in canPlaceAt()
        return (!doRotation && !useItemStack || canBuild(BlockUtility.determineForgeDirection(entity), stack)) ? ActionResponse.DO : ActionResponse.CANCEL;
    }

    protected boolean canBuild(ForgeDirection direction, ItemStack stack)
    {
        return MultiBlockHelper.canBuild(world().unwrap(), xi(), yi(), zi(), getMultiTileHost() != null ? getMultiTileHost() : this, getLayoutOfMultiBlock(direction, stack), true);
    }

    @Override
    protected boolean isValidForRuntime()
    {
        return true;
    }

    public String getLayoutKey(ItemStack stack)
    {
        //Handling for item & nodes with listeners
        if (stack != null)
        {
            if (stack.getItem() instanceof IItemWithListeners)
            {
                //TODO implement listener for item
            }

            //Get key via listeners on the block
            if (stack.getItem() instanceof ItemBlock)
            {
                Block block = ((ItemBlock) stack.getItem()).field_150939_a;
                if (block instanceof BlockBase)
                {
                    List<ITileEventListener> listeners = ((BlockBase) block).listeners.get(BlockListenerKeys.MULTI_BLOCK_LAYOUT_LISTENER);
                    if (listeners != null)
                    {
                        for (ITileEventListener listener : listeners)
                        {
                            if (listener instanceof IMultiBlockLayoutListener)
                            {
                                String key = ((IMultiBlockLayoutListener) listener).getMultiBlockLayoutKey(stack);
                                if (key != null && !key.isEmpty())
                                {
                                    return key;
                                }
                            }
                        }
                    }
                }
            }
        }

        //Handle content ID
        if (_layoutKey != null && _layoutKey.contains(JSON_CONTENT_ID))
        {
            String contentID = "base";
            TileEntity tile = getTileEntity();
            if (tile instanceof ITileNodeHost)
            {
                contentID = ((ITileNodeHost) tile).getTileNode().getContentID();
            }
            else if (tile instanceof IModObject)
            {
                contentID = ((IModObject) tile).getContentID();
            }
            else if (block instanceof IModObject)
            {
                contentID = ((IModObject) block).getContentID();
            }
            return _layoutKey.replace(JSON_CONTENT_ID, contentID);
        }

        //Handle node overrides
        TileEntity tile = getTileEntity();
        if (tile instanceof ITileNodeHost)
        {
            ITileNode node = ((ITileNodeHost) tile).getTileNode();
            if (node instanceof IMultiBlockNodeListener && ((IMultiBlockNodeListener) node).getMultiBlockLayoutKey() != null)
            {
                return ((IMultiBlockNodeListener) node).getMultiBlockLayoutKey();
            }
        }

        //Allow listeners to hook
        if (block instanceof BlockBase && world().unwrap().blockExists(xi(), yi(), zi()))
        {
            ListenerIterator it = new ListenerIterator(world().unwrap(), xi(), yi(), zi(), ((BlockBase) block), BlockListenerKeys.MULTI_BLOCK_LAYOUT_LISTENER);
            while (it.hasNext())
            {
                while (it.hasNext())
                {
                    ITileEventListener next = it.next();
                    if (next instanceof IMultiBlockLayoutListener)
                    {
                        String key = ((IMultiBlockLayoutListener) next).getMultiBlockLayoutKey();
                        if (key != null && !key.isEmpty())
                        {
                            return key;
                        }
                    }
                }
            }
        }
        return _layoutKey;
    }

    public static class Builder implements ITileEventListenerBuilder
    {
        @Override
        public ITileEventListener createListener(Block block)
        {
            return new MultiBlockListener(block);
        }

        @Override
        public String getListenerKey()
        {
            return "multiblock";
        }
    }
}
