package dev.omnishape.client.mixin;

import dev.omnishape.api.OmnishapeData;
import dev.omnishape.block.FrameBlock;
import dev.omnishape.block.entity.FrameBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {

    private static final int FULL_BLOCK_PARTICLE_COUNT = 64;

    @Shadow
    protected ClientLevel level;

    @Inject(
            method = "destroy",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$scaleFrameDestroyParticles(
            BlockPos pos,
            BlockState state,
            CallbackInfo ci
    ) {
        if (!(state.getBlock() instanceof FrameBlock)) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof FrameBlockEntity frame)) {
            /*
             * If the BE has disappeared for some unusual reason, allow
             * vanilla to handle the particles rather than producing none.
             */
            return;
        }

        double volume =
                OmnishapeData.calculateVolume(
                        frame.getCorners()
                );

        /*
         * A full cube should retain the normal vanilla-style quantity:
         * 4 x 4 x 4 = 64 particles.
         *
         * Scale linearly down with volume.
         */
        int particleCount =
                Math.max(
                        1,
                        (int) Math.round(
                                FULL_BLOCK_PARTICLE_COUNT * volume
                        )
                );

        VoxelShape shape =
                state.getShape(
                        level,
                        pos
                );

        List<WeightedBox> boxes =
                collectWeightedBoxes(shape);

        if (boxes.isEmpty()) {
            ci.cancel();
            return;
        }

        double totalVolume =
                boxes.getLast().cumulativeVolume();

        if (totalVolume <= 0.0) {
            ci.cancel();
            return;
        }

        RandomSource random =
                level.getRandom();

        ParticleEngine engine =
                (ParticleEngine) (Object) this;

        for (int i = 0; i < particleCount; i++) {
            AABB box =
                    chooseWeightedBox(
                            boxes,
                            totalVolume,
                            random
                    );

            double localX =
                    randomBetween(
                            random,
                            box.minX,
                            box.maxX
                    );

            double localY =
                    randomBetween(
                            random,
                            box.minY,
                            box.maxY
                    );

            double localZ =
                    randomBetween(
                            random,
                            box.minZ,
                            box.maxZ
                    );

            /*
             * Use the particle's position relative to the centre of the
             * block as its initial velocity, similar to vanilla's destroy
             * particles.
             */
            double velocityX =
                    localX - 0.5;

            double velocityY =
                    localY - 0.5;

            double velocityZ =
                    localZ - 0.5;

            BlockState particleState = frame.getCamo();

            if (particleState.isAir()) {
                particleState = state;
            }

            TerrainParticle particle =
                    new TerrainParticle(
                            level,
                            pos.getX() + localX,
                            pos.getY() + localY,
                            pos.getZ() + localZ,
                            velocityX,
                            velocityY,
                            velocityZ,
                            particleState,
                            pos
                    );

            engine.add(particle);
        }

        /*
         * We have completely handled this FrameBlock's particle burst.
         * Do not allow vanilla to iterate every voxel box afterwards.
         */
        ci.cancel();
    }

    private static List<WeightedBox> collectWeightedBoxes(
            VoxelShape shape
    ) {
        List<WeightedBox> result =
                new ArrayList<>();

        double cumulative =
                0.0;

        for (AABB box : shape.toAabbs()) {
            double volume =
                    Math.max(
                            0.0,
                            box.getXsize()
                                    * box.getYsize()
                                    * box.getZsize()
                    );

            if (volume <= 0.0) {
                continue;
            }

            cumulative += volume;

            result.add(
                    new WeightedBox(
                            box,
                            cumulative
                    )
            );
        }

        return result;
    }

    private static AABB chooseWeightedBox(
            List<WeightedBox> boxes,
            double totalVolume,
            RandomSource random
    ) {
        double value =
                random.nextDouble()
                        * totalVolume;

        for (WeightedBox weighted : boxes) {
            if (value <= weighted.cumulativeVolume()) {
                return weighted.box();
            }
        }

        return boxes.getLast().box();
    }

    private static double randomBetween(
            RandomSource random,
            double min,
            double max
    ) {
        if (max <= min) {
            return min;
        }

        return min
                + random.nextDouble()
                * (max - min);
    }

    private record WeightedBox(
            AABB box,
            double cumulativeVolume
    ) {
    }

    @Inject(
            method = "crack",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$useCamoForHitParticles(
            BlockPos pos,
            Direction direction,
            CallbackInfo ci
    ) {
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof FrameBlock)) {
            return;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(pos);

        if (!(blockEntity instanceof FrameBlockEntity frame)) {
            return;
        }

        BlockState particleState =
                frame.getCamo();

        if (particleState.isAir()) {
            return;
        }

        VoxelShape shape =
                state.getShape(
                        level,
                        pos
                );

        List<AABB> boxes =
                shape.toAabbs();

        if (boxes.isEmpty()) {
            ci.cancel();
            return;
        }

        AABB box =
                boxes.get(
                        level.random.nextInt(
                                boxes.size()
                        )
                );

        double inset = 0.02;

        double x =
                pos.getX()
                        + randomBetween(
                        level.random,
                        box.minX + inset,
                        box.maxX - inset
                );

        double y =
                pos.getY()
                        + randomBetween(
                        level.random,
                        box.minY + inset,
                        box.maxY - inset
                );

        double z =
                pos.getZ()
                        + randomBetween(
                        level.random,
                        box.minZ + inset,
                        box.maxZ - inset
                );

        switch (direction) {
            case DOWN ->
                    y = pos.getY()
                            + box.minY
                            - inset;

            case UP ->
                    y = pos.getY()
                            + box.maxY
                            + inset;

            case NORTH ->
                    z = pos.getZ()
                            + box.minZ
                            - inset;

            case SOUTH ->
                    z = pos.getZ()
                            + box.maxZ
                            + inset;

            case WEST ->
                    x = pos.getX()
                            + box.minX
                            - inset;

            case EAST ->
                    x = pos.getX()
                            + box.maxX
                            + inset;
        }

        TerrainParticle particle =
                new TerrainParticle(
                        level,
                        x,
                        y,
                        z,
                        0.0,
                        0.0,
                        0.0,
                        particleState,
                        pos
                );

        particle.setPower(0.2F).scale(0.6F);

        ((ParticleEngine) (Object) this)
                .add(particle);

        ci.cancel();
    }
}