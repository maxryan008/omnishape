package dev.omnishape.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.omnishape.Constant;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

import java.util.List;

import static net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE;

public class OmnishapeComponents {
    public static final Codec<Vector3f> VECTOR3F_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.FLOAT.fieldOf("x").forGetter(v -> v.x),
                    Codec.FLOAT.fieldOf("y").forGetter(v -> v.y),
                    Codec.FLOAT.fieldOf("z").forGetter(v -> v.z)
            ).apply(instance, Vector3f::new)
    );

    public static final Codec<List<Vector3f>> VECTOR3F_LIST_CODEC = Codec.list(VECTOR3F_CODEC);
    public static final DataComponentType<List<Vector3f>> CORNERS_STATE =
            DataComponentType.<List<Vector3f>>builder()
                    .persistent(VECTOR3F_LIST_CODEC)
                    .build();
    public static final ResourceLocation CAMO_STATE_ID = Constant.id("camo_state");
    public static final ResourceLocation CORNERS_STATE_ID = Constant.id("corners_state");
    public static final DataComponentType<BlockState> CAMO_STATE =
            DataComponentType.<BlockState>builder()
                    .persistent(BlockState.CODEC)
                    .build();

    public static void register() {
        Registry.register(DATA_COMPONENT_TYPE, CAMO_STATE_ID, CAMO_STATE);
        Registry.register(DATA_COMPONENT_TYPE, CORNERS_STATE_ID, CORNERS_STATE);
    }
}