package dev.omnishape.client.model;

import dev.omnishape.block.entity.FrameBlockEntity;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.material.MaterialFinderImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.function.Supplier;

public class FrameBlockBakedModel extends ForwardingBakedModel {
    private final SpriteFinder spriteFinder = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS));

    public FrameBlockBakedModel(BakedModel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter world, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof FrameBlockEntity frame)) {
            this.wrapped.emitBlockQuads(world, state, pos, randomSupplier, context);
            return;
        }

        Vector3f[] corners = frame.getCorners();
        BlockState camo = frame.getCamo();
        if (camo.isAir()) {
            camo = Blocks.IRON_BLOCK.defaultBlockState();
        }
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(camo);
        TextureAtlasSprite sprite = null;
        for (Direction dir : Direction.values()) {
            var quads = model.getQuads(camo, dir, RandomSource.create());
            if (!quads.isEmpty()) {
                sprite = quads.get(0).getSprite();
                break;
            }
        }
        if (sprite == null) {
            sprite = Minecraft.getInstance()
                    .getModelManager()
                    .getMissingModel()
                    .getParticleIcon();
        }

        int[][] faces = {
                {2, 3, 1, 0}, {4, 5, 7, 6},
                {0, 1, 5, 4}, {6, 7, 3, 2},
                {4, 6, 2, 0}, {1, 3, 7, 5}
        };

        int[][] uvAxes = {
                {0, 1}, {0, 1}, {0, 2}, {0, 2}, {2, 1}, {2, 1}
        };

        float minU = sprite.getU0(), maxU = sprite.getU1();
        float minV = sprite.getV0(), maxV = sprite.getV1();
        float diffU = maxU - minU, diffV = maxV - minV;

        int[] faceLights = new int[6];
        for (Direction dir : Direction.values()) {
            faceLights[dir.ordinal()] = world.getRawBrightness(pos.relative(dir), 0);
        }

        for (int f = 0; f < 6; f++) {
            int[] face = faces[f];
            int uAxis = uvAxes[f][0], vAxis = uvAxes[f][1];

            Vector3f[] vs = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                vs[i] = new Vector3f(corners[face[i]]);
            }

            float[] us = new float[4], vs_ = new float[4];
            float minUx = Float.MAX_VALUE, maxUx = -Float.MAX_VALUE;
            float minVy = Float.MAX_VALUE, maxVy = -Float.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                float[] xyz = {vs[i].x, vs[i].y, vs[i].z};
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
                uvs[i] = new Vector2f(minU + normU * diffU, minV + normV * diffV);
            }

            int brightness = faceLights[Direction.from3DDataValue(f).ordinal()];

            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            RenderMaterial material = renderer.materialFinder()
                    .disableDiffuse(false)  // true disables AO
                    .emissive(false)
                    .find();

            QuadEmitter emitter = context.getEmitter();
            emitter.material(material);
            emitter.cullFace(Direction.UP); // only if needed
            emitter.nominalFace(Direction.UP); // used for AO and lighting
            emitter.spriteBake(sprite, Direction.UP.get3DDataValue()); // for UV interpolation

            // Position and UV are floats, convert using Float.intBitsToFloat(...)
            emitter
                    .pos(0, vs[0].x, vs[0].y, vs[0].z).uv(0, uvs[0].x, uvs[0].y).lightmap(0, brightness).color(0, -1)
                    .pos(1, vs[1].x, vs[1].y, vs[1].z).uv(1, uvs[1].x, uvs[1].y).lightmap(1, brightness).color(1, -1)
                    .pos(2, vs[2].x, vs[2].y, vs[2].z).uv(2, uvs[2].x, uvs[2].y).lightmap(2, brightness).color(2, -1)
                    .pos(3, vs[3].x, vs[3].y, vs[3].z).uv(3, uvs[3].x, uvs[3].y).lightmap(3, brightness).color(3, -1)
                    .emit();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
        //todo
        super.emitItemQuads(stack, randomSupplier, context);
    }
}
