package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeData;
import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.api.facade.FacadeRaycast;
import dev.omnishape.block.entity.FrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ExplosionDamageCalculator.class)
public class ExplosionDamageCalculatorMixin {

    @Inject(
            method = "getBlockExplosionResistance",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$overrideResistance(
            Explosion explosion,
            BlockGetter world,
            BlockPos pos,
            BlockState state,
            FluidState fluid,
            CallbackInfoReturnable<Optional<Float>> cir
    ) {
        BlockEntity blockEntity =
                world.getBlockEntity(pos);

        /*
         * Existing standalone Frame Block behaviour.
         */
        if (blockEntity instanceof FrameBlockEntity frame
                && frame.getData() != null) {

            BlockState camo =
                    frame.getData()
                            .camouflage();

            float resistance =
                    camo.getBlock()
                            .getExplosionResistance();

            cir.setReturnValue(
                    Optional.of(
                            Math.max(
                                    resistance,
                                    fluid.getExplosionResistance()
                            )
                    )
            );

            return;
        }

        Optional<FacadeData> facadeOptional =
                FacadeManager.getFacade(
                        world,
                        pos
                );

        if (facadeOptional.isEmpty()) {
            return;
        }

        /*
         * Determine which component an explosion travelling from the
         * explosion centre toward this block reaches first.
         *
         * Example:
         *
         * TNT -> obsidian panel -> cable
         *       facade wins
         *
         * TNT -> exposed cable
         *       host wins
         */
        Vec3 start =
                explosion.center();

        Vec3 end =
                pos.getCenter();

        FacadeRaycast.HitPart part =
                FacadeRaycast.raycast(
                        world,
                        pos,
                        start,
                        end
                );

        if (part != FacadeRaycast.HitPart.FACADE) {
            /*
             * Exposed cable: vanilla host resistance remains in effect.
             */
            return;
        }

        FacadeData facade =
                facadeOptional.get();

        float facadeResistance =
                facade.omnishape()
                        .camouflage()
                        .getBlock()
                        .getExplosionResistance();

        cir.setReturnValue(
                Optional.of(
                        Math.max(
                                facadeResistance,
                                fluid.getExplosionResistance()
                        )
                )
        );
    }
}