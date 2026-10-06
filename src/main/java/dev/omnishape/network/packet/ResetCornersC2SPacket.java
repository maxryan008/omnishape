package dev.omnishape.network.packet;

import dev.omnishape.Constant;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;

public record ResetCornersC2SPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ResetCornersC2SPacket> TYPE =
            new Type<>(Constant.id("reset_corners_c2s"));

    public static final StreamCodec<FriendlyByteBuf, ResetCornersC2SPacket> CODEC =
            StreamCodec.of(ResetCornersC2SPacket::write, ResetCornersC2SPacket::read);

    private static void write(FriendlyByteBuf buf, ResetCornersC2SPacket packet) {
        buf.writeBlockPos(packet.pos);
    }

    private static ResetCornersC2SPacket read(FriendlyByteBuf buf) {
        return new ResetCornersC2SPacket(buf.readBlockPos());
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}