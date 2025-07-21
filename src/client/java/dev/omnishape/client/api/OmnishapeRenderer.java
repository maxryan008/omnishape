package dev.omnishape.client.api;

import dev.omnishape.BlockRotation;
import dev.omnishape.api.OmnishapeData;
import dev.omnishape.client.TextureUtils;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import org.joml.Matrix3f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;

public class OmnishapeRenderer {
    public static void emitOverlay(OmnishapeData data, RenderContext context, BlockPos pos, BlockRotation rot, BlockAndTintGetter world) {
        Vector3f[] corners = data.corners();
        HashMap<Direction, TextureAtlasSprite> camoSprites = TextureUtils.GetCamoSprites(data.camouflage());

        for (Direction dir : Direction.values()) {
            if (camoSprites.get(dir) == null) {
                camoSprites.put(dir, Minecraft.getInstance().getBlockRenderer().getBlockModel(data.camouflage()).getQuads(data.camouflage(), dir, null).getFirst().getSprite());
            }
        }

        Matrix3f rotationMatrix = new Matrix3f()
                .rotateXYZ(
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
            faceLights[dir.ordinal()] = world.getRawBrightness(pos.relative(dir), 0);
        }

        RenderMaterial material = RendererAccess.INSTANCE.getRenderer().materialFinder().disableDiffuse(false).emissive(false).find();
        QuadEmitter emitter = context.getEmitter();

        for (Direction dir : Direction.values()) {
            TextureAtlasSprite sprite = camoSprites.get(dir);
            float minU = sprite.getU0(), maxU = sprite.getU1();
            float minV = sprite.getV0(), maxV = sprite.getV1();
            float diffU = maxU - minU, diffV = maxV - minV;

            int[] face = faces[dir.ordinal()];
            int uAxis = uvAxes[dir.ordinal()][0], vAxis = uvAxes[dir.ordinal()][1];

            Vector3f[] vs = new Vector3f[4];
            float[] us = new float[4], vs_ = new float[4];
            float minUx = Float.MAX_VALUE, maxUx = -Float.MAX_VALUE;
            float minVy = Float.MAX_VALUE, maxVy = -Float.MAX_VALUE;

            for (int i = 0; i < 4; i++) {
                vs[i] = new Vector3f(corners[face[i]]);
                float[] xyz = {vs[i].x, vs[i].y, vs[i].z};
                us[i] = xyz[uAxis];
                vs_[i] = xyz[vAxis];
                minUx = Math.min(minUx, us[i]);
                maxUx = Math.max(maxUx, us[i]);
                minVy = Math.min(minVy, vs_[i]);
                maxVy = Math.max(maxVy, vs_[i]);
            }

            for (int i = 0; i < 4; i++) {
                vs[i].x = 1.0f - vs[i].x;
                vs[i].sub(0.5f, 0.5f, 0.5f);
                rotationMatrix.transform(vs[i]);
                vs[i].add(0.5f, 0.5f, 0.5f);
            }

            float uRange = maxUx - minUx, vRange = maxVy - minVy;
            if (uRange == 0 || vRange == 0) continue;

            Vector2f[] uvs = new Vector2f[4];
            for (int i = 0; i < 4; i++) {
                float normU = 1.0f - (us[i] - minUx) / uRange;
                float normV = 1.0f - (vs_[i] - minVy) / vRange;
                uvs[i] = new Vector2f(minU + normU * diffU, minV + normV * diffV);
            }

            int light = faceLights[dir.ordinal()];
            emitter.material(material);
            emitter.nominalFace(dir);
            emitter.spriteBake(sprite, dir.get3DDataValue());

            emitter
                    .pos(0, vs[0].x, vs[0].y, vs[0].z).uv(0, uvs[0].x, uvs[0].y).lightmap(0, light).color(0, -1)
                    .pos(1, vs[1].x, vs[1].y, vs[1].z).uv(1, uvs[1].x, uvs[1].y).lightmap(1, light).color(1, -1)
                    .pos(2, vs[2].x, vs[2].y, vs[2].z).uv(2, uvs[2].x, uvs[2].y).lightmap(2, light).color(2, -1)
                    .pos(3, vs[3].x, vs[3].y, vs[3].z).uv(3, uvs[3].x, uvs[3].y).lightmap(3, light).color(3, -1)
                    .emit();
        }
    }
}