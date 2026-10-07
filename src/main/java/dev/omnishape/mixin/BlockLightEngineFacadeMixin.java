package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLightEngine.class)
public abstract class BlockLightEngineFacadeMixin {

    @Redirect(
            method = "getEmission",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;getLightEmission()I"
            )
    )
    private int omnishape$getFacadeLightEmission(
            BlockState state,
            long packedPos,
            BlockState methodState
    ) {
        LightChunkGetter chunkSource =
                ((LightEngineAccessor) this)
                        .omnishape$getChunkSource();

        BlockGetter level =
                chunkSource.getLevel();

        BlockPos pos =
                BlockPos.of(
                        packedPos
                );

        return FacadeManager.getFacade(
                        level,
                        pos
                )
                .map(
                        facade ->
                                facade.omnishape()
                                        .camouflage()
                                        .getLightEmission()
                )
                .orElseGet(
                        state::getLightEmission
                );
    }
}