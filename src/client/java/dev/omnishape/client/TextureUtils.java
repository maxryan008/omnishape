package dev.omnishape.client;

import dev.omnishape.Constant;
import dev.omnishape.registry.OmnishapeBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Random;

import static dev.omnishape.Omnishape.MOD_ID;

public class TextureUtils {
    public static @NotNull HashMap<Direction, TextureAtlasSprite> GetCamoSprites(@Nullable BlockState state) {
        // Get camo block-type of the frame block
        BakedModel camoModel = Minecraft.getInstance().getModelManager().getModel(
                new ModelResourceLocation(MOD_ID,"block/frame_block_default")
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
            var quads = camoModel.getQuads(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState(), dir, new Random());
            if (!quads.isEmpty()) {
                camoSprites.put(dir, quads.getFirst().getSprite());
            }
        }
        return camoSprites;
    }
}
