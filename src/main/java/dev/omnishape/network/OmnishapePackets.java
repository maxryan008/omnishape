package dev.omnishape.network;

import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.network.packet.ResetCornersC2SPacket;
import dev.omnishape.network.packet.SetCornerC2SPacket;
import dev.omnishape.network.packet.SyncCornersS2CPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public class OmnishapePackets {
    public static void registerC2SPackets() {
        PayloadTypeRegistry.playS2C().register(
                SyncCornersS2CPacket.TYPE,
                SyncCornersS2CPacket.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                SetCornerC2SPacket.TYPE,
                SetCornerC2SPacket.CODEC
        );

        PayloadTypeRegistry.playC2S().register(
                ResetCornersC2SPacket.TYPE,
                ResetCornersC2SPacket.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SetCornerC2SPacket.TYPE,
                (packet, context) ->
                        context.server().execute(() -> {
                            var level = context.player().serverLevel();
                            var be = level.getBlockEntity(packet.pos());

                            if (be instanceof OmnibenchBlockEntity omnibench) {
                                omnibench.setCorner(
                                        packet.index(),
                                        packet.vec()
                                );

                                omnibench.setChanged();

                                syncCorners(
                                        level.players(),
                                        packet.pos(),
                                        omnibench
                                );
                            }
                        })
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ResetCornersC2SPacket.TYPE,
                (packet, context) ->
                        context.server().execute(() -> {
                            var level = context.player().serverLevel();
                            var be = level.getBlockEntity(packet.pos());

                            if (be instanceof OmnibenchBlockEntity omnibench) {
                                omnibench.resetCorners();

                                syncCorners(
                                        level.players(),
                                        packet.pos(),
                                        omnibench
                                );
                            }
                        })
        );
    }

    private static void syncCorners(
            Iterable<ServerPlayer> players,
            net.minecraft.core.BlockPos pos,
            OmnibenchBlockEntity omnibench
    ) {
        SyncCornersS2CPacket syncPacket =
                new SyncCornersS2CPacket(
                        pos,
                        omnibench.getCorners()
                );

        for (ServerPlayer player : players) {
            if (player.containerMenu instanceof OmnibenchMenu menu
                    && menu.getBlockEntity() == omnibench) {
                ServerPlayNetworking.send(
                        player,
                        syncPacket
                );
            }
        }
    }
}