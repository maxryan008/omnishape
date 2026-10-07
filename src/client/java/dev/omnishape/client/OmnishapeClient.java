package dev.omnishape.client;

import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.client.gui.OmnibenchScreen;
import dev.omnishape.client.model.OmnishapeModelLoader;
import dev.omnishape.network.packet.FacadeSyncS2CPacket;
import dev.omnishape.network.packet.SyncCornersS2CPacket;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import org.joml.Vector3f;

public class OmnishapeClient
        implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(
                OmnishapeModelLoader.INSTANCE
        );

        MenuScreens.register(
                OmnishapeMenus.OMNIBENCH_MENU,
                OmnibenchScreen::new
        );

        ClientPlayNetworking.registerGlobalReceiver(
                SyncCornersS2CPacket.TYPE,
                (packet, context) -> {
                    Minecraft client =
                            context.client();

                    context.client().execute(
                            () -> {
                                if (client.level == null) {
                                    return;
                                }

                                var be =
                                        client.level.getBlockEntity(
                                                packet.pos()
                                        );

                                if (!(be
                                        instanceof OmnibenchBlockEntity omnibench)) {
                                    return;
                                }

                                Vector3f[] newCorners =
                                        packet.corners();

                                for (int i = 0;
                                     i < newCorners.length
                                             && i < omnibench.getCorners().length;
                                     i++) {

                                    omnibench.getCorner(i)
                                            .set(
                                                    newCorners[i]
                                            );
                                }

                                omnibench.getInventory()
                                        .setChanged();
                            }
                    );
                }
        );

        ClientPlayNetworking.registerGlobalReceiver(
                FacadeSyncS2CPacket.TYPE,
                (packet, context) ->
                        context.client().execute(
                                () -> {
                                    Minecraft client =
                                            context.client();

                                    if (client.level == null) {
                                        return;
                                    }

                                    FacadeManager.applyClientSync(
                                            client.level,
                                            packet.pos(),
                                            packet.facade()
                                    );
                                }
                        )
        );
    }
}