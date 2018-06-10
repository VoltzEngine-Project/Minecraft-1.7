package com.builtbroken.mc.seven.framework.block;

import com.builtbroken.jlib.data.Colors;
import com.builtbroken.mc.api.data.ActionResponse;
import com.builtbroken.mc.api.tile.access.IGuiTile;
import com.builtbroken.mc.api.tile.node.ITileNodeHost;
import com.builtbroken.mc.client.json.ClientDataHandler;
import com.builtbroken.mc.client.json.imp.IRenderState;
import com.builtbroken.mc.client.json.render.RenderData;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.registry.ModManager;
import com.builtbroken.mc.framework.block.imp.*;
import com.builtbroken.mc.framework.json.IJsonGenMod;
import com.builtbroken.mc.framework.json.imp.IJsonGenObject;
import com.builtbroken.mc.framework.json.imp.JsonLoadPhase;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import com.builtbroken.mc.lib.helper.WrenchUtility;
import com.builtbroken.mc.prefab.inventory.InventoryUtility;
import com.builtbroken.mc.seven.abstraction.MinecraftWrapper;
import com.builtbroken.mc.seven.framework.block.listeners.ListenerIterator;
import com.builtbroken.mc.seven.framework.block.listeners.client.IIconListener;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

/**
 * Block generated through a json based file format... Used to reduce dependency on code
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 6/24/2016.
 */
public class BlockBase extends BlockContainer implements IJsonGenObject, ITileEntityProvider
{
    /** Data about the block */
    public final BlockPropertyData data;
    /** Mod that claimed this block */
    public IJsonGenMod mod;

    /** Has the block been registered */
    protected boolean registered = false;

    public String unlocalizedBlockName;

    //Listeners
    public final HashMap<String, List<ITileEventListener>> listeners = new HashMap();

    public BlockBase(BlockPropertyData data)
    {
        super(data.getMaterial());
        this.data = data;
        this.data.block = this;
        unlocalizedBlockName = data.localization.replace("${name}", data.name).replace("${mod}", data.getMod());
        this.setBlockName(unlocalizedBlockName);
        this.setResistance(data.getResistance());
        this.setHardness(data.getHardness());

        setBlockBounds(data.getBlockBounds().min().xf(), data.getBlockBounds().min().yf(), data.getBlockBounds().min().zf(), data.getBlockBounds().max().xf(), data.getBlockBounds().max().yf(), data.getBlockBounds().max().zf());

        //Run later, as the default is set without data working
        this.opaque = this.isOpaqueCube();
        this.lightOpacity = this.isOpaqueCube() ? 255 : 0;
    }

    @Override
    public String getLoader()
    {
        return "block";
    }

    @Override
    public String getMod()
    {
        return data != null ? data.getMod() : null;
    }

    @Override
    public String getContentID()
    {
        return data.registryKey;
    }

    @Override
    public void register(IJsonGenMod mod, ModManager manager)
    {
        if (!registered)
        {
            this.mod = mod;
            registered = true;
            manager.newBlock(data.registryKey, this, getItemBlockClass());
            if (data.tileEntityProvider != null)
            {
                data.tileEntityProvider.register(this, mod, manager);
            }
        }
    }

    /**
     * Gets the item block class to use with this block,
     * only used during registration.
     *
     * @return
     */
    protected Class<? extends ItemBlock> getItemBlockClass()
    {
        return ItemBlockBase.class;
    }

    @Override
    public void onPhase(JsonLoadPhase phase)
    {
        if (phase == JsonLoadPhase.LOAD_PHASE_TWO)
        {
            if (data.oreName != null)
            {
                OreDictionary.registerOre(data.oreName, new ItemStack(this));
            }
        }
    }

    @Override
    public String toString()
    {
        return "Block[" + data.name + "]";
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta)
    {
        if (data.tileEntityProvider != null)
        {
            return data.tileEntityProvider.createNewTileEntity(this, world, meta);
        }
        return null;
    }

    @Override
    public TileEntity createTileEntity(World world, int meta)
    {
        //TODO add creation listener to inject listeners for tiles
        return createNewTileEntity(world, meta);
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "break");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IDestroyedListener && ((IDestroyedListener) next).canSilkHarvest(player, metadata))
            {
                return true;
            }
        }
        return super.canSilkHarvest(world, player, x, y, z, metadata);
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, int x, int y, int z, int meta)
    {
        //Remove block is called before this TODO fix so tiles can use this event

        player.addStat(StatList.mineBlockStatArray[getIdFromBlock(this)], 1);
        player.addExhaustion(0.025F);

        if (this.canSilkHarvest(world, player, x, y, z, meta) && EnchantmentHelper.getSilkTouchModifier(player))
        {
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();

            boolean ignoreNormalDrop = false;

            //Allow listeners to override default drops
            ListenerIterator it = new ListenerIterator(world, x, y, z, this, "blockStack");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof IBlockStackListener)
                {
                    if (((IBlockStackListener) next).collectSilkHarvestDrops(items, player, meta))
                    {
                        ignoreNormalDrop = true;
                    }
                }
            }

            //Get normal drop if there were no overrides
            if (!ignoreNormalDrop || items.isEmpty())
            {
                ItemStack itemstack = this.createStackedBlock(meta);
                if (itemstack != null)
                {
                    items.add(itemstack);
                }
            }

            //Fire event to allow hooking drops
            ForgeEventFactory.fireBlockHarvesting(items, world, this, x, y, z, meta, 0, 1.0f, true, player);

            //Drop items
            for (ItemStack is : items)
            {
                this.dropBlockAsItem(world, x, y, z, is);
            }
        }
        else
        {
            harvesters.set(player);
            int i1 = EnchantmentHelper.getFortuneModifier(player);
            this.dropBlockAsItem(world, x, y, z, meta, i1);
            harvesters.set(null);
        }
    }

    @Override
    public float getBlockHardness(World world, int x, int y, int z)
    {
        float hardness = data.getHardness();
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "hardness");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IHardnessListener)
            {
                float h = ((IHardnessListener) next).getBlockHardness();
                if (h > hardness)
                {
                    //Highest hardness wins
                    hardness = h;
                }
            }
        }
        return hardness;
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, int x, int y, int z)
    {
        final int metadata = world.getBlockMetadata(x, y, z);
        float hardness = 0;

        //Get player relative hardness
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "hardness");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IHardnessListener)
            {
                float h = ((IHardnessListener) next).getBlockHardness(player);
                if (h > hardness)
                {
                    //Highest hardness wins
                    hardness = h;
                }
            }
        }

        //Only use default hardness if no listener provided hardness
        if (hardness == 0)
        {
            hardness = data.getHardness();
        }

        boolean canHarvest = canHarvestBlock(player, world, x, y, z, metadata);

        //Get break speed
        float breakSpeed;
        if (!canHarvest) //TODO build calculator into debug GUI to see [break speed vs hardness vs canBreak]
        {
            breakSpeed = player.getBreakSpeed(this, true, metadata, x, y, z) / hardness / 100F;
        }
        else
        {
            breakSpeed = player.getBreakSpeed(this, false, metadata, x, y, z) / hardness / 30F;
        }

        return breakSpeed;
    }

    /**
     * Location based version of {@link #canHarvestBlock(EntityPlayer, int)}
     *
     * @param player
     * @param world
     * @param x
     * @param y
     * @param z
     * @param meta
     * @return
     */
    public boolean canHarvestBlock(EntityPlayer player, World world, int x, int y, int z, int meta)
    {
        //Material doesn't care, we don't care
        if (getMaterial().isToolNotRequired())
        {
            return true;
        }

        final ItemStack stack = player.inventory.getCurrentItem();

        //pull harvest data from block
        final String desiredHarvestTool = getHarvestTool(world, x, y, z, meta);
        final int desiredHarvestLevel = getHarvestLevel(world, x, y, z, meta);

        //If no item or tool, pop harvest even
        if (stack == null || desiredHarvestTool == null)
        {
            return player.canHarvestBlock(this);
        }

        //Get harvest level from tool
        final int toolLevel = stack.getItem().getHarvestLevel(stack, desiredHarvestTool);

        //If less than zero, then pop harvest event
        if (toolLevel < 0)
        {
            return player.canHarvestBlock(this);
        }

        //Check tool level against harvest level
        if (toolLevel >= desiredHarvestLevel)
        {
            //Check listeners
            ListenerIterator it = new ListenerIterator(world, x, y, z, this, "break");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof IDestroyedListener && !((IDestroyedListener) next).canHarvest(player, meta))
                {
                    return false;
                }
            }

            //No fails, then we can harvest
            return true;
        }
        return false;
    }

    public String getHarvestTool(World world, int x, int y, int z, int meta)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "tool");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IToolListener)
            {
                String tool = ((IToolListener) next).getBlockHarvestTool(meta);
                if (tool == null || !tool.isEmpty())
                {
                    return tool;
                }
            }
        }
        return getHarvestTool(meta);
    }

    @Override
    public String getHarvestTool(int metadata)
    {
        List<ITileEventListener> toolListeners = listeners.get("tool");
        if (toolListeners != null)
        {
            for (ITileEventListener next : toolListeners)
            {
                if (next instanceof IToolListener)
                {
                    String tool = ((IToolListener) next).getHarvestTool(metadata);
                    if (tool == null || !tool.isEmpty())
                    {
                        return tool;
                    }
                }
            }
        }
        String tool = getBlockHarvestTool(metadata);
        if (tool != null && !tool.isEmpty())
        {
            return tool;
        }
        return super.getHarvestTool(metadata);
    }

    public int getHarvestLevel(World world, int x, int y, int z, int meta)
    {
        int level = getHarvestLevel(meta);
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "tool");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IToolListener)
            {
                int l = ((IToolListener) next).getHarvestLevel(meta);
                if (l > level)
                {
                    level = l;
                }
            }
        }
        return level;
    }

    @Override
    public int getHarvestLevel(int metadata)
    {
        List<ITileEventListener> toolListeners = listeners.get("tool");
        //Get default harvest level
        int level = super.getHarvestLevel(metadata);

        //Get assigned harvest level
        if (level < getBlockHarvestLevel(metadata))
        {
            level = getBlockHarvestLevel(metadata);
        }

        if (toolListeners != null)
        {
            for (ITileEventListener next : toolListeners)
            {
                if (next instanceof IToolListener)
                {
                    int l = ((IToolListener) next).getBlockHarvestLevel(metadata);
                    if (l > level)
                    {
                        level = l;
                    }
                }
            }
        }
        return level;
    }

    protected int getBlockHarvestLevel(int metadata)
    {
        return data.getHarvestLevel();
    }

    protected String getBlockHarvestTool(int metadata)
    {
        return data.getHarvestTool();
    }

    @Override
    public void fillWithRain(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "rain");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IFillRainListener)
            {
                ((IFillRainListener) next).onFilledWithRain();
            }
        }
    }

    @Override
    public float getExplosionResistance(Entity entity)
    {
        if (listeners.containsKey("resistance"))
        {
            float re = -1;
            for (ITileEventListener listener : listeners.get("resistance"))
            {
                if (listener instanceof IResistanceListener)
                {
                    float value = ((IResistanceListener) listener).getExplosionResistance(entity);
                    if (value >= 0 && (value < re || re < 0))
                    {
                        re = value;
                    }
                }
            }
            if (re >= 0)
            {
                return re;
            }
        }
        return data.getResistance() / 5.0F;
    }

    @Override
    public float getExplosionResistance(Entity entity, World world, int x, int y, int z, double explosionX, double explosionY, double explosionZ)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "resistance");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IResistanceListener)
            {
                ((IResistanceListener) next).getExplosionResistance(entity, explosionX, explosionY, explosionZ);
            }
        }
        return getExplosionResistance(entity);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                ((IPlacementListener) next).onAdded();
            }
        }
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entityLiving, ItemStack itemStack)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                ((IPlacementListener) next).onPlacedBy(entityLiving, itemStack);
            }
        }
    }

    @Override
    public void onPostBlockPlaced(World world, int x, int y, int z, int metadata)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                ((IPlacementListener) next).onPostPlaced(metadata);
            }
        }
    }

    /**
     * Called upon the block being destroyed by an explosion
     */
    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion ex)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "break");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockListener)
            {
                ((IDestroyedListener) next).onDestroyedByExplosion(ex);
            }
        }
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int par6)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "break");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockListener)
            {
                ((IDestroyedListener) next).breakBlock(block, par6);
            }
        }
        super.breakBlock(world, x, y, z, block, par6);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "break");
        boolean removed = false;
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockListener)
            {
                if (((IDestroyedListener) next).removedByPlayer(player, willHarvest))
                {
                    removed = true;
                }
            }
        }
        return super.removedByPlayer(world, player, x, y, z, willHarvest) || removed;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block)
    {
        if (!world.isRemote && world.blockExists(x, y, z)) //Fix inf loop caused by unloaded chunks
        {
            ListenerIterator it = new ListenerIterator(world, x, y, z, this, "change");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof IChangeListener)
                {
                    ((IChangeListener) next).onNeighborBlockChange(block);
                }
            }
        }
    }

    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tileX, int tileY, int tileZ)
    {
        if (world instanceof World && !((World) world).isRemote && ((World) world).blockExists(x, y, z))
        {
            ListenerIterator it = new ListenerIterator(world, x, y, z, this, "change");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof IChangeListener)
                {
                    ((IChangeListener) next).onNeighborChange(tileX, tileY, tileZ);
                }
            }
        }
    }

    @Override
    public boolean getWeakChanges(IBlockAccess world, int x, int y, int z)
    {
        return super.getWeakChanges(world, x, y, z);
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, int x, int y, int z, int side)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                if (((IPlacementListener) next).canPlaceOnSide(side) == ActionResponse.CANCEL)
                {
                    return false;
                }
            }
        }
        return canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                if (((IPlacementListener) next).canPlaceAt() == ActionResponse.CANCEL)
                {
                    return false;
                }
            }
        }
        return super.canPlaceBlockAt(world, x, y, z);
    }

    public boolean canPlaceBlockAt(Entity entity, ItemStack stack, World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                if (((IPlacementListener) next).canPlaceAt(MinecraftWrapper.INSTANCE.get(entity), stack) == ActionResponse.CANCEL)
                {
                    return false;
                }
            }
        }
        return canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public boolean canReplace(World world, int x, int y, int z, int side, ItemStack stack)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                if (((IPlacementListener) next).canReplace(world, x, y, z, side, stack) == ActionResponse.CANCEL)
                {
                    return false;
                }
            }
        }
        return super.canReplace(world, x, y, z, side, stack);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "placement");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IPlacementListener)
            {
                if (((IPlacementListener) next).canBlockStay() == ActionResponse.CANCEL)
                {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "activation");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IActivationListener)
            {
                ((IActivationListener) next).onPlayerClicked(player);
            }
        }
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ)
    {
        try
        {
            final ItemStack heldItem = player.getHeldItem();
            boolean activated = false;

            Object tile = getTile(world, x, y, z);
            if (WrenchUtility.isUsableWrench(player, heldItem, x, y, z))
            {
                ListenerIterator it = new ListenerIterator(world, x, y, z, this, "wrench");
                while (it.hasNext())
                {
                    ITileEventListener next = it.next();
                    if (next instanceof IWrenchListener && ((IWrenchListener) next).handlesWrenchRightClick() && ((IWrenchListener) next).onPlayerRightClickWrench(player, side, hitX, hitY, hitZ))
                    {
                        activated = true;
                    }
                }
                if (activated)
                {
                    WrenchUtility.damageWrench(player, heldItem, x, y, z);
                }
                if (activated)
                {
                    return true;
                }
            }
            //We do not want to open GUIs with the wrench
            else if (heldItem != null && heldItem.getItem() == Engine.itemWrench)
            {
                return false;
            }

            //TODO move to listener to prevent usage of IGuiTile in special cases
            if (tile instanceof IGuiTile && ((IGuiTile) tile).shouldOpenOnRightClick(player))
            {
                int id = ((IGuiTile) tile).getDefaultGuiID(player);
                if (id >= 0)
                {
                    Object o = ((IGuiTile) tile).getServerGuiElement(id, player);
                    if (o != null)
                    {
                        //open GUI only on server, but still mark as activated to consume action correctly
                        if (!world.isRemote)
                        {
                            player.openGui(mod, id, world, x, y, z);
                        }
                        activated = true;
                    }
                }
            }

            ListenerIterator it = new ListenerIterator(world, x, y, z, this, "activation");
            while (it.hasNext())
            {
                ITileEventListener next = it.next();
                if (next instanceof IActivationListener)
                {
                    if (((IActivationListener) next).onPlayerActivated(player, side, hitX, hitY, hitZ))
                    {
                        activated = true;
                    }
                }
            }
            return activated;
        }
        catch (Exception e)
        {
            outputError(world, x, y, z, "while right click block on side " + side, e);
            player.addChatComponentMessage(new ChatComponentText(Colors.RED.code + LanguageUtility.getLocal("blockTile.error.onBlockActivated")));
        }
        return false;
    }

    protected Object getTile(World world, int x, int y, int z)
    {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof ITileNodeHost)
        {
            return ((ITileNodeHost) tile).getTileNode();
        }
        return tile;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random par5Random)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "update");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IUpdateListener)
            {
                ((IUpdateListener) next).updateTick(par5Random);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random par5Random)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "update");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IUpdateListener)
            {
                ((IUpdateListener) next).randomDisplayTick(par5Random);
            }
        }
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "update");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof ICollisionListener)
            {
                ((ICollisionListener) next).onEntityCollidedWithBlock(entity);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB aabb, List list, Entity entity)
    {
        super.addCollisionBoxesToList(world, x, y, z, aabb, list, entity);

        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "bounds");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBoundListener)
            {
                List collect = new ArrayList();
                ((IBoundListener) next).addCollisionBoxesToList(aabb, collect, entity);
                for (Object object : collect)
                {
                    if (object instanceof AxisAlignedBB)
                    {
                        boolean interest = aabb.intersectsWith((AxisAlignedBB) object);
                        if (interest)
                        {
                            list.add(object);
                        }
                    }
                }
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "bounds");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBoundListener)
            {
                AxisAlignedBB bound = ((IBoundListener) next).getSelectedBounds();
                if (bound != null)
                {
                    return bound; //TODO change to largest box wins
                }
            }
        }
        return data.getSelectionBounds().clone().add(x, y, z).toAABB();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "bounds");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBoundListener)
            {
                AxisAlignedBB bound = ((IBoundListener) next).getCollisionBounds();
                if (bound != null)
                {
                    return bound; //TODO change to largest box wins
                }
            }
        }
        return data.getBlockBounds().clone().add(x, y, z).toAABB();
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement listeners
        return super.shouldSideBeRendered(access, x, y, z, side);
    }

    @Override
    public boolean isBlockSolid(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement listeners
        return data.isSolid() || super.isBlockSolid(access, x, y, z, side);
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side)
    {
        //TODO implement listeners
        return data.isSolid() || isNormalCube(world, x, y, z);
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int x, int y, int z)
    {
        //TODO implement listeners
        return isNormalCube();
    }

    @Override
    public boolean isNormalCube()
    {
        return data.isNormalCube() || getMaterial().isOpaque() && renderAsNormalBlock() && !canProvidePower();
    }

    @Override
    public int getLightValue(IBlockAccess access, int x, int y, int z)
    {
        int lightValue = 0;
        ListenerIterator it = new ListenerIterator(access, x, y, z, this, "light");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof ILightLevelListener)
            {
                int level = Math.min(15, ((ILightLevelListener) next).getLightLevel());
                if (level >= 0 && level > lightValue)
                {
                    lightValue = level;
                }
            }
        }
        if (lightValue > 0)
        {
            return lightValue;
        }
        if (data != null && data.getLightValue() > 0)
        {
            return data.getLightValue();
        }
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride()
    {
        return data != null ? data.hasComparatorInputOverride() : false;
    }

    @Override
    public boolean isOpaqueCube()
    {
        return data != null ? data.isOpaqueCube() : false;
    }

    @Override
    public boolean renderAsNormalBlock()
    {
        return data != null ? data.renderAsNormalBlock() : true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderType()
    {
        return data != null ? data.getRenderType() : 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
    {
        int meta = world.getBlockMetadata(x, y, z);
        if (Engine.runningAsDev && (meta < 0 || meta > 15))
        {
            Engine.logger().error(String.format("BlockBase#getIcon(%s, %s, %s, %s, %s) -> meta returned from world was invalid, meta: %s, block: %s",
                    world, x, y, z, side, meta, data.getMod() + ":" + data.registryKey), new RuntimeException("stack"));
            meta = 0;
        }

        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "icon");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IIconListener)
            {
                IIcon icon = ((IIconListener) next).getTileIcon(side, meta);
                if (icon != null)
                {
                    return icon;
                }
            }
        }
        return getIcon(side, meta);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(int side, int meta)
    {
        //Handle icon listeners
        if (listeners.containsKey("icon"))
        {
            for (ITileEventListener listener : listeners.get("icon"))
            {
                if (listener instanceof IIconListener && !(listener instanceof IBlockListener))
                {
                    IIcon icon = ((IIconListener) listener).getTileIcon(side, meta);
                    if (icon != null)
                    {
                        return icon;
                    }
                }
            }
        }
        return getIconFromJson(side, meta);
    }

    public IIcon getIconFromJson(int side, int meta)
    {
        //handle json data
        final RenderData data = getRenderData(meta);
        if (data != null)
        {
            final Stack<String> stack = new Stack();
            getRenderStates(stack, side, meta);
            while (!stack.isEmpty())
            {
                final String key = stack.pop();
                if (key != null)
                {
                    final IRenderState state = data.getState(key);
                    if (state != null)
                    {
                        final IIcon icon = state.getIcon(side);
                        if (icon != null)
                        {
                            return icon;
                        }
                    }
                }
            }
        }
        return Blocks.wool.getIcon(0, side);
    }

    protected void getRenderStates(Stack<String> stack, int side, int meta)
    {
        stack.push("tile");
        stack.push("block");
        stack.push("tile." + meta);
        stack.push("tile." + ForgeDirection.getOrientation(meta).name().toLowerCase());
        stack.push("block." + meta);
        stack.push("block." + ForgeDirection.getOrientation(meta).name().toLowerCase());
    }

    public RenderData getRenderData(int meta)
    {
        return ClientDataHandler.INSTANCE.getRenderData(getContentID(meta));
    }

    /**
     * @param meta - index of the item, -1 should be the master content ID
     * @return
     */
    public String getContentID(int meta)
    {
        if (data == null)
        {
            return getClass().getName();
        }
        return data.getMod() + ":" + data.registryKey;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void registerBlockIcons(IIconRegister iconRegister)
    {
        //Texture registration is handled by ClientDataHandler
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockAccess access, int x, int y, int z)
    {
        //TODO implement
        return super.colorMultiplier(access, x, y, z);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getBlockColor()
    {
        if (data != null && data.getColor() >= 0)
        {
            return data.getColor();
        }
        return super.getBlockColor();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getRenderColor(int meta)
    {
        //TODO implement metadata values
        if (data != null && data.getColor() >= 0)
        {
            return data.getColor();
        }
        return getBlockColor();
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "blockStack");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockStackListener)
            {
                ItemStack stack = ((IBlockStackListener) next).getPickBlock(target, player);
                if (stack != null && stack.getItem() != null)
                {
                    return stack;
                }
            }
        }
        return super.getPickBlock(target, world, x, y, z, player);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune)
    {
        ArrayList<ItemStack> items = super.getDrops(world, x, y, z, metadata, fortune);

        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "blockStack");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockStackListener)
            {
                ((IBlockStackListener) next).collectDrops(items, metadata, fortune);
            }
        }
        return items;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune)
    {
        if (data != null && data.getItemToDropString() != null)
        {
            //Override to remove default item drop
            if ("nil".equalsIgnoreCase(data.getItemToDropString()))
            {
                return null;
            }

            //Get drop
            Item item = data.getItemToDrop();
            if (item != null)
            {
                return item;
            }
        }
        //Normal drop if nothing is set
        return Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random)
    {
        if (data != null)
        {
            return data.getItemDropCount();
        }
        return 1;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs creativeTabs, List list)
    {
        super.getSubBlocks(item, creativeTabs, list);
        if (listeners.containsKey("blockStack"))
        {
            for (ITileEventListener listener : listeners.get("blockStack"))
            {
                if (listener instanceof IBlockStackListener)
                {
                    ((IBlockStackListener) listener).getSubBlocks(item, creativeTabs, list);
                }
            }
        }
    }

    /**
     * Redstone interaction
     */
    @Override
    public boolean canProvidePower()
    {
        return data != null && data.isSupportsRedstone();
    }

    @Override
    public int isProvidingWeakPower(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return 0;
    }

    @Override
    public int isProvidingStrongPower(IBlockAccess access, int x, int y, int z, int side)
    {
        //TODO implement
        return 0;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess access, int x, int y, int z)
    {
        ListenerIterator it = new ListenerIterator(access, x, y, z, this, "bounds");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBoundListener)
            {
                ((IBoundListener) next).setBlockBoundsBasedOnState();
            }
        }
    }

    @Override
    public void setBlockBoundsForItemRender()
    {
        if (listeners.containsKey("bounds"))
        {
            for (ITileEventListener listener : listeners.get("bounds"))
            {
                if (listener instanceof IBoundListener)
                {
                    ((IBoundListener) listener).setBlockBoundsForItemRender();
                }
            }
        }
    }

    @Override
    protected void dropBlockAsItem(World world, int x, int y, int z, ItemStack itemStack)
    {
        ListenerIterator it = new ListenerIterator(world, x, y, z, this, "blockStack");
        while (it.hasNext())
        {
            ITileEventListener next = it.next();
            if (next instanceof IBlockStackListener)
            {
                ((IBlockStackListener) next).dropBlockAsItem(itemStack);
            }
        }
        if (itemStack != null && itemStack.getItem() != null)
        {
            InventoryUtility.dropItemStack(world, x + 0.5, y + 0.5, z + 0.5, itemStack, 0, 0);
        }
    }

    @Override
    public int getRenderBlockPass()
    {
        return data != null && data.isAlpha() ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass)
    {
        return data != null && data.useAllRenderPasses() || pass == getRenderBlockPass();
    }

    @Override
    public int tickRate(World world)
    {
        int tickRate = super.tickRate(world);
        for (ITileEventListener next : listeners.get("update"))
        {
            if (next instanceof IUpdateListener)
            {
                int r = ((IUpdateListener) next).tickRate(world);
                if (r > 0 && r < tickRate)
                {
                    tickRate = r;
                }
            }
        }
        return tickRate;

    }

    /**
     * Outputs an error to console with location data
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param msg
     * @param e
     */
    protected void outputError(World world, int x, int y, int z, String msg, Throwable e)
    {
        String dim = "null";
        if (world != null && world.provider != null)
        {
            dim = "" + world.provider.dimensionId;
        }
        Engine.logger().error("Error: " + msg + " \nLocation[" + dim + "w " + x + "x " + y + "y " + z + "z" + "]", e);
    }

    /**
     * Called to add a listener to the block
     *
     * @param listener
     */
    public void addListener(ITileEventListener listener)
    {
        if (listener != null)
        {
            List<String> keys = listener.getListenerKeys();
            if (keys != null && !keys.isEmpty())
            {
                for (String key : keys)
                {
                    if (key != null)
                    {
                        List<ITileEventListener> listeners = this.listeners.get(key);
                        if (listeners == null)
                        {
                            listeners = new ArrayList();
                        }
                        listeners.add(listener);
                        this.listeners.put(key, listeners);
                    }
                }
            }
        }
    }
}
