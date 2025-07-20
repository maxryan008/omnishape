package dev.omnishape.block;

import com.mojang.serialization.MapCodec;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.menu.OmnibenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class OmnibenchBlock extends BaseEntityBlock {
    public OmnibenchBlock() {
        super(BlockBehaviour.Properties.of(Material.METAL).color(MaterialColor.COLOR_LIGHT_GRAY).strength(2.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof OmnibenchBlockEntity omnibench) {
                player.openMenu(omnibench);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OmnibenchBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!blockState.is(blockState2.getBlock())) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be instanceof OmnibenchBlockEntity benchEntity) {
                if (benchEntity.hasMenu()) {
                    OmnibenchMenu menu = benchEntity.getMenu();
                    Containers.dropContents(level, blockPos, menu.getContainer());
                    level.updateNeighbourForOutputSignal(blockPos, this);
                }
            }
            super.onRemove(blockState, level, blockPos, blockState2, bl);
        }
    }
}
