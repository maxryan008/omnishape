    package dev.omnishape.block;

    import dev.omnishape.BlockRotation;
    import dev.omnishape.BlockRotationProperty;
    import dev.omnishape.api.OmnishapeData;
    import dev.omnishape.block.entity.FrameBlockEntity;
    import net.minecraft.core.BlockPos;
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
        public static final BlockRotationProperty ROTATION = new BlockRotationProperty("rotation");

        public FrameBlock(Properties properties) {
            super(properties);
            this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, BlockRotation.IDENTITY));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new FrameBlockEntity(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            if (level.getBlockEntity(pos) instanceof FrameBlockEntity be) {
                if (OmnishapeData.canExtractFromItem(stack)) {
                    be.setData(OmnishapeData.extractFromItem(stack));
                    be.setChanged();
                    level.blockUpdated(pos, this);
                }
            }
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(ROTATION, BlockRotation.fromPlacementContext(ctx));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ROTATION);
        }

        private Matrix3f getRotationMatrix(BlockState state) {
            BlockRotation rot = state.getValue(ROTATION);
            return new Matrix3f().rotateXYZ(
                    (float) Math.toRadians(rot.pitch),
                    (float) Math.toRadians(rot.yaw),
                    (float) Math.toRadians(rot.roll)
            );
        }

        private VoxelShape getFrameShape(BlockState state, BlockGetter level, BlockPos pos) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FrameBlockEntity frame && frame.getData() != null) {
                return frame.getOrBuildShape(getRotationMatrix(state));
            }
            return Shapes.block();
        }

        @Override public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return getFrameShape(s, l, p); }
        @Override public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return getShape(s, l, p, c); }
        @Override public VoxelShape getInteractionShape(BlockState s, BlockGetter l, BlockPos p) { return getFrameShape(s, l, p); }
        @Override public VoxelShape getVisualShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return getShape(s, l, p, c); }
        @Override public VoxelShape getBlockSupportShape(BlockState s, BlockGetter l, BlockPos p) { return getShape(s, l, p, null); }
        @Override public VoxelShape getOcclusionShape(BlockState s, BlockGetter l, BlockPos p) { return getShape(s, l, p, null); }

        @Override public boolean hasDynamicShape() { return true; }
        @Override public boolean isPathfindable(BlockState s, PathComputationType t) { return false; }
        @Override public boolean isPossibleToRespawnInThis(BlockState s) { return false; }
        @Override protected boolean isCollisionShapeFullBlock(BlockState s, BlockGetter l, BlockPos p) { return false; }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            if (level.getBlockEntity(pos) instanceof FrameBlockEntity frame && frame.getData() != null) {
                ItemStack stack = new ItemStack(this);
                OmnishapeData.writeToItem(stack, frame.getData());
                return stack;
            }
            return super.getCloneItemStack(level, pos, state);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
            BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
            if (be instanceof FrameBlockEntity frame && frame.getData() != null) {
                ItemStack stack = new ItemStack(this);
                OmnishapeData.writeToItem(stack, frame.getData());
                return List.of(stack);
            }
            return super.getDrops(state, builder);
        }

        @Override
        protected float getDestroyProgress(BlockState blockState, Player player, BlockGetter blockGetter, BlockPos blockPos) {
            BlockEntity blockEntity = blockGetter.getBlockEntity(blockPos);
            if (blockEntity instanceof FrameBlockEntity frame && frame.getCamo().getBlock() != Blocks.AIR) {
                return frame.getCamo().getDestroyProgress(player, blockGetter, blockPos);
            }
            return super.getDestroyProgress(blockState, player, blockGetter, blockPos);
        }
    }
