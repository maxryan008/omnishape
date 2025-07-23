package dev.omnishape.network.packet;

import dev.omnishape.Constant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record SyncCornersS2CPacket(BlockPos pos, Vector3f[] corners) implements CustomPacketPayload {
    public static final Type<SyncCornersS2CPacket> TYPE =
            new Type<>(Constant.id("sync_corners_s2c"));

    public static final StreamCodec<FriendlyByteBuf, SyncCornersS2CPacket> CODEC =
            StreamCodec.of(SyncCornersS2CPacket::write, SyncCornersS2CPacket::read);

    public static void write(FriendlyByteBuf buf, SyncCornersS2CPacket packet) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.corners.length);
        for (Vector3f vec : packet.corners) {
            buf.writeFloat(vec.x);
            buf.writeFloat(vec.y);
            buf.writeFloat(vec.z);
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

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}