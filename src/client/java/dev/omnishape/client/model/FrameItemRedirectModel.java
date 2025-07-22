package dev.omnishape.client.model;

import dev.omnishape.Constant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class FrameItemRedirectModel implements UnbakedModel {
    @Override
    public Collection<ResourceLocation> getDependencies() {
        return List.of(Constant.Model.FRAME_BLOCK);
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> function) {

    }

    @Override
    public @Nullable BakedModel bake(ModelBaker modelBaker, Function<Material, TextureAtlasSprite> function, ModelState modelState) {
        BakedModel blockModel = modelBaker.bake(Constant.Model.FRAME_BLOCK, modelState);
        return new FrameBlockBakedModel(blockModel);
    }
}
