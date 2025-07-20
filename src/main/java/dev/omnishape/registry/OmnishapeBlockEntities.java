package dev.omnishape.registry;

import dev.omnishape.Constant;
import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class OmnishapeBlockEntities {
    public static BlockEntityType<OmnibenchBlockEntity> OMNIBENCH;
    public static BlockEntityType<FrameBlockEntity> FRAME_BLOCK;

    public static void register() {
        OMNIBENCH = register(
                "omnibench",
                FabricBlockEntityTypeBuilder.create(OmnibenchBlockEntity::new, OmnishapeBlocks.OMNIBENCH).build()
        );

        FRAME_BLOCK = register(
                "frame_block",
                FabricBlockEntityTypeBuilder.create(FrameBlockEntity::new, OmnishapeBlocks.FRAME_BLOCK).build()
        );
    }

    public static <T extends BlockEntityType<?>> T register(String path, T blockEntityType) {
        return Registry.register(Registry.BLOCK_ENTITY_TYPE, Constant.id(path), blockEntityType);
    }
}
