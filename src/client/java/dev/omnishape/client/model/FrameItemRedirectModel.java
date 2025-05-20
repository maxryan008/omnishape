package dev.omnishape.client.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class FrameItemRedirectModel implements UnbakedModel {
    private static final ResourceLocation BLOCK_MODEL_ID = ResourceLocation.fromNamespaceAndPath("omnishape", "block/frame_block_base");

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(BLOCK_MODEL_ID);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {

    }

    @Override
    public @Nullable BakedModel bake(ModelBaker modelBaker, Function<Material, TextureAtlasSprite> function, ModelState modelState) {
        BakedModel blockModel = modelBaker.bake(BLOCK_MODEL_ID, modelState);
        return new FrameBlockBakedModel(blockModel);
    }
}
