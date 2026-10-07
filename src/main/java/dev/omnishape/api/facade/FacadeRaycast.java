package dev.omnishape.api.facade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FacadeRaycast {

    public enum HitPart {
        NONE,
        HOST,
        FACADE
    }

    private FacadeRaycast() {
    }

    public static HitPart raycastPlayer(
            Player player,
            BlockGetter level,
            BlockPos pos
    ) {
        FacadeData facade =
                FacadeManager.getFacadeOrNull(
                        level,
                        pos
                );

        if (facade == null) {
            return HitPart.HOST;
        }

        return raycastPlayer(
                player,
                level,
                pos,
                facade
        );
    }

    /**
     * Fast path for callers that already fetched the facade.
     */
    public static HitPart raycastPlayer(
            Player player,
            BlockGetter level,
            BlockPos pos,
            FacadeData facade
    ) {
        Vec3 start =
                player.getEyePosition();

        double reach =
                player.blockInteractionRange();

        Vec3 end =
                start.add(
                        player.getViewVector(
                                1.0F
                        ).scale(
                                reach
                        )
                );

        return raycast(
                level,
                pos,
                start,
                end,
                CollisionContext.of(
                        player
                ),
                facade
        );
    }

    public static HitPart raycast(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end
    ) {
        FacadeData facade =
                FacadeManager.getFacadeOrNull(
                        level,
                        pos
                );

        if (facade == null) {
            return HitPart.HOST;
        }

        return raycast(
                level,
                pos,
                start,
                end,
                CollisionContext.empty(),
                facade
        );
    }

    /**
     * Fast path for systems such as explosion handling that already know
     * which facade they are evaluating.
     */
    public static HitPart raycast(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end,
            FacadeData facade
    ) {
        return raycast(
                level,
                pos,
                start,
                end,
                CollisionContext.empty(),
                facade
        );
    }

    private static HitPart raycast(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end,
            CollisionContext context,
            FacadeData facade
    ) {
        BlockState hostState =
                level.getBlockState(
                        pos
                );

        /*
         * We explicitly request the host shape without the facade attached,
         * otherwise we'd just be comparing:
         *
         *     host + facade
         *
         * against:
         *
         *     facade
         */
        VoxelShape hostShape =
                FacadeContext.withoutFacadeShape(
                        () ->
                                hostState.getShape(
                                        level,
                                        pos,
                                        context
                                )
                );

        VoxelShape facadeShape =
                facade.getShape();

        BlockHitResult hostHit =
                hostShape.clip(
                        start,
                        end,
                        pos
                );

        BlockHitResult facadeHit =
                facadeShape.clip(
                        start,
                        end,
                        pos
                );

        if (hostHit == null) {
            return facadeHit == null
                    ? HitPart.NONE
                    : HitPart.FACADE;
        }

        if (facadeHit == null) {
            return HitPart.HOST;
        }

        double hostDistance =
                hostHit.getLocation()
                        .distanceToSqr(
                                start
                        );

        double facadeDistance =
                facadeHit.getLocation()
                        .distanceToSqr(
                                start
                        );

        return facadeDistance <= hostDistance
                ? HitPart.FACADE
                : HitPart.HOST;
    }
}