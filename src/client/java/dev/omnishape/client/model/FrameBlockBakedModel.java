package dev.omnishape.client.model;

import com.mojang.math.Matrix3f;
import com.mojang.math.Vector3f;
import dev.omnishape.BlockRotation;
import dev.omnishape.Vector2f;
import dev.omnishape.block.FrameBlock;
import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.client.TextureUtils;
import dev.omnishape.registry.OmnishapeBlocks;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class FrameBlockBakedModel extends ForwardingBakedModel {
    private final FrameOverride itemOverride;

    public FrameBlockBakedModel(BakedModel wrapped) {
        this.wrapped = wrapped;
        this.itemOverride = new FrameOverride();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context) {
        // Get the block entity data of the frame block.
        BlockEntity be = blockView.getBlockEntity(pos);
        if (!(be instanceof FrameBlockEntity frame)) {
            if (this.wrapped instanceof FabricBakedModel fabricModel) {
                fabricModel.emitBlockQuads(blockView, state, pos, randomSupplier, context);
            }
            return;
        }

        // Get frame block corner data
        Vector3f[] corners = frame.getCorners();

        HashMap<Direction, TextureAtlasSprite> camoSprites = TextureUtils.GetCamoSprites(frame.getCamo());

        for (Direction dir : Direction.values()) {
            if (camoSprites.get(dir) == null) {
                List<BakedQuad> quads = Minecraft.getInstance()
                        .getBlockRenderer()
                        .getBlockModel(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState())
                        .getQuads(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState(), dir, new Random());

                if (!quads.isEmpty()) {
                    camoSprites.put(dir, quads.get(0).getSprite());
                }
            }
        }

        BlockRotation rot = state.getValue(FrameBlock.ROTATION);

        Matrix3f rotationMatrix = Matrix3f.createScaleMatrix(
                (float) Math.toRadians(rot.pitch),
                (float) Math.toRadians(rot.yaw),
                (float) Math.toRadians(rot.roll)
        );

        int[][] faces = {
                {4, 5, 1, 0}, // TOP (Y+)
                {2, 3, 7, 6}, // BOTTOM (Y-)
                {1, 3, 2, 0}, // NORTH (Z-)
                {6, 7, 5, 4}, // SOUTH (Z+)
                {0, 2, 6, 4}, // WEST (X-)
                {5, 7, 3, 1}  // EAST (X+)
        };

        int[][] uvAxes = {
                {0, 2}, {0, 2}, {0, 1}, {0, 1},  {2, 1}, {2, 1}
        };

        int[] faceLights = new int[6];
        for (Direction dir : Direction.values()) {
            faceLights[dir.ordinal()] = blockView.getRawBrightness(pos.relative(dir), 0);
        }

        for (Direction dir : Direction.values()) {
            float minU = camoSprites.get(dir).getU0(), maxU = camoSprites.get(dir).getU1();
            float minV = camoSprites.get(dir).getV0(), maxV = camoSprites.get(dir).getV1();
            float diffU = maxU - minU, diffV = maxV - minV;

            int[] face = faces[dir.ordinal()];
            int uAxis = uvAxes[dir.ordinal()][0], vAxis = uvAxes[dir.ordinal()][1];

            Vector3f[] vs = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                vs[i] = corners[face[i]];
            }

            float[] us = new float[4], vs_ = new float[4];
            float minUx = Float.MAX_VALUE, maxUx = -Float.MAX_VALUE;
            float minVy = Float.MAX_VALUE, maxVy = -Float.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                float[] xyz = {vs[i].x(), vs[i].y(), vs[i].z()};
                us[i] = xyz[uAxis];
                vs_[i] = xyz[vAxis];
                minUx = Math.min(minUx, us[i]);
                maxUx = Math.max(maxUx, us[i]);
                minVy = Math.min(minVy, vs_[i]);
                maxVy = Math.max(maxVy, vs_[i]);
            }

            for (int i = 0; i < 4; i++) {
                vs[i].set(1.0f - vs[i].x(), vs[i].y(), vs[i].z());
            }

            for (int i = 0; i < 4; i++) {
                vs[i].sub(new Vector3f(0.5f, 0.5f, 0.5f));        // move to origin
                vs[i].transform(rotationMatrix);     // apply rotation
                vs[i].add(0.5f, 0.5f, 0.5f);        // move back to center
            }

            float uRange = maxUx - minUx, vRange = maxVy - minVy;
            if (uRange == 0 || vRange == 0) continue;


            Vector2f[] uvs = new Vector2f[4];
            for (int i = 0; i < 4; i++) {
                float normU = (us[i] - minUx) / uRange;
                float normV = (vs_[i] - minVy) / vRange;

                normU = 1.0f - normU;
                normV = 1.0f - normV;

                uvs[i] = new Vector2f(minU + normU * diffU, minV + normV * diffV);
            }

            int brightness = faceLights[Direction.from3DDataValue(dir.ordinal()).ordinal()];

            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            RenderMaterial material = renderer.materialFinder()
                    .disableDiffuse(0, false)  // true disables AO
                    .emissive(0, false)
                    .find();

            QuadEmitter emitter = context.getEmitter();
            emitter.material(material);
            emitter.nominalFace(dir); // used for AO and lighting
            emitter.spriteBake(0, camoSprites.get(dir), QuadEmitter.BAKE_LOCK_UV);
            // Position and UV are floats, convert using Float.intBitsToFloat(...)
            emitter
                    .pos(0, vs[0].x(), vs[0].y(), vs[0].z()).sprite(0, 0, uvs[0].x, uvs[0].y).lightmap(0, brightness).spriteColor(0, 0, -1)
                    .pos(1, vs[1].x(), vs[1].y(), vs[1].z()).sprite(1, 0, uvs[1].x, uvs[1].y).lightmap(1, brightness).spriteColor(1, 0, -1)
                    .pos(2, vs[2].x(), vs[2].y(), vs[2].z()).sprite(2, 0, uvs[2].x, uvs[2].y).lightmap(2, brightness).spriteColor(2, 0, -1)
                    .pos(3, vs[3].x(), vs[3].y(), vs[3].z()).sprite(3, 0, uvs[3].x, uvs[3].y).lightmap(3, brightness).spriteColor(3, 0, -1)
                    .emit();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        // Fallback cube if no data
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f((i & 1), (i >> 1 & 1), (i >> 2 & 1));
        }

        BakedModel camoModel = Minecraft.getInstance().getModelManager().getModel(
                new ModelResourceLocation("omnishape", "block/frame_block_default")
        );

        // Load NBT data
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("CornersState", Tag.TAG_LIST)) {
                ListTag cornerList = tag.getList("CornersState", Tag.TAG_COMPOUND);
                if (cornerList.size() == 8) {
                    for (int i = 0; i < 8; i++) {
                        CompoundTag vecTag = cornerList.getCompound(i);
                        corners[i].set(vecTag.getFloat("x"), vecTag.getFloat("y"), vecTag.getFloat("z"));
                    }
                }
            }

            if (tag.contains("CamoState", Tag.TAG_COMPOUND)) {
                BlockState camoState = NbtUtils.readBlockState(tag.getCompound("CamoState"));
                if (camoState != null && !camoState.isAir()) {
                    camoModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(camoState);
                }
            }
        }

        HashMap<Direction, TextureAtlasSprite> camoSprites = TextureUtils.AssembleHashmap(camoModel);

        for (Direction dir : Direction.values()) {
            if (camoSprites.get(dir) == null) {
                List<BakedQuad> quads = Minecraft.getInstance()
                        .getBlockRenderer()
                        .getBlockModel(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState())
                        .getQuads(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState(), dir, new Random());

                if (!quads.isEmpty()) {
                    camoSprites.put(dir, quads.get(0).getSprite());
                }
            }
        }

        int[][] faces = {
                {4, 5, 1, 0}, // TOP (Y+)
                {2, 3, 7, 6}, // BOTTOM (Y-)
                {1, 3, 2, 0}, // NORTH (Z-)
                {6, 7, 5, 4}, // SOUTH (Z+)
                {0, 2, 6, 4}, // WEST (X-)
                {5, 7, 3, 1}  // EAST (X+)
        };

        int[][] uvAxes = {
                {0, 2}, {0, 2}, {0, 1}, {0, 1}, {2, 1}, {2, 1}
        };

        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        RenderMaterial material = renderer.materialFinder().disableDiffuse(0, false).emissive(0, false).find();

        for (Direction dir : Direction.values()) {
            float minU = camoSprites.get(dir).getU0(), maxU = camoSprites.get(dir).getU1();
            float minV = camoSprites.get(dir).getV0(), maxV = camoSprites.get(dir).getV1();
            float diffU = maxU - minU, diffV = maxV - minV;

            int[] face = faces[dir.ordinal()];
            int uAxis = uvAxes[dir.ordinal()][0], vAxis = uvAxes[dir.ordinal()][1];

            Vector3f[] vs = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                vs[i] = corners[face[i]];
            }

            for (int i = 0; i < 4; i++) {
                vs[i].set(1.0f - vs[i].x(), vs[i].y(), vs[i].z());
            }

            float[] us = new float[4], vs_ = new float[4];
            float minUx = Float.MAX_VALUE, maxUx = -Float.MAX_VALUE;
            float minVy = Float.MAX_VALUE, maxVy = -Float.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                float[] xyz = {vs[i].x(), vs[i].y(), vs[i].z()};
                us[i] = xyz[uAxis];
                vs_[i] = xyz[vAxis];
                minUx = Math.min(minUx, us[i]);
                maxUx = Math.max(maxUx, us[i]);
                minVy = Math.min(minVy, vs_[i]);
                maxVy = Math.max(maxVy, vs_[i]);
            }

            float uRange = maxUx - minUx, vRange = maxVy - minVy;
            if (uRange == 0 || vRange == 0) continue;

            Vector2f[] uvs = new Vector2f[4];
            for (int i = 0; i < 4; i++) {
                float normU = (us[i] - minUx) / uRange;
                float normV = (vs_[i] - minVy) / vRange;
                normU = 1.0f - normU;
                normV = 1.0f - normV;
                uvs[i] = new Vector2f(minU + normU * diffU, minV + normV * diffV);
            }

            QuadEmitter emitter = context.getEmitter();
            emitter.material(material);
            emitter.cullFace(null);
            emitter.nominalFace(dir);
            emitter.spriteBake(0, camoSprites.get(dir), QuadEmitter.BAKE_LOCK_UV);

            emitter
                    .pos(0, vs[0].x(), vs[0].y(), vs[0].z()).sprite(0, 0, uvs[0].x, uvs[0].y).spriteColor(0, 0, -1)
                    .pos(1, vs[1].x(), vs[1].y(), vs[1].z()).sprite(1, 0, uvs[1].x, uvs[1].y).spriteColor(1, 0, -1)
                    .pos(2, vs[2].x(), vs[2].y(), vs[2].z()).sprite(2, 0, uvs[2].x, uvs[2].y).spriteColor(2, 0, -1)
                    .pos(3, vs[3].x(), vs[3].y(), vs[3].z()).sprite(3, 0, uvs[3].x, uvs[3].y).spriteColor(3, 0, -1)
                    .emit();
        }
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return super.useAmbientOcclusion();
    }

    @Override
    public boolean usesBlockLight() {
        return super.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return super.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return super.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return super.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return itemOverride;
    }

    public class FrameOverride extends ItemOverrides {
        public FrameOverride(ModelBakery modelBakery, BlockModel blockModel, Function<ResourceLocation, UnbakedModel> function, List<ItemOverride> list) {
            super(modelBakery, blockModel, function, list);
        }

        @Override
        public @Nullable BakedModel resolve(BakedModel bakedModel, ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int i) {
            return FrameBlockBakedModel.this;
        }
    }
}
