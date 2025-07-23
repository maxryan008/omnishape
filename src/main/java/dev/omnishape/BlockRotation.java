package dev.omnishape;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class BlockRotation implements Comparable<BlockRotation> {
    public static final BlockRotation IDENTITY = new BlockRotation(0, 0, 0);

    public final int pitch; // X
    public final int yaw;   // Y
    public final int roll;  // Z

    public BlockRotation(int pitch, int yaw, int roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public static BlockRotation fromPlacementContext(BlockPlaceContext ctx) {
        Direction face = ctx.getClickedFace(); // Face the player clicked (bottom of block)
        Vec3 playerPos = ctx.getPlayer().position();
        Vec3 blockCenter = ctx.getClickedPos().getCenter();
        Vec3 toPlayer = playerPos.subtract(blockCenter);

        int pitch = 0, yaw = 0, roll = 0;

        // Rotation axis = axis perpendicular to clicked face
        Direction.Axis axis = face.getAxis();

        // Determine rotation angle based on 2D direction from block to player on perpendicular plane
        if (axis == Direction.Axis.Y) {
            double dx = toPlayer.x;
            double dz = toPlayer.z;
            yaw = (int) Math.round(Math.toDegrees(Math.atan2(dx, dz)) / 90) * 90;
            yaw += 180;
            yaw = Math.floorMod(yaw, 360);
            roll = (face == Direction.UP) ? 0 : 180;
        } else if (axis == Direction.Axis.Z) {
            double dx = toPlayer.x;
            double dy = toPlayer.y;
            yaw = (face == Direction.NORTH) ? 0 : 180;
            roll = (int) Math.round(Math.toDegrees(Math.atan2(dy, dx)) / 90) * 90;
            roll = (face == Direction.SOUTH) ? -roll + 90 : roll - 90;
            roll = Math.floorMod(roll, 360);
        } else if (axis == Direction.Axis.X) {
            double dz = toPlayer.z;
            double dy = toPlayer.y;
            pitch = (int) Math.round(Math.toDegrees(Math.atan2(dy, -dz)) / 90) * 90;
            pitch = Math.floorMod(pitch, 360);
            roll = (face == Direction.WEST) ? 90 : 270;
        }

        return new BlockRotation(pitch, yaw, roll);
    }

    public static BlockRotation fromTag(CompoundTag tag) {
        return new BlockRotation(tag.getInt(Constant.Nbt.PITCH), tag.getInt(Constant.Nbt.YAW), tag.getInt(Constant.Nbt.ROLL));
    }

    public Vector3f toVector() {
        return new Vector3f(pitch, yaw, roll);
    }

    public Tag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(Constant.Nbt.PITCH, pitch);
        tag.putInt(Constant.Nbt.YAW, yaw);
        tag.putInt(Constant.Nbt.ROLL, roll);
        return tag;
    }

    @Override
    public int compareTo(@NotNull BlockRotation o) {
        int cmp = Integer.compare(this.pitch, o.pitch);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.yaw, o.yaw);
        if (cmp != 0) return cmp;
        return Integer.compare(this.roll, o.roll);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BlockRotation other)) return false;
        return pitch == other.pitch && yaw == other.yaw && roll == other.roll;
    }

    @Override
    public int hashCode() {
        return pitch * 31 * 31 + yaw * 31 + roll;
    }

    @Override
    public String toString() {
        return "BlockRotation[pitch=%d, yaw=%d, roll=%d]".formatted(pitch, yaw, roll);
    }
}
