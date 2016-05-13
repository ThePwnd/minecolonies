package com.minecolonies.util;

import com.minecolonies.entity.EntityCitizen;
import com.minecolonies.entity.pathfinding.PathResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class BlockPosUtil
{
    /**
     * Writes a Chunk Coordinate to an NBT compound, with a specific tag name
     *
     * @param compound Compound to write to
     * @param name     Name of the tag
     * @param pos   Coordinates to write to NBT
     */
    public static void writeToNBT(NBTTagCompound compound, String name, BlockPos pos)
    {
        NBTTagCompound coordsCompound = new NBTTagCompound();
        coordsCompound.setInteger("x", pos.getX());
        coordsCompound.setInteger("y", pos.getY());
        coordsCompound.setInteger("z", pos.getZ());
        compound.setTag(name, coordsCompound);
    }

    /**
     * Reads Chunk Coordinates from an NBT Compound with a specific tag name
     *
     * @param compound Compound to read data from
     * @param name     Tag name to read data from
     * @return Chunk coordinates read from the compound
     */
    public static BlockPos readFromNBT(NBTTagCompound compound, String name)
    {
        NBTTagCompound coordsCompound = compound.getCompoundTag(name);
        int            x              = coordsCompound.getInteger("x");
        int            y              = coordsCompound.getInteger("y");
        int            z              = coordsCompound.getInteger("z");
        return new BlockPos(x, y, z);
    }

    /**
     * Write a compound with chunk coordinate to a tag list.
     *
     * @param tagList Tag list to write compound with chunk coordinates to
     * @param pos  Coordinate to write to the tag list
     */
    public static void writeToNBTTagList(NBTTagList tagList, BlockPos pos)
    {
        NBTTagCompound coordsCompound = new NBTTagCompound();
        coordsCompound.setInteger("x", pos.getX());
        coordsCompound.setInteger("y", pos.getY());
        coordsCompound.setInteger("z", pos.getZ());
        tagList.appendTag(coordsCompound);
    }

    /**
     * Reads a Chunk Coordinate from a tag list.
     *
     * @param tagList Tag list to read compound with chunk coordinate from
     * @param index   Index in the tag list where the required chunk coordinate is
     * @return Chunk coordinate read from the tag list
     */
    public static BlockPos readFromNBTTagList(NBTTagList tagList, int index)
    {
        NBTTagCompound coordsCompound = tagList.getCompoundTagAt(index);
        int            x              = coordsCompound.getInteger("x");
        int            y              = coordsCompound.getInteger("y");
        int            z              = coordsCompound.getInteger("z");
        return new BlockPos(x, y, z);
    }

    /**
     * Writes chunk coordinates to a {@link ByteBuf}
     *
     * @param buf    Buf to write to
     * @param pos Coordinate to write
     */
    public static void writeToByteBuf(ByteBuf buf, BlockPos pos)
    {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    /**
     * Read chunk coordinates from a {@link ByteBuf}
     *
     * @param buf Buf to read from
     * @return Chunk coordinate that was read
     */
    public static BlockPos readFromByteBuf(ByteBuf buf)
    {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        return new BlockPos(x, y, z);
    }

    /**
     * Returns if the {@link #distanceSqrd(BlockPos, Vec3)} from a coordinate to an cititzen is closer than 4.84
     *
     * @param coordinate Coordinate you want check distance of
     * @param citizen    Citizen you want check distance of
     * @return Whether or not the distance is less than 4.84
     */
    public static boolean isClose(BlockPos coordinate, EntityCitizen citizen)
    {
        return distanceSqrd(coordinate, citizen.getPosition()) < 4.84;
    }

    public static int getDistanceSquared(BlockPos block1, BlockPos block2){
        float f = (float)(block1.getX() - block2.getX());
        float f1 = (float)(block1.getY() - block2.getY());
        float f2 = (float)(block1.getZ() - block2.getZ());
        return  (int) (f * f + f1 * f1 + f2 * f2);
    }

    /**
     * Returns the squared distance to a Vec3
     *
     * @param coords1 BlockPos   (point 1)
     * @param coords2 BlockPos   (point 2)
     * @return Squared distance between points (float)
     */
    public static float distanceSqrd(BlockPos coords1, BlockPos coords2)
    {
        return getDistanceSquared(coords1, coords2); // TODO: does this work
    	//return getDistanceSquared(coords1, new BlockPos((int) coords2.xCoord, (int) coords2.yCoord, (int) coords2.zCoord));
    }

    /**
     * Returns the tile entity at a specific chunk coordinate
     *
     * @param world  World the tile entity is in
     * @param pos Coordinates of the tile entity
     * @return Tile entity at the given coordinates
     */
    public static TileEntity getTileEntity(World world, BlockPos pos)
    {
        return world.getTileEntity(pos);
    }

    /**
     * Returns a list of drops possible mining a specific block with specific fortune level
     *
     * @param world   World the block is in
     * @param coords  Coordinates of the block
     * @param fortune Level of fortune on the pickaxe
     * @return List of {@link ItemStack} with possible drops
     */
    public static List<ItemStack> getBlockDrops(World world, BlockPos coords, int fortune)
    {
        return getBlock(world, coords).getDrops(world, new BlockPos(coords.getX(), coords.getY(), coords.getZ()), getBlockState(world, coords), fortune);
    }

    /**
     * Returns the block at a specific chunk coordinate
     *
     * @param world  World the block is in
     * @param coords Coordinates of the block
     * @return Block at the given coordinates
     */
    public static Block getBlock(World world, BlockPos coords)
    {
        return world.getBlockState(coords).getBlock();
    }

    /**
     * Returns the metadata of a block at a specific chunk coordinate
     *
     * @param world  World the block is in
     * @param coords Coordinates of the block
     * @return Metadata of the block at the given coordinates
     */
    public static IBlockState getBlockState(World world, BlockPos coords)
    {
        return world.getBlockState(coords);
    }

    /**
     * Sets a block in the world
     *
     * @param world  World the block needs to be set in
     * @param coords Coordinate to place block
     * @param block  Block to place
     * @return True if block is placed, otherwise false
     */
    public static boolean setBlock(World world, BlockPos coords, Block block)
    {
    	return world.setBlockState(coords, block.getDefaultState());
    }

    /**
     * Sets a block in the world, with specific metadata and flags
     *
     * @param worldIn  World the block needs to be set in
     * @param coords   Coordinate to place block
     * @param state    BlockState to be placed
     * @param flag     Flag to set
     * @return True if block is placed, otherwise false
     */
    public static boolean setBlock(World worldIn, BlockPos coords, IBlockState state, int flag)
    {
    	return worldIn.setBlockState(coords, state, flag);
    }

    /**
     * Returns whether or not the citizen is heading to a specific location
     *
     * @param citizen Citizen you want to check
     * @param pos     Position you want to check
     * @return True if citizen heads to pos, otherwise false
     * @see {@link EntityUtils#isPathingTo(EntityCitizen, int, int)}
     */
    public static boolean isPathingTo(EntityCitizen citizen, BlockPos pos)
    {
        return EntityUtils.isPathingTo(citizen, pos.getX(), pos.getZ());
    }

    /**
     * @param worker Worker to check
     * @param site   Chunk coordinates of site to check
     * @return True when worker is at site, otherwise false
     * @see {@link EntityUtils#isWorkerAtSiteWithMove(EntityCitizen, int, int, int)}
     */
    public static boolean isWorkerAtSiteWithMove(EntityCitizen worker, BlockPos site)
    {
        return EntityUtils.isWorkerAtSiteWithMove(worker, site.getX(), site.getY(), site.getZ());
    }

    /**
     * @param worker Worker to check
     * @param site   Chunk coordinates of site to check
     * @param range  Range to check in
     * @return True when within range, otherwise false
     * @see {@link EntityUtils#isWorkerAtSiteWithMove(EntityCitizen, int, int, int, int)}
     */
    public static boolean isWorkerAtSiteWithMove(EntityCitizen worker, BlockPos site, int range)
    {
        return EntityUtils.isWorkerAtSiteWithMove(worker, site.getX(), site.getY(), site.getZ(), range);
    }

    /**
     * @param living      A living entity
     * @param destination chunk coordinates to check moving to
     * @return True when XYZ is found, an set moving to, otherwise false
     * @see {@link EntityUtils#tryMoveLivingToXYZ(EntityLiving, int, int, int)}
     */
    public static boolean tryMoveLivingToXYZ(EntityLiving living, BlockPos destination)
    {
        return EntityUtils.tryMoveLivingToXYZ(living, destination.getX(), destination.getY(), destination.getZ());
    }

    /**
     * Attempt to move to XYZ.
     * True when found and destination is set
     *
     * @param citizen     Citizen to move to XYZ
     * @param destination Chunk coordinate of the distance
     * @return True when found, and destination is set, otherwise false
     */
    public static PathResult moveLivingToXYZ(EntityCitizen citizen, BlockPos destination)
    {
        return citizen.getNavigator().moveToXYZ(destination.getX(), destination.getY(), destination.getZ(), 1.0);
    }

    /**
     * Returns whether a chunk coordinate is equals to (x, y, z)
     *
     * @param coords Chunk Coordinate    (point 1)
     * @param x      x-coordinate        (point 2)
     * @param y      y-coordinate        (point 2)
     * @param z      z-coordinate        (point 2)
     * @return True when coordinates are equal, otherwise false
     */
    public static boolean equals(BlockPos coords, int x, int y, int z)
    {
        return coords.getX() == x && coords.getY() == y && coords.getZ() == z;
    }

    /**
     * Substract two coordinates, and returns the result
     *
     * @param coords1 Chunk Coordinate (point 1)
     * @param coords2 Chunk Coordinate (point 2)
     * @return coordinates with the result of {@code coords1} minus {@code coords2}
     */
    public static BlockPos subtract(BlockPos coords1, BlockPos coords2)
    {
        return new BlockPos(coords1.getX() - coords2.getX(), coords1.getY() - coords2.getY(), coords1.getZ() - coords2.getZ());
    }

    /**
     * Add two coordinates, and returns the result
     *
     * @param coords Chunk Coordinate (point 1)
     * @param x      x-coordinate (point 2)
     * @param y      y-coordinate (point 2)
     * @param z      z-coordinate (point 2)
     * @return the result of {@code x},{@code y},{@code z} added to {@code coords}
     */
    public static BlockPos add(BlockPos coords, int x, int y, int z)
    {
        return new BlockPos(coords.getX() + x, coords.getY() + y, coords.getZ() + z);
    }

    /**
     * Returns the Chunk Coordinate created from an entity
     *
     * @param entity Entity to create chunk coordinates from
     * @return Chunk Coordinates created from the entity
     */
    public static BlockPos fromEntity(Entity entity)
    {
        return new BlockPos(MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ));
    }
}