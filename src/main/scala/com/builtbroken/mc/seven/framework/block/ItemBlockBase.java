package com.builtbroken.mc.seven.framework.block;

import com.builtbroken.mc.client.json.ClientDataHandler;
import com.builtbroken.mc.client.json.IJsonRenderStateProvider;
import com.builtbroken.mc.client.json.imp.IRenderState;
import com.builtbroken.mc.client.json.render.RenderData;
import com.builtbroken.mc.client.json.render.item.RenderStateItem;
import com.builtbroken.mc.prefab.items.ItemBlockAbstract;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.client.IItemRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/20/2017.
 */
public class ItemBlockBase extends ItemBlockAbstract implements IJsonRenderStateProvider
{
    int spriteID = -1;

    public ItemBlockBase(Block block)
    {
        super(block);
    }


    public BlockBase getBlockBase()
    {
        return (BlockBase) field_150939_a;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float xHit, float yHit, float zHit)
    {
        Block block = world.getBlock(x, y, z);

        if (block == Blocks.snow_layer && (world.getBlockMetadata(x, y, z) & 7) < 1)
        {
            side = 1;
        }
        else if (block != Blocks.vine && block != Blocks.tallgrass && block != Blocks.deadbush && !block.isReplaceable(world, x, y, z))
        {
            if (side == 0)
            {
                --y;
            }

            if (side == 1)
            {
                ++y;
            }

            if (side == 2)
            {
                --z;
            }

            if (side == 3)
            {
                ++z;
            }

            if (side == 4)
            {
                --x;
            }

            if (side == 5)
            {
                ++x;
            }
        }

        if (stack.stackSize == 0)
        {
            return false;
        }
        else if (!player.canPlayerEdit(x, y, z, side, stack))
        {
            return false;
        }
        else if (y == 255 && this.field_150939_a.getMaterial().isSolid())
        {
            return false;
        }
        else if (!getBlockBase().canPlaceBlockAt(player, world, x, y, z))
        {
            return false;
        }
        else if (world.canPlaceEntityOnSide(this.field_150939_a, x, y, z, false, side, player, stack))
        {
            int i1 = this.getMetadata(stack.getItemDamage());
            int j1 = this.field_150939_a.onBlockPlaced(world, x, y, z, side, xHit, yHit, zHit, i1);

            if (placeBlockAt(stack, player, world, x, y, z, side, xHit, yHit, zHit, j1))
            {
                world.playSoundEffect((double) ((float) x + 0.5F), (double) ((float) y + 0.5F), (double) ((float) z + 0.5F), this.field_150939_a.stepSound.func_150496_b(), (this.field_150939_a.stepSound.getVolume() + 1.0F) / 2.0F, this.field_150939_a.stepSound.getPitch() * 0.8F);
                --stack.stackSize;
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public List<String> getRenderContentIDs()
    {
        List<String> list = new ArrayList();
        list.add(getRenderContentID(0));
        return list;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getRenderContentID(IItemRenderer.ItemRenderType renderType, Object objectBeingRendered)
    {
        if (objectBeingRendered instanceof ItemStack)
        {
            return getRenderContentID((ItemStack) objectBeingRendered);
        }
        else if (objectBeingRendered instanceof Item)
        {
            return getRenderContentID(new ItemStack((Item) objectBeingRendered));
        }
        else if (objectBeingRendered instanceof Block)
        {
            return getRenderContentID(new ItemStack((Block) objectBeingRendered));
        }
        return getRenderContentID(0);
    }

    public String getRenderContentID(ItemStack stack)
    {
        return getRenderContentID(stack.getItemDamage());
    }

    public String getRenderContentID(int meta)
    {
        return getBlockBase().getContentID(meta);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses()
    {
        return true; //Fix for getting ItemStack calls
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister reg)
    {
        super.registerIcons(reg);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int meta)
    {
        return getIconFromState(ClientDataHandler.INSTANCE.getRenderData(getRenderContentID(meta)), meta, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderPasses(int metadata)
    {
        RenderData data = ClientDataHandler.INSTANCE.getRenderData(getRenderContentID(metadata));
        if (data != null)
        {
            return data.getItemRenderLayers(metadata);
        }

        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber()
    {
        //Check if we should render as an item
        if (spriteID == -1)
        {
            //Loop through all content IDs
            for (String id : getRenderContentIDs())
            {
                RenderData data = ClientDataHandler.INSTANCE.getRenderData(id);
                if (data != null)
                {
                    if (data.getSpriteIndexForItems() != -1)
                    {
                        spriteID = data.getSpriteIndexForItems();
                        return spriteID;
                    }

                    //Guesses at what ID to use based on item renders
                    List<String> keys = new ArrayList();
                    keys.add(RenderData.INVENTORY_RENDER_KEY);
                    keys.add("item");

                    //Loop through keys until we find a valid match
                    for (String key : keys)
                    {
                        IRenderState state = data.getState(key);
                        if (state instanceof RenderStateItem)
                        {
                            spriteID = 1;
                            return spriteID;
                        }
                    }
                }
            }

            //Backup case, if not found default to zero
            spriteID = 0;
        }
        return spriteID;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int meta, int pass)
    {
        return getIconFromState(ClientDataHandler.INSTANCE.getRenderData(getRenderContentID(meta)), meta, pass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int renderPass, EntityPlayer player, ItemStack usingItem, int useRemaining)
    {
        final String contentID = getRenderContentID(stack);
        final RenderData data = ClientDataHandler.INSTANCE.getRenderData(contentID);
        if (data != null)
        {
            final String renderKey = getRenderKey(stack, player, useRemaining);
            if (renderKey != null)
            {
                IRenderState state = data.getState(RenderData.INVENTORY_RENDER_KEY + "." + renderKey);  //TODO add render pass & use remaining
                if (state != null)
                {
                    IIcon icon = state.getIcon(renderPass);
                    if (icon != null)
                    {
                        return icon;
                    }
                }
            }
        }
        return getIcon(stack, renderPass);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ItemStack stack, int pass)
    {
        //Attempt to render using stack -> content ID
        final String contentID = getRenderContentID(stack);
        final RenderData data = ClientDataHandler.INSTANCE.getRenderData(contentID);
        if (data != null)
        {
            //Build key set to attempt to get icon TODO cache to improve performance
            List<String> keys = new ArrayList();
            String recommendedKey = getRenderKey(stack);
            if (recommendedKey != null && !recommendedKey.isEmpty())
            {
                keys.add(RenderData.INVENTORY_RENDER_KEY + "." + recommendedKey + "." + pass);
                keys.add(RenderData.INVENTORY_RENDER_KEY + "." + recommendedKey);
                keys.add(recommendedKey);
            }
            keys.add(RenderData.INVENTORY_RENDER_KEY + "." + pass);
            keys.add(RenderData.INVENTORY_RENDER_KEY);

            //Loop through keys until we find a valid match
            for (String key : keys)
            {
                IRenderState state = data.getState(key);
                if (state != null)
                {
                    if (state != null)
                    {
                        IIcon icon = state.getIcon(pass);
                        if (icon != null)
                        {
                            return icon;
                        }
                    }
                }
            }
        }
        //If all else fails fall back to using metadata
        return getIconFromDamageForRenderPass(stack.getItemDamage(), pass);
    }

    /**
     * Called to get the render key for the stack
     * <p>
     * Keep in mind key is prefixed by render type
     * <p>
     * Ex: item.inventory.key
     * item.entity.key
     *
     * @param stack - stack being rendered
     * @return key for the render ID
     */

    public String getRenderKey(ItemStack stack)
    {
        return null;
    }

    /**
     * Called to ge the render key for the stack
     * <p>
     * Keep in mind key is prefixed by render type
     * <p>
     * Ex: item.inventory.key
     * item.entity.key
     *
     * @param stack        - stack being rendered
     * @param entity       - entity holding the item
     * @param useRemaining - how many uses are left
     * @return key for the render ID
     */
    public String getRenderKey(ItemStack stack, Entity entity, int useRemaining)
    {
        return null;
    }

    /**
     * Called to get the icon from the state
     *
     * @param data - render data
     * @param meta - metadata or damage of the item
     * @param pass - render pass, 0 by default
     * @return icon, can not be null or will crash
     */
    protected IIcon getIconFromState(RenderData data, int meta, int pass)
    {
        if (data != null)
        {
            //TODO cache list for faster runtime
            List<String> keys = getIconStateKeys(data, meta, pass);

            //Attempt to get meta
            for (String key : keys)
            {
                IRenderState state = data.getState(key);
                if (state != null)
                {
                    IIcon icon = state.getIcon(pass);
                    if (icon != null)
                    {
                        return icon;
                    }
                }
            }
        }
        return getFallBackIcon();
    }

    protected List<String> getIconStateKeys(RenderData data, int meta, int pass)
    {
        List<String> keys = new ArrayList();
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + meta + "." + pass);
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + meta);
        keys.add(RenderData.INVENTORY_RENDER_KEY + "." + pass);
        keys.add(RenderData.INVENTORY_RENDER_KEY);
        keys.add("item");

        return keys;
    }

    /**
     * Called to get a fallback icon to display
     * when an icon can not be retrieved from
     * the json render system
     *
     * @return icon, can not be null
     */
    protected IIcon getFallBackIcon()
    {
        return itemIcon != null ? itemIcon : getBlockBase().getIcon(0, 0);
    }
}
