package dev.omnishape.network.packet;

import com.mojang.math.Vector3f;
import dev.omnishape.Constant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record SyncCornersS2CPacket(BlockPos pos, Vector3f[] corners) {
    public static final ResourceLocation ID = Constant.id("sync_corners_s2c");

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(corners.length);
        for (Vector3f vec : corners) {
            buf.writeFloat(vec.x());
            buf.writeFloat(vec.y());
            buf.writeFloat(vec.z());
        }
    }

    public static SyncCornersS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int len = buf.readVarInt();
        Vector3f[] corners = new Vector3f[len];
        for (int i = 0; i < len; i++) {
            corners[i] = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        }
        return new SyncCornersS2CPacket(pos, corners);
    }
}