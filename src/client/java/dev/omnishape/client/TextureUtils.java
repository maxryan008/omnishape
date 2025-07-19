package dev.omnishape.client;

import dev.omnishape.registry.OmnishapeBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class TextureUtils {
    public static @NotNull HashMap<Direction, TextureAtlasSprite> GetCamoSprites(@Nullable BlockState state) {
        // Get camo block-type of the frame block
        BakedModel camoModel = Minecraft.getInstance().getModelManager().getModel(
                ResourceLocation.fromNamespaceAndPath("omnishape", "block/frame_block_default")
        );

        // If frame camo block exists (Not default frame block represented as AIR)
        if (state == null) {
            camoModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState());
        } else {
            if (state.getBlock() != Blocks.AIR) {
                if (!state.isAir()) {
                    // get the model of the camo block
                    camoModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                }
            }
        }

        return AssembleHashmap(camoModel);
    }

    public static HashMap<Direction, TextureAtlasSprite> AssembleHashmap(BakedModel camoModel) {
        HashMap<Direction, TextureAtlasSprite> camoSprites = new HashMap<>();
        for (Direction dir : Direction.values()) {
            var quads = camoModel.getQuads(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState(), dir, RandomSource.create());
            if (!quads.isEmpty()) {
                camoSprites.put(dir, quads.getFirst().getSprite());
            }
        }
        return camoSprites;
    }
}
