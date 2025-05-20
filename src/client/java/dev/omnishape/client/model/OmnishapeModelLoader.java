package dev.omnishape.client.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;

public class OmnishapeModelLoader implements net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin {
    public static final OmnishapeModelLoader INSTANCE = new OmnishapeModelLoader();

    private static final ResourceLocation FRAME_BLOCK_MODEL = ResourceLocation.fromNamespaceAndPath("omnishape", "block/frame_block_base");
    private static final ResourceLocation FRAME_ITEM_MODEL = ResourceLocation.fromNamespaceAndPath("omnishape", "item/frame_block");

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
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
