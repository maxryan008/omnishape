package dev.omnishape.client.model;

import dev.omnishape.Constant;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

public class OmnishapeModelLoader implements ModelLoadingPlugin {
    public static final OmnishapeModelLoader INSTANCE = new OmnishapeModelLoader();

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.addModels(Constant.Model.FRAME_BLOCK);

        // Wrap block model
        pluginContext.modifyModelAfterBake().register((model, ctx) -> {
            if (ctx.resourceId() != null && ctx.resourceId().equals(Constant.Model.FRAME_BLOCK)) {
                return new FrameBlockBakedModel(model);
            }
            return model;
        });

        pluginContext.resolveModel().register(ctx -> {
            if (ctx.id() != null && ctx.id().equals(Constant.Model.FRAME_ITEM)) {
                return new FrameItemRedirectModel();
            }
            return null;
        });
    }
}
