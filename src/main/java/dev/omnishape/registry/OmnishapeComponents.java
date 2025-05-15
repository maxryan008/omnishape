package dev.omnishape.registry;

import com.mojang.serialization.Codec;
import dev.omnishape.Omnishape;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import static net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;

public class OmnishapeComponents {
    public static final ResourceLocation CAMO_STATE_ID = ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "camo_state");

    public static final DataComponentType<BlockState> CAMO_STATE =
            DataComponentType.<BlockState>builder()
                    .persistent(BlockState.CODEC)
                    .build();

    public static void register() {
        Registry.register(DATA_COMPONENT_TYPE, CAMO_STATE_ID, CAMO_STATE);
    }
}