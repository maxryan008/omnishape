package dev.omnishape.client;

import com.mojang.math.Vector3f;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.client.gui.OmnibenchScreen;
import dev.omnishape.client.model.OmnishapeModelLoader;
import dev.omnishape.network.packet.SyncCornersS2CPacket;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;

public class OmnishapeClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        OmnishapeModelLoader.register();

        MenuScreens.register(OmnishapeMenus.OMNIBENCH_MENU, OmnibenchScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(SyncCornersS2CPacket.ID, (Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender) -> {
            SyncCornersS2CPacket packet = SyncCornersS2CPacket.read(buf);
            client.execute(() -> {
                ClientLevel level = client.level;
                if (level == null) return;

                var be = level.getBlockEntity(packet.pos());
                if (be instanceof OmnibenchBlockEntity omnibench) {
                    Vector3f[] newCorners = packet.corners();
                    for (int i = 0; i < newCorners.length; i++) {
                        omnibench.getCorner(i).set(newCorners[i].x(), newCorners[i].y(), newCorners[i].z());
                    }
                    omnibench.getInventory().setChanged();
                }
            });
        });
    }
}