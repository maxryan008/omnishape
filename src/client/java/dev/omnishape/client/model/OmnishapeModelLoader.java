package dev.omnishape.client.model;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

public class OmnishapeModelLoader implements ModelLoadingPlugin {
    public static final OmnishapeModelLoader INSTANCE = new OmnishapeModelLoader();

    public static final ResourceLocation FRAME_BLOCK_MODEL = ResourceLocation.fromNamespaceAndPath("omnishape", "block/frame_block_base");
    private static final ResourceLocation FRAME_ITEM_MODEL = ResourceLocation.fromNamespaceAndPath("omnishape", "item/frame_block");

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.addModels(ResourceLocation.fromNamespaceAndPath("omnishape", "block/frame_block_default"));

        // Wrap block model
        pluginContext.modifyModelAfterBake().register((model, ctx) -> {
            if (ctx.resourceId() != null && ctx.resourceId().equals(FRAME_BLOCK_MODEL)) {
                return new FrameBlockBakedModel(model);
            }
            return model;
        });

        pluginContext.resolveModel().register(ctx -> {
            if (ctx.id() != null && ctx.id().equals(FRAME_ITEM_MODEL)) {
                return new FrameItemRedirectModel();
            }
            return null;
        });
    }
}
