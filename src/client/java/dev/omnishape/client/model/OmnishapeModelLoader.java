package dev.omnishape.client.model;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class OmnishapeModelLoader {
    public static final ResourceLocation FRAME_BLOCK_MODEL = new ResourceLocation("omnishape", "block/frame_block_base");
    public static final ResourceLocation FRAME_ITEM_MODEL = new ResourceLocation("omnishape", "item/frame_block");

    public static void register() {
        // Ensure required base models are loaded
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> {
            out.accept(FRAME_BLOCK_MODEL);
            out.accept(new ResourceLocation("omnishape", "block/frame_block_default"));
        });

        // Bake-time model wrapping
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(resourceManager -> (id, context) -> {
            if (id.equals(FRAME_BLOCK_MODEL)) {
                return new UnbakedModel() {
                    @Override
                    public java.util.Collection<ResourceLocation> getDependencies() {
                        return java.util.List.of(FRAME_BLOCK_MODEL);
                    }

                    @Override
                    public Collection<Material> getMaterials(Function<ResourceLocation, UnbakedModel> function, Set<Pair<String, String>> set) {
                        return List.of();
                    }

                    @Override
                    public @Nullable BakedModel bake(ModelBakery modelBakery, Function<Material, TextureAtlasSprite> function, ModelState modelState, ResourceLocation resourceLocation) {
                        BakedModel base = modelBakery.bake(FRAME_BLOCK_MODEL, modelState);
                        return new FrameBlockBakedModel(base);
                    }
                };
            }
            return null;
        });

        // Provide a redirected item model
        ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> {
            out.accept(FRAME_ITEM_MODEL);
        });

        ModelLoadingRegistry.INSTANCE.registerVariantProvider(resourceManager -> (id, ctx) -> {
            if (id.equals(FRAME_ITEM_MODEL)) {
                return new FrameItemRedirectModel();  // must implement UnbakedModel (you already do)
            }
            return null;
        });
    }
}