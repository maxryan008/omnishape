package dev.omnishape.block;

import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.registry.OmnishapeComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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
