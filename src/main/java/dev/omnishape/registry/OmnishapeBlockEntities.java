package dev.omnishape.registry;

import dev.omnishape.Omnishape;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class OmnishapeBlockEntities {
    public static BlockEntityType<OmnibenchBlockEntity> OMNIBENCH;

    public static void register() {
        OMNIBENCH = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "omnibench"),
                BlockEntityType.Builder.of(OmnibenchBlockEntity::new, OmnishapeBlocks.OMNIBENCH).build(null)
        );
    }
}
