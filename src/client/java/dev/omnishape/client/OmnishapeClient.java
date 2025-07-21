package dev.omnishape.client;

import dev.omnishape.client.api.OmnishapeApi;
import dev.omnishape.api.OmnishapeData;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.client.gui.OmnibenchScreen;
import dev.omnishape.client.model.OmnishapeModelLoader;
import dev.omnishape.network.packet.SyncCornersS2CPacket;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

public class OmnishapeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Model loader (baked models)
        ModelLoadingPlugin.register(OmnishapeModelLoader.INSTANCE);

        // UI screen binding
        MenuScreens.register(OmnishapeMenus.OMNIBENCH_MENU, OmnibenchScreen::new);

        // Corner sync handler
        ClientPlayNetworking.registerGlobalReceiver(SyncCornersS2CPacket.TYPE, (packet, context) -> {
                    Minecraft client = context.client();
                    if (client.level == null) return;
                    context.client().execute(() -> {
                        var be = context.client().level.getBlockEntity(packet.pos());
                        if (be instanceof OmnibenchBlockEntity omnibench) {
                            Vector3f[] newCorners = packet.corners();
                            for (int i = 0; i < newCorners.length; i++) {
                                omnibench.getCorner(i).set(newCorners[i]);
                            }
                            omnibench.getInventory().setChanged();
                        }
                    });
                }
        );

        // Register no-op API implementation for Omnishape itself
        OmnishapeApi.register(new OmnishapeApi.Internal() {
            @Override
            public boolean hasOverlay(Level world, BlockPos pos) {
                return false;
            }

            @Override
            public Optional<OmnishapeData> getOverlay(Level world, BlockPos pos) {
                return Optional.empty();
            }

            @Override
            public void renderOverlay(OmnishapeData data, BakedModel model, List<BakedQuad> quads, RenderType layer) {
                // Should never be called inside Omnishape itself
                System.err.println("[Omnishape] Warning: renderOverlay() called internally.");
            }
        });
    }
}
