package dev.omnishape.api.facade;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

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
        Vec3 start =
                player.getEyePosition();

        double reach =
                player.blockInteractionRange();

        Vec3 end =
                start.add(
                        player.getViewVector(1.0F)
                                .scale(reach)
                );

        return raycast(
                level,
                pos,
                start,
                end,
                CollisionContext.of(player)
        );
    }

    public static HitPart raycast(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end
    ) {
        return raycast(
                level,
                pos,
                start,
                end,
                CollisionContext.empty()
        );
    }

    private static HitPart raycast(
            BlockGetter level,
            BlockPos pos,
            Vec3 start,
            Vec3 end,
            CollisionContext context
    ) {
        Optional<FacadeData> facadeOptional =
                FacadeManager.getFacade(
                        level,
                        pos
                );

        if (facadeOptional.isEmpty()) {
            return HitPart.HOST;
        }

        FacadeData facade =
                facadeOptional.get();

        BlockState hostState =
                level.getBlockState(pos);

        VoxelShape hostShape =
                FacadeContext.withoutFacadeShape(
                        () -> hostState.getShape(
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

        if (hostHit == null && facadeHit == null) {
            return HitPart.NONE;
        }

        if (hostHit == null) {
            return HitPart.FACADE;
        }

        if (facadeHit == null) {
            return HitPart.HOST;
        }

        double hostDistance =
                hostHit.getLocation()
                        .distanceToSqr(start);

        double facadeDistance =
                facadeHit.getLocation()
                        .distanceToSqr(start);

        return facadeDistance <= hostDistance
                ? HitPart.FACADE
                : HitPart.HOST;
    }
}