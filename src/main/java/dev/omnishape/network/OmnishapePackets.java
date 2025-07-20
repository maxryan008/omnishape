package dev.omnishape.network;

import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.network.packet.SetCornerC2SPacket;
import dev.omnishape.network.packet.SyncCornersS2CPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OmnishapePackets {
    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SetCornerC2SPacket.ID, (server, player, handler, buf, responseSender) -> {
            SetCornerC2SPacket packet = SetCornerC2SPacket.read(buf);

            server.execute(() -> {
                BlockEntity be = player.getLevel().getBlockEntity(packet.pos());
                if (be instanceof OmnibenchBlockEntity omnibench) {
                    omnibench.setCorner(packet.index(), packet.vec());
                    omnibench.setChanged();

                    // Sync back to all watching players with OmnibenchMenu open
                    SyncCornersS2CPacket syncPacket = new SyncCornersS2CPacket(packet.pos(), omnibench.getCorners());
                    for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                        if (p.containerMenu instanceof OmnibenchMenu menu && menu.getBlockEntity() == omnibench) {
                            FriendlyByteBuf buf2 = new FriendlyByteBuf(Unpooled.buffer());
                            new SyncCornersS2CPacket(packet.pos(), omnibench.getCorners()).write(buf2);
                            ServerPlayNetworking.send(p, SyncCornersS2CPacket.ID, buf2);
                        }
                    }
                }
            });
        });
    }
}
