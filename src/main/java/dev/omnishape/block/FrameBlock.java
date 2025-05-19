package dev.omnishape.block;

import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.registry.OmnishapeComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class FrameBlock extends Block implements EntityBlock {
    public FrameBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof FrameBlockEntity be)) return;

        if (stack.has(OmnishapeComponents.CAMO_STATE)) {
            BlockState camo = stack.get(OmnishapeComponents.CAMO_STATE);
            if (camo != null) {
                be.setCamo(camo);
            }
        }

        if (stack.has(OmnishapeComponents.CORNERS_STATE)) {
            List<Vector3f> corners = stack.get(OmnishapeComponents.CORNERS_STATE);
            if (!corners.isEmpty()) {
                be.setCorners(corners);
            }
        }

        be.setChanged();
        level.updateNeighborsAt(pos, this);
        level.blockUpdated(pos, this);
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        super.onPlace(blockState, level, blockPos, blockState2, bl);
    }

    @Override
    public float getExplosionResistance() {
        return 2000F; // Needed only if you override explosion behavior
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrameBlockEntity(pos, state);
    }
}
