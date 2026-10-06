package dev.omnishape.block;

import dev.omnishape.BlockRotation;
import dev.omnishape.BlockRotationProperty;
import dev.omnishape.api.OmnishapeData;
import dev.omnishape.block.entity.FrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;

import java.util.List;

public class FrameBlock extends Block implements EntityBlock {

    public static final BlockRotationProperty ROTATION =
            new BlockRotationProperty("rotation");

    /*
     * Vanilla block lighting is driven by BlockState, not BlockEntity data.
     *
     * The camouflage block's emitted light level is copied into this
     * property so the vanilla light engine can treat the Frame Block like
     * the block it is imitating.
     */
    public static final IntegerProperty LIGHT_LEVEL =
            IntegerProperty.create("light_level", 0, 15);

    public FrameBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(
                                ROTATION,
                                BlockRotation.IDENTITY
                        )
                        .setValue(
                                LIGHT_LEVEL,
                                0
                        )
        );
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new FrameBlockEntity(
                pos,
                state
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        if (!(level.getBlockEntity(pos) instanceof FrameBlockEntity frame)) {
            return;
        }

        if (!OmnishapeData.canExtractFromItem(stack)) {
            return;
        }

        OmnishapeData data =
                OmnishapeData.extractFromItem(stack);

        frame.setData(data);

        /*
         * Copy the camouflage block's light emission into the Frame Block's
         * own block state.
         *
         * This allows Minecraft's vanilla light engine to see the correct
         * value without needing to know anything about the block entity.
         */
        int lightLevel =
                Mth.clamp(
                        data.camouflage().getLightEmission(),
                        0,
                        15
                );

        BlockState updatedState =
                state.setValue(
                        LIGHT_LEVEL,
                        lightLevel
                );

        if (updatedState != state) {
            level.setBlock(
                    pos,
                    updatedState,
                    Block.UPDATE_ALL
            );
        }

        frame.setChanged();

        /*
         * Explicitly request a light recalculation for this position.
         *
         * setBlock() already performs the normal block update path, but
         * checkBlock() makes the intent explicit and prevents stale lighting
         * when placing a Frame Block with a different emission value.
         */
        level.getChunkSource()
                .getLightEngine()
                .checkBlock(pos);

        level.blockUpdated(
                pos,
                this
        );
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext ctx
    ) {
        return defaultBlockState()
                .setValue(
                        ROTATION,
                        BlockRotation.fromPlacementContext(ctx)
                )
                .setValue(
                        LIGHT_LEVEL,
                        0
                );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                ROTATION,
                LIGHT_LEVEL
        );
    }

    private Matrix3f getRotationMatrix(
            BlockState state
    ) {
        BlockRotation rot =
                state.getValue(ROTATION);

        return new Matrix3f().rotateXYZ(
                (float) Math.toRadians(rot.pitch),
                (float) Math.toRadians(rot.yaw),
                (float) Math.toRadians(rot.roll)
        );
    }

    private VoxelShape getFrameShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        BlockEntity be =
                level.getBlockEntity(pos);

        if (be instanceof FrameBlockEntity frame
                && frame.getData() != null) {

            return frame.getOrBuildShape(
                    getRotationMatrix(state)
            );
        }

        return Shapes.block();
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getFrameShape(
                state,
                level,
                pos
        );
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(
                state,
                level,
                pos,
                context
        );
    }

    @Override
    public VoxelShape getInteractionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return getFrameShape(
                state,
                level,
                pos
        );
    }

    @Override
    public VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getShape(
                state,
                level,
                pos,
                context
        );
    }

    @Override
    public VoxelShape getBlockSupportShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return getShape(
                state,
                level,
                pos,
                null
        );
    }

    @Override
    public VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return getShape(
                state,
                level,
                pos,
                null
        );
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    @Override
    public boolean isPathfindable(
            BlockState state,
            PathComputationType type
    ) {
        return false;
    }

    @Override
    public boolean isPossibleToRespawnInThis(
            BlockState state
    ) {
        return false;
    }

    @Override
    protected boolean isCollisionShapeFullBlock(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state
    ) {
        if (level.getBlockEntity(pos)
                instanceof FrameBlockEntity frame
                && frame.getData() != null) {

            ItemStack stack =
                    new ItemStack(this);

            OmnishapeData.writeToItem(
                    stack,
                    frame.getData()
            );

            return stack;
        }

        return super.getCloneItemStack(
                level,
                pos,
                state
        );
    }

    @Override
    protected List<ItemStack> getDrops(
            BlockState state,
            LootParams.Builder builder
    ) {
        BlockEntity be =
                builder.getOptionalParameter(
                        LootContextParams.BLOCK_ENTITY
                );

        if (be instanceof FrameBlockEntity frame
                && frame.getData() != null) {

            ItemStack stack =
                    new ItemStack(this);

            OmnishapeData.writeToItem(
                    stack,
                    frame.getData()
            );

            return List.of(stack);
        }

        return super.getDrops(
                state,
                builder
        );
    }

    @Override
    protected float getDestroyProgress(
            BlockState blockState,
            Player player,
            BlockGetter blockGetter,
            BlockPos blockPos
    ) {
        BlockEntity blockEntity =
                blockGetter.getBlockEntity(blockPos);

        if (blockEntity instanceof FrameBlockEntity frame
                && frame.getCamo().getBlock() != Blocks.AIR) {

            return frame.getCamo().getDestroyProgress(
                    player,
                    blockGetter,
                    blockPos
            );
        }

        return super.getDestroyProgress(
                blockState,
                player,
                blockGetter,
                blockPos
        );
    }
}