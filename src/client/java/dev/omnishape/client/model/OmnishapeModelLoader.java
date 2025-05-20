package dev.omnishape.client.model;

public class OmnishapeModelLoader implements net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin {
    public static final OmnishapeModelLoader INSTANCE = new OmnishapeModelLoader();

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, ctx) -> {
            if (ctx.resourceId() != null) {
                if (ctx.resourceId().getNamespace().equals("omnishape") && ctx.resourceId().getPath().equals("block/frame_block_base")) {
                    return new FrameBlockBakedModel(model);
                }
            }
            return model;
        });
    }
}
