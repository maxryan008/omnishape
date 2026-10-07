package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeContext;
import dev.omnishape.api.facade.FacadeData;
import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.api.facade.FacadeRaycast;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateFacadeMixin {

    /*
     * Target the context-free BlockStateBase#getShape(BlockGetter, BlockPos)
     * overload explicitly.
     *
     * This is the overload used by a large amount of vanilla block querying
     * code, and targeting the descriptor prevents Mixin from selecting the
     * CollisionContext overload by ambiguity.
     */
    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void omnishape$includeFacadeSelectionShape(
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (FacadeContext.isShapeBypassed()) {
            return;
        }

        Optional<FacadeData> facade =
                FacadeManager.getFacade(
                        level,
                        pos
                );

        if (facade.isEmpty()) {
            return;
        }

        cir.setReturnValue(
                Shapes.or(
                        cir.getReturnValue(),
                        facade.get().getShape()
                )
        );
    }

    /*
     * Same idea for the context-free collision shape overload.
     */
    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void omnishape$includeFacadeCollisionShape(
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (FacadeContext.isShapeBypassed()) {
            return;
        }

        Optional<FacadeData> facade =
                FacadeManager.getFacade(
                        level,
                        pos
                );

        if (facade.isEmpty()) {
            return;
        }

        cir.setReturnValue(
                Shapes.or(
                        cir.getReturnValue(),
                        facade.get().getShape()
                )
        );
    }

    @Inject(
            method = "getDestroyProgress",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$facadeMiningSpeed(
            Player player,
            BlockGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Float> cir
    ) {
        if (FacadeContext.isMiningBypassed()) {
            return;
        }

        Optional<FacadeData> facadeOptional =
                FacadeManager.getFacade(
                        level,
                        pos
                );

        if (facadeOptional.isEmpty()) {
            return;
        }

        if (FacadeRaycast.raycastPlayer(
                player,
                level,
                pos
        ) != FacadeRaycast.HitPart.FACADE) {
            return;
        }

        FacadeData facade =
                facadeOptional.get();

        float progress =
                FacadeContext.withoutFacadeMining(
                        () ->
                                facade.omnishape()
                                        .camouflage()
                                        .getDestroyProgress(
                                                player,
                                                level,
                                                pos
                                        )
                );

        cir.setReturnValue(
                progress
        );
    }
}