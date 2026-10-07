package dev.omnishape.client.model;

import dev.omnishape.Constant;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

public class OmnishapeModelLoader
        implements ModelLoadingPlugin {

    public static final OmnishapeModelLoader INSTANCE =
            new OmnishapeModelLoader();

    @Override
    public void onInitializeModelLoader(
            Context pluginContext
    ) {
        pluginContext.addModels(
                Constant.Model.FRAME_BLOCK
        );

        pluginContext.modifyModelAfterBake()
                .register(
                        (model, ctx) -> {
                            ResourceLocation id =
                                    ctx.resourceId();

                            if (id == null) {
                                return model;
                            }

                            /*
                             * The actual standalone Frame Block still uses
                             * its specialised model.
                             */
                            if (id.equals(
                                    Constant.Model.FRAME_BLOCK
                            )) {
                                return new FrameBlockBakedModel(
                                        model
                                );
                            }

                            /*
                             * Wrap block models globally.
                             *
                             * The wrapper is effectively free unless a
                             * facade exists at the rendered position.
                             */
                            if (id.getPath()
                                    .startsWith("block/")) {
                                return new FacadeAwareBakedModel(
                                        model
                                );
                            }

                            return model;
                        }
                );

        pluginContext.resolveModel()
                .register(
                        ctx -> {
                            if (ctx.id() != null
                                    && ctx.id().equals(
                                    Constant.Model.FRAME_ITEM
                            )) {
                                return new FrameItemRedirectModel();
                            }

                            return null;
                        }
                );
    }
}