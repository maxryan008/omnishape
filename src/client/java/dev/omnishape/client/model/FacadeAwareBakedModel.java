package dev.omnishape.client.model;

import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.client.api.OmnishapeRenderer;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class FacadeAwareBakedModel
        extends ForwardingBakedModel {

    public FacadeAwareBakedModel(
            net.minecraft.client.resources.model.BakedModel wrapped
    ) {
        this.wrapped = wrapped;
    }

    @Override
    public void emitBlockQuads(
            BlockAndTintGetter world,
            BlockState state,
            BlockPos pos,
            Supplier<RandomSource> randomSupplier,
            RenderContext context
    ) {
        /*
         * Render the foreign block exactly as it normally would first.
         */
        wrapped.emitBlockQuads(
                world,
                state,
                pos,
                randomSupplier,
                context
        );

        /*
         * Then append OmniShape facade geometry.
         */
        FacadeManager.getFacade(
                world,
                pos
        ).ifPresent(
                facade ->
                        OmnishapeRenderer.emitOverlay(
                                facade.omnishape(),
                                context,
                                pos,
                                facade.rotation(),
                                world
                        )
        );
    }

    @Override
    public boolean isVanillaAdapter() {
        /*
         * Forces the Fabric renderer path so emitBlockQuads() above is
         * actually invoked.
         */
        return false;
    }
}