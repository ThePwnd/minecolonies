package com.minecolonies.coremod.util;

import com.minecolonies.api.colony.IChunkmanagerCapability;
import com.minecolonies.api.colony.IColonyTagCapability;
import com.minecolonies.api.configuration.Configurations;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.ChunkLoadStorage;
import com.minecolonies.api.util.Log;
import com.minecolonies.coremod.MineColonies;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyManager;
import com.minecolonies.coremod.colony.IColonyManagerCapability;
import com.minecolonies.coremod.network.messages.UpdateChunkCapabilityMessage;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

import static com.minecolonies.api.util.constant.ColonyManagerConstants.CHUNK_INFO_PATH;
import static com.minecolonies.api.util.constant.ColonyManagerConstants.DISTANCE_TO_LOAD_IMMEDIATELY;
import static com.minecolonies.api.util.constant.ColonyManagerConstants.UNABLE_TO_FIND_WORLD_CAP_TEXT;
import static com.minecolonies.api.util.constant.Constants.BLOCKS_PER_CHUNK;
import static com.minecolonies.coremod.MineColonies.CHUNK_STORAGE_UPDATE_CAP;
import static com.minecolonies.coremod.MineColonies.CLOSE_COLONY_CAP;
import static com.minecolonies.coremod.MineColonies.COLONY_MANAGER_CAP;

/**
 * Class to take care of chunk data helper.
 */
public final class ChunkDataHelper
{
    /**
     * If colony is farther away from a capability then this times the default colony distance it will delete the capability.
     */
    private static final int DISTANCE_TO_DELETE = Configurations.gameplay.workingRangeTownHallChunks * BLOCKS_PER_CHUNK * 2 * 5;

    /**
     * Private constructor to hide implicit one.
     */
    private ChunkDataHelper()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Load the colony info for a certain chunk.
     *
     * @param chunk the chunk.
     * @param world the worldg to.
     */
    public static void loadChunk(final Chunk chunk, final World world)
    {
        final IColonyManagerCapability cap = world.getCapability(COLONY_MANAGER_CAP, null);
        if (cap == null)
        {
            return;
        }
        if (cap.getMissingChunksToLoad() > 0)
        {
            final IChunkmanagerCapability chunkManager = world.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
            if (chunkManager == null)
            {
                Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT);
                return;
            }

            final ChunkLoadStorage existingStorage = chunkManager.getChunkStorage(chunk.x, chunk.z);
            if (existingStorage != null)
            {
                addStorageToChunk(chunk, existingStorage);
                cap.setMissingChunksToLoad(cap.getMissingChunksToLoad() - 1);
            }
            else
            {
                if (Configurations.gameplay.fixOrphanedChunks)
                {
                    final IColonyTagCapability closeCap = chunk.getCapability(CLOSE_COLONY_CAP, null);
                    if (closeCap != null)
                    {
                        boolean dirty = false;
                        for (final int colony : closeCap.getAllCloseColonies())
                        {
                            if (colony != 0 && (cap.getColony(colony) == null
                                                  ||  BlockPosUtil.getDistance2D(cap.getColony(colony).getCenter(), new BlockPos(chunk.x * BLOCKS_PER_CHUNK, 0, chunk.z * BLOCKS_PER_CHUNK)) > DISTANCE_TO_DELETE))
                            {
                                Log.getLogger().warn("Removing orphaned chunk at:  " + chunk.x * BLOCKS_PER_CHUNK + " 100 " + chunk.z * BLOCKS_PER_CHUNK);
                                closeCap.removeColony(colony);
                                dirty = true;
                            }
                        }
                        if (dirty)
                        {
                            chunk.markDirty();
                            MineColonies.getNetwork().sendToAll(new UpdateChunkCapabilityMessage(closeCap, chunk.x, chunk.z));
                        }
                    }
                }
            }
        }
    }

    /**
     * Add a chunk storage to a chunk.
     *
     * @param chunk   the chunk to add it to.
     * @param storage the said storage.
     */
    public static void addStorageToChunk(final Chunk chunk, final ChunkLoadStorage storage)
    {
        final IColonyTagCapability cap = chunk.getCapability(CLOSE_COLONY_CAP, null);
        storage.applyToCap(cap);
        chunk.markDirty();

        if (cap != null)
        {
            MineColonies.getNetwork().sendToAll(new UpdateChunkCapabilityMessage(cap, chunk.x, chunk.z));
        }
    }

    /**
     * Load the chunk storages from the server into the world.
     *
     * @param world the world to load them to.
     */
    public static void loadChunkStorageToWorldCapability(final World world)
    {
        @NotNull final File chunkDir = new File(world.getSaveHandler().getWorldDirectory(), CHUNK_INFO_PATH);
        if (!chunkDir.exists())
        {
            return;
        }

        final IChunkmanagerCapability chunkManager = world.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
        if (chunkManager == null)
        {
            Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT);
            return;
        }

        final File[] files = chunkDir.listFiles();
        if (files != null)
        {
            for (final File file : files)
            {
                @Nullable final NBTTagCompound chunkData = BackUpHelper.loadNBTFromPath(file);
                if (chunkData != null)
                {
                    final ChunkLoadStorage storage = new ChunkLoadStorage(chunkData);
                    final int z = (int) (storage.getXz() >> 32);
                    final int x = (int) storage.getXz();

                    chunkManager.addChunkStorage(x, z, storage);
                    file.delete();
                }
            }
        }
    }

    /**
     * Notify all chunks in the range of the colony about the colony.
     *
     * @param world the world.
     * @param add if add or remove.
     * @param id the colony id.
     * @param center the center chunk.
     * @param dimension the dimension.
     */
    public static void claimColonyChunks(final World world, final boolean add, final int id, final BlockPos center, final int dimension)
    {
        final int range = Configurations.gameplay.workingRangeTownHallChunks;
        final int buffer = Configurations.gameplay.townHallPaddingChunk;

        claimChunksInRange(id, dimension, add, center, range, buffer, world);
    }

    /**
     * Notify all chunks in the range of the colony about the colony.
     *
     * --- This is only for dynamic claiming ---
     *
     * @param world the world it was placed in.
     * @param add if add or remove.
     * @param id the id of the colony.
     * @param center the center position of the colony.
     * @param dimension the dimension it was placed in.
     * @param range the range to claim.
     */
    public static void claimColonyChunks(final World world, final boolean add, final int id, final BlockPos center, final int dimension, final int range)
    {
        claimChunksInRange(id, dimension, add, range, world, center);
    }

    /**
     * Check if all chunks within a certain range can be claimed, if range is too big this might require to load chunks.
     * Use carefully.
     *
     * --- This is only for dynamic claiming ---
     *
     * @param w the world.
     * @param pos the center position.
     * @param range the range to check.
     * @return true if possible.
     */
    public static boolean canClaimChunksInRange(final World w, final BlockPos pos, final int range)
    {
        final IChunkmanagerCapability worldCapability = w.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
        if (worldCapability == null)
        {
            return true;
        }
        final Chunk centralChunk = w.getChunk(pos);
        final int chunkX = centralChunk.x;
        final int chunkZ = centralChunk.z;

        for (int i = chunkX - range; i <= chunkX + range; i++)
        {
            for (int j = chunkZ - range; j <= chunkZ + range; j++)
            {
                final Chunk chunk = w.getChunk(i, j);
                final IColonyTagCapability colonyCap = chunk.getCapability(CLOSE_COLONY_CAP, null);
                if (colonyCap == null)
                {
                    return false;
                }
                final ChunkLoadStorage storage = worldCapability.getChunkStorage(chunk.x, chunk.z);
                if (storage != null)
                {
                    storage.applyToCap(colonyCap);
                }
                if (colonyCap.getOwningColony() != 0)
                {
                    return false;
                }
            }
        }
       return true;
    }


    /**
     * Claim a number of chunks in a certain range around a position.
     *
     * --- This is only for dynamic claiming ---
     *
     * @param colonyId  the colony id.
     * @param dimension the dimension.
     * @param add       if claim or unclaim.
     * @param range     the range.
     * @param world     the world.
     * @param center    the center position to be claimed.
     */
    public static void claimChunksInRange(
      final int colonyId,
      final int dimension,
      final boolean add,
      final int range,
      final World world,
      final BlockPos center)
    {
        final Chunk centralChunk = world.getChunk(center);
        loadChunkAndAddData(world, center, add, colonyId, center);

        final int chunkX = centralChunk.x;
        final int chunkZ = centralChunk.z;

        final IChunkmanagerCapability chunkManager = world.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
        if (chunkManager == null)
        {
            Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT);
            return;
        }

        final Colony colony = ColonyManager.getColonyByWorld(colonyId, world);
        if (colony == null)
        {
            return;
        }

        int additionalChunksToLoad = 0;
        for (int i = chunkX - range; i <= chunkX + range; i++)
        {
            for (int j = chunkZ - range; j <= chunkZ + range; j++)
            {
                final BlockPos pos = new BlockPos(i * BLOCKS_PER_CHUNK, 0, j * BLOCKS_PER_CHUNK);
                if (Configurations.gameplay.workingRangeTownHall != 0 && pos.distanceSq(colony.getCenter()) > Math.pow(Configurations.gameplay.workingRangeTownHall, 2))
                {
                    continue;
                }

                if (i == chunkX && j == chunkZ)
                {
                    continue;
                }
                if (loadChunkAndAddData(world, pos, add, colonyId, center))
                {
                    continue;
                }

                @NotNull final ChunkLoadStorage newStorage = new ChunkLoadStorage(colonyId, ChunkPos.asLong(i, j), dimension, center);
                if (!chunkManager.addChunkStorage(i, j, newStorage))
                {
                    additionalChunksToLoad++;
                }
            }
        }
        final IColonyManagerCapability cap = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension).getCapability(COLONY_MANAGER_CAP, null);
        cap.setMissingChunksToLoad(cap.getMissingChunksToLoad() + additionalChunksToLoad);
    }

    /**
     * Claim a number of chunks in a certain range around a position.
     *
     * @param colonyId  the colony id.
     * @param dimension the dimension.
     * @param add       if claim or unclaim.
     * @param center    the center position to be claimed.
     * @param range     the range.
     * @param buffer    the buffer.
     * @param world     the world.
     */
    public static void claimChunksInRange(
      final int colonyId,
      final int dimension,
      final boolean add,
      final BlockPos center,
      final int range,
      final int buffer,
      final World world)
    {
        final Chunk centralChunk = world.getChunk(center);
        loadChunkAndAddData(world, center, add, colonyId);

        final int chunkX = centralChunk.x;
        final int chunkZ = centralChunk.z;

        final IChunkmanagerCapability chunkManager = world.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
        if (chunkManager == null)
        {
            Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT);
            return;
        }

        final int maxRange = range * 2 + buffer;
        int additionalChunksToLoad = 0;
        for (int i = chunkX - maxRange; i <= chunkX + maxRange; i++)
        {
            for (int j = chunkZ - maxRange; j <= chunkZ + maxRange; j++)
            {
                if (i == chunkX && j == chunkZ)
                {
                    continue;
                }

                if (i >= chunkX - DISTANCE_TO_LOAD_IMMEDIATELY && j >= chunkZ - DISTANCE_TO_LOAD_IMMEDIATELY && i <= chunkX + DISTANCE_TO_LOAD_IMMEDIATELY
                      && j <= chunkZ + DISTANCE_TO_LOAD_IMMEDIATELY
                      && loadChunkAndAddData(world, new BlockPos(i * BLOCKS_PER_CHUNK, 0, j * BLOCKS_PER_CHUNK), add, colonyId))
                {
                    continue;
                }


                final boolean owning = i >= chunkX - range && j >= chunkZ - range && i <= chunkX + range && j <= chunkZ + range;
                @NotNull final ChunkLoadStorage newStorage = new ChunkLoadStorage(colonyId, ChunkPos.asLong(i, j), add, dimension, owning);
                if (!chunkManager.addChunkStorage(i, j, newStorage))
                {
                    additionalChunksToLoad++;
                }
            }
        }
        final IColonyManagerCapability cap = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension).getCapability(COLONY_MANAGER_CAP, null);
        cap.setMissingChunksToLoad(cap.getMissingChunksToLoad() + additionalChunksToLoad);
    }

    /**
     * This is a utility methods to detect chunks which are claimed in a certain range.
     *
     * @param chunkX the chunkX starter position.
     * @param chunkZ the chunkZ starter position.
     * @param range  the range.
     * @param buffer the buffer.
     * @param world  the world.
     */
    public static void debugChunksInRange(final int chunkX, final int chunkZ, final int range, final int buffer, final World world)
    {
        final IChunkmanagerCapability chunkManager = world.getCapability(CHUNK_STORAGE_UPDATE_CAP, null);
        if (chunkManager == null)
        {
            Log.getLogger().error(UNABLE_TO_FIND_WORLD_CAP_TEXT);
            return;
        }

        final int maxRange = range * 2 + buffer;
        for (int i = chunkX - maxRange; i <= chunkX + maxRange; i++)
        {
            for (int j = chunkZ - maxRange; j <= chunkZ + maxRange; j++)
            {
                final BlockPos pos = new BlockPos(i * BLOCKS_PER_CHUNK, 0, j * BLOCKS_PER_CHUNK);
                final Chunk chunk = world.getChunk(pos);
                if (chunk.getCapability(CLOSE_COLONY_CAP, null).getOwningColony() != 0)
                {
                    Log.getLogger().warn("Has owner: " + pos.toString());
                }
            }
        }
    }

    /**
     * Add the data to the chunk directly.
     *
     * @param world the world.
     * @param pos   the position.
     * @param add   if add or delete.
     * @param id    the id.
     * @return true if successful.
     */
    public static boolean loadChunkAndAddData(final World world, final BlockPos pos, final boolean add, final int id)
    {
        if (!world.isBlockLoaded(pos))
        {
            return false;
        }

        final Chunk chunk = world.getChunk(pos);
        if (chunk.getCapability(CLOSE_COLONY_CAP, null).getOwningColony() == id && add)
        {
            return false;
        }
        final IColonyTagCapability cap = chunk.getCapability(CLOSE_COLONY_CAP, null);

        if (cap == null)
        {
            return false;
        }

        if (add)
        {
            cap.setOwningColony(id);
            cap.addColony(id);
        }
        else
        {
            cap.removeColony(id);
        }

        chunk.markDirty();
        MineColonies.getNetwork().sendToAll(new UpdateChunkCapabilityMessage(cap, chunk.x, chunk.z));
        return true;
    }

    /**
     * Add the data to the chunk directly for dynamic claiming.
     *
     * ----- Only for dynamic claiming -----
     *
     * @param world the world.
     * @param pos   the position.
     * @param add   if add or delete.
     * @param id    the id.
     * @param buildingPos the building pos.
     * @return true if successful.
     */
    public static boolean loadChunkAndAddData(final World world, final BlockPos pos, final boolean add, final int id, final BlockPos buildingPos)
    {
        if (!world.isBlockLoaded(pos))
        {
            return false;
        }

        final Chunk chunk = world.getChunk(pos);
        final IColonyTagCapability cap = chunk.getCapability(CLOSE_COLONY_CAP, null);
        if (cap == null)
        {
            return false;
        }

        if (add)
        {
            cap.addBuildingClaim(id, buildingPos);
        }
        else
        {
            cap.removeBuildingClaim(id, buildingPos);
        }

        chunk.markDirty();
        MineColonies.getNetwork().sendToAll(new UpdateChunkCapabilityMessage(cap, chunk.x, chunk.z));
        return true;
    }
}
