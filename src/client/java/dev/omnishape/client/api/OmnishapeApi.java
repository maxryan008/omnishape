package dev.omnishape.client.api;

import dev.omnishape.api.OmnishapeData;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class OmnishapeApi {
    private static final boolean ENABLED = FabricLoader.getInstance().isModLoaded("omnishape");
    private static Internal internal = Dummy.INSTANCE; // CHANGED: moved from interface to field

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static boolean hasOverlay(Level world, BlockPos pos) {
        return ENABLED && internal.hasOverlay(world, pos);
    }

    public static Optional<OmnishapeData> getOverlay(Level world, BlockPos pos) {
        return ENABLED ? internal.getOverlay(world, pos) : Optional.empty();
    }

    public static void renderOverlay(OmnishapeData data, BakedModel model, List<BakedQuad> quads, RenderType layer) {
        if (ENABLED) internal.renderOverlay(data, model, quads, layer);
    }

    // Add this method:
    public static void register(Internal impl) {
        if (ENABLED && impl != null) {
            internal = impl;
            System.out.println("[Omnishape] Internal API registered: " + impl.getClass().getName());
        }
    }

    public interface Internal {
        boolean hasOverlay(Level world, BlockPos pos);
        Optional<OmnishapeData> getOverlay(Level world, BlockPos pos);
        void renderOverlay(OmnishapeData data, BakedModel model, List<BakedQuad> quads, RenderType layer);
    }

    private static final class Dummy implements Internal {
        static final Dummy INSTANCE = new Dummy();

        public boolean hasOverlay(Level world, BlockPos pos) { return false; }
        public Optional<OmnishapeData> getOverlay(Level world, BlockPos pos) { return Optional.empty(); }
        public void renderOverlay(OmnishapeData data, BakedModel model, List<BakedQuad> quads, RenderType layer) {
            System.err.println("[Omnishape] Warning: renderOverlay() called on Dummy fallback.");
        }
    }
}