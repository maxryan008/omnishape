package dev.omnishape.registry;

import dev.omnishape.Constant;
import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class OmnishapeBlockEntities {
    public static BlockEntityType<OmnibenchBlockEntity> OMNIBENCH;
    public static BlockEntityType<FrameBlockEntity> FRAME_BLOCK;

    public static void register() {
        OMNIBENCH = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Constant.Block.OMNIBENCH,
                BlockEntityType.Builder.of(OmnibenchBlockEntity::new, OmnishapeBlocks.OMNIBENCH).build(null)
        );

        FRAME_BLOCK = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Constant.Block.FRAME_BLOCK,
                BlockEntityType.Builder.of(FrameBlockEntity::new, OmnishapeBlocks.FRAME_BLOCK).build(null)
        );
    }
}
