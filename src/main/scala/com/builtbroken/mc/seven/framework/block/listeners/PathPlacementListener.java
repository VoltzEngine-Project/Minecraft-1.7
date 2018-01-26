package com.builtbroken.mc.seven.framework.block.listeners;

import com.builtbroken.jlib.data.vector.IPos3D;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.data.Direction;
import com.builtbroken.mc.framework.block.imp.ITileEventListener;
import com.builtbroken.mc.framework.block.imp.ITileEventListenerBuilder;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.imp.transform.vector.BlockPos;
import com.builtbroken.mc.lib.data.BlockStateEntry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;

import java.util.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/21/2017.
 */
public class PathPlacementListener extends AdjacentPlacementListener
{
    @JsonProcessorData(value = "pathRange", type = "int")
    protected int pathRange = 5;

    protected List<BlockStateEntry> pathBlocks = new ArrayList();
    protected List<String> pathContentIDs = new ArrayList();


    public PathPlacementListener(Block block)
    {
        super(block);
    }

    @Override
    protected boolean isPlacementValid()
    {
        if(isServer())
        {
            //Contains block moved over
            final HashSet<BlockPos> pathedPositions = new HashSet();
            //Contains blocks to path next
            final Queue<BlockPos> pathNextList = new LinkedList();

            //Center of the path
            final BlockPos center = new BlockPos(this);

            //Attempt to path center first
            if (canPath(center))
            {
                pathNextList.add(center);
            }
            //Fix for placement code where center is not the block we are placing
            else
            {
                for (Direction direction : supportedDirections == null ? Direction.DIRECTIONS : supportedDirections)
                {
                    pathNextList.add(center.add(direction));
                }
            }

            //Loop all tiles
            while (!pathNextList.isEmpty())
            {
                //Get next tile and add to pathed list
                final BlockPos nextPos = pathNextList.poll();
                pathedPositions.add(nextPos);

                //Loop connections
                for (Direction direction : supportedDirections == null ? Direction.DIRECTIONS : supportedDirections)
                {
                    final BlockPos pos = nextPos.add(direction);

                    if (!pathedPositions.contains(pos) && !pathNextList.contains(pos))
                    {
                        //Only do check once per tile
                        if (canPath(pos))
                        {
                            //Check if valid, exit condition for loop
                            if (isSupportingTile(getBlockAccess(), pos))
                            {
                                return true;
                            }

                            //Add to path next list
                            pathNextList.add(pos);
                        }
                        else
                        {
                            pathedPositions.add(pos);
                        }
                    }
                }
            }
            return false;
        }
        return true;
    }

    protected boolean canPath(IPos3D pos)
    {
        return isInDistance(pos) && !getBlockAccess().isAirBlock(pos.xi(), pos.yi(), pos.zi()) && isPathTile(getBlockAccess(), pos);
    }

    /**
     * Checks if tile should be pathed
     *
     * @param access
     * @param pos
     * @return
     */
    protected boolean isPathTile(IBlockAccess access, IPos3D pos)
    {
        return pathBlocks.isEmpty() && pathContentIDs.isEmpty() || doesContainTile(access, pos, pathBlocks, pathContentIDs);
    }

    protected boolean isInDistance(IPos3D pos)
    {
        if (pos.xi() > pathRange + xi() || pos.xi() < xi() - pathRange)
        {
            return false;
        }
        if (pos.yi() > pathRange + yi() || pos.yi() < yi() - pathRange)
        {
            return false;
        }
        if (pos.zi() > pathRange + zi() || pos.zi() < zi() - pathRange)
        {
            return false;
        }
        return true;
    }

    @JsonProcessorData("canPath")
    public void processPathBlocks(JsonElement inputElement)
    {
        if (inputElement.isJsonArray())
        {
            //Loop through elements in array
            for (JsonElement element : inputElement.getAsJsonArray())
            {
                //Get as object
                if (element.isJsonObject())
                {
                    JsonObject object = element.getAsJsonObject();

                    if (object.has("block"))
                    {
                        String blockName = object.getAsJsonPrimitive("block").getAsString();
                        int meta = -1;
                        if (object.has("data"))
                        {
                            meta = object.getAsJsonPrimitive("data").getAsInt();
                        }

                        pathBlocks.add(new BlockStateEntry(blockName, meta));
                    }
                    else if (object.has("contentID"))
                    {
                        pathContentIDs.add(object.getAsJsonPrimitive("contentID").getAsString().toLowerCase());
                    }
                    else
                    {
                        Engine.logger().warn("AdjacentPlacementListener#process(JsonElement) >> Could not find convert '" + element + "' int a usable type for " + this);
                    }
                }
                else
                {
                    throw new IllegalArgumentException("Invalid data, block entries must look like \n {\n\t \"block\" : \"minecraft:tnt\",\n\t \"data\" : 0 \n}");
                }
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid data, blocks data must be an array");
        }
    }

    @Override
    public String toString()
    {
        if (contentUseID != null)
        {
            return "PathFinderPlacementListener[" + block + " >> " + contentUseID + "]@" + hashCode();
        }
        else if (metaCheck != -1)
        {
            return "PathFinderPlacementListener[" + block + "@" + metaCheck + "]@" + hashCode();
        }
        return "PathFinderPlacementListener[" + block + "]@" + hashCode();
    }

    public static class Builder implements ITileEventListenerBuilder
    {
        @Override
        public ITileEventListener createListener(Block block)
        {
            return new PathPlacementListener(block);
        }

        @Override
        public String getListenerKey()
        {
            return "pathFinderPlacementListener";
        }
    }
}
