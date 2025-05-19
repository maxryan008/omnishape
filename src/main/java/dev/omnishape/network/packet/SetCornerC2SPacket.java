package dev.omnishape.network.packet;

import dev.omnishape.Omnishape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public record SetCornerC2SPacket(BlockPos pos, int index, Vector3f vec) implements CustomPacketPayload {
    public static final Type<SetCornerC2SPacket> TYPE =
            new Type<>(Omnishape.id("set_corner_c2s"));

    public static final StreamCodec<FriendlyByteBuf, SetCornerC2SPacket> CODEC =
            StreamCodec.of(SetCornerC2SPacket::write, SetCornerC2SPacket::read);

    public static void write(FriendlyByteBuf buf, SetCornerC2SPacket packet) {
        buf.writeBlockPos(packet.pos);
        buf.writeVarInt(packet.index);
        buf.writeFloat(packet.vec.x);
        buf.writeFloat(packet.vec.y);
        buf.writeFloat(packet.vec.z);
    }

    public static SetCornerC2SPacket read(FriendlyByteBuf buf) {
        return new SetCornerC2SPacket(
                buf.readBlockPos(),
                buf.readVarInt(),
                new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat())
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}