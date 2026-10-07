package dev.omnishape.network.packet;

import dev.omnishape.Constant;
import dev.omnishape.api.facade.FacadeData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record FacadeSyncS2CPacket(
        BlockPos pos,
        @Nullable FacadeData facade
) implements CustomPacketPayload {

    public static final Type<FacadeSyncS2CPacket> TYPE =
            new Type<>(
                    Constant.id(
                            "facade_sync_s2c"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            FacadeSyncS2CPacket
            > CODEC =
            StreamCodec.of(
                    FacadeSyncS2CPacket::write,
                    FacadeSyncS2CPacket::read
            );

    private static void write(
            FriendlyByteBuf buf,
            FacadeSyncS2CPacket packet
    ) {
        buf.writeBlockPos(
                packet.pos
        );

        boolean present =
                packet.facade != null;

        buf.writeBoolean(
                present
        );

        if (present) {
            buf.writeNbt(
                    packet.facade.toNbt()
            );
        }
    }

    private static FacadeSyncS2CPacket read(
            FriendlyByteBuf buf
    ) {
        BlockPos pos =
                buf.readBlockPos();

        boolean present =
                buf.readBoolean();

        if (!present) {
            return new FacadeSyncS2CPacket(
                    pos,
                    null
            );
        }

        CompoundTag tag =
                buf.readNbt();

        if (tag == null) {
            return new FacadeSyncS2CPacket(
                    pos,
                    null
            );
        }

        return new FacadeSyncS2CPacket(
                pos,
                FacadeData.fromNbt(tag)
        );
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}