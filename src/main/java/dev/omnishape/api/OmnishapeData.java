package dev.omnishape.api;

import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;

public record OmnishapeData(BlockState camouflage, Vector3f[] corners) {
    public static final int CORNER_COUNT = 8;

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("Camo", BlockState.CODEC.encodeStart(NbtOps.INSTANCE, this.camouflage())
                .result().orElseThrow(() -> new IllegalStateException("Failed to encode blockstate")));
        var cornerList = new net.minecraft.nbt.ListTag();
        for (Vector3f vec : corners) {
            var vecTag = new CompoundTag();
            vecTag.putFloat("x", vec.x);
            vecTag.putFloat("y", vec.y);
            vecTag.putFloat("z", vec.z);
            cornerList.add(vecTag);
        }
        tag.put("Corners", cornerList);
        return tag;
    }

    public static OmnishapeData fromNbt(CompoundTag tag) {
        BlockState camo = BlockState.CODEC.parse(NbtOps.INSTANCE, tag.get("Camo"))
                .result().orElseThrow(() -> new IllegalStateException("Failed to decode blockstate"));

        var cornerList = tag.getList("Corners", CompoundTag.TAG_COMPOUND);
        Vector3f[] corners = new Vector3f[CORNER_COUNT];
        for (int i = 0; i < CORNER_COUNT; i++) {
            if (i < cornerList.size()) {
                var vec = (CompoundTag) cornerList.get(i);
                corners[i] = new Vector3f(vec.getFloat("x"), vec.getFloat("y"), vec.getFloat("z"));
            } else {
                corners[i] = new Vector3f(); // fallback
            }
        }

        return new OmnishapeData(camo, corners);
    }

    public static boolean canExtractFromItem(ItemStack stack) {
        return stack.has(OmnishapeComponents.CAMO_STATE) && stack.has(OmnishapeComponents.CORNERS_STATE);
    }

    public static OmnishapeData extractFromItem(ItemStack stack) {
        BlockState camo = stack.get(OmnishapeComponents.CAMO_STATE);
        List<Vector3f> cornerList = stack.get(OmnishapeComponents.CORNERS_STATE);

        Vector3f[] corners = new Vector3f[CORNER_COUNT];
        for (int i = 0; i < CORNER_COUNT; i++) {
            if (i < cornerList.size()) {
                corners[i] = cornerList.get(i);
            } else {
                corners[i] = new Vector3f(); // fallback
            }
        }

        return new OmnishapeData(camo, corners);
    }

    public static void writeToItem(ItemStack stack, OmnishapeData data) {
        stack.set(OmnishapeComponents.CAMO_STATE, data.camouflage);
        stack.set(OmnishapeComponents.CORNERS_STATE, List.of(data.corners)); // convert array to list
    }

    public ItemStack createItem() {
        ItemStack stack = new ItemStack(OmnishapeBlocks.FRAME_BLOCK.asItem());
        writeToItem(stack, this);
        return stack;
    }

    public static final int RESOLUTION = 8;
    private static final int[][] FACE_INDICES = {
            {0, 1, 3, 2}, {6, 7, 5, 4}, {4, 5, 1, 0},
            {2, 3, 7, 6}, {0, 2, 6, 4}, {5, 7, 3, 1}
    };

    public VoxelShape generateShape(Matrix3f rotation) {
        return generateVoxelShape(corners, rotation);
    }

    public static VoxelShape generateVoxelShape(Vector3f[] corners, Matrix3f rotationMatrix) {
        Vector3f[] rotated = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            rotated[i] = new Vector3f(corners[i]);
            rotated[i].x = 1.0f - rotated[i].x;
            rotated[i].sub(0.5f, 0.5f, 0.5f);
            rotationMatrix.transform(rotated[i]);
            rotated[i].add(0.5f, 0.5f, 0.5f);
        }

        boolean[][][] voxels = new boolean[RESOLUTION][RESOLUTION][RESOLUTION];
        rasterizeMask(rotated, voxels);
        return buildVoxelShape(voxels, RESOLUTION);
    }

    private static void rasterizeMask(Vector3f[] corners, boolean[][][] mask) {
        for (int[] face : FACE_INDICES) {
            Vector3f v0 = corners[face[0]];
            Vector3f v1 = corners[face[1]];
            Vector3f v2 = corners[face[2]];
            Vector3f v3 = corners[face[3]];

            // Tri 1
            rasterizeTriangleByIntersection(mask, v0, v1, v2);
            // Tri 2
            rasterizeTriangleByIntersection(mask, v0, v2, v3);
        }
    }

    private static void shrinkTowardsCenter(Vector3f v, float amount) {
        if (v.x > 0.5f) v.x -= amount;
        else if (v.x < 0.5f) v.x += amount;

        if (v.y > 0.5f) v.y -= amount;
        else if (v.y < 0.5f) v.y += amount;

        if (v.z > 0.5f) v.z -= amount;
        else if (v.z < 0.5f) v.z += amount;
    }

    private static void rasterizeTriangleByIntersection(boolean[][][] voxels, Vector3f v0, Vector3f v1, Vector3f v2) {
        int res = voxels.length;
        float unit = 1f / res;

        // Clone to avoid mutating the original corners
        Vector3f p0 = new Vector3f(v0);
        Vector3f p1 = new Vector3f(v1);
        Vector3f p2 = new Vector3f(v2);

        shrinkTowardsCenter(p0, 0.01f);
        shrinkTowardsCenter(p1, 0.01f);
        shrinkTowardsCenter(p2, 0.01f); // Shrink to avoid max-edge voxel skipping

        AABB bounds = new AABB(
                Math.min(p0.x, Math.min(p1.x, p2.x)),
                Math.min(p0.y, Math.min(p1.y, p2.y)),
                Math.min(p0.z, Math.min(p1.z, p2.z)),
                Math.max(p0.x, Math.max(p1.x, p2.x)),
                Math.max(p0.y, Math.max(p1.y, p2.y)),
                Math.max(p0.z, Math.max(p1.z, p2.z))
        );

        int minX = Math.max(0, (int)(bounds.minX * res));
        int maxX = Math.min(res - 1, (int)(bounds.maxX * res));
        int minY = Math.max(0, (int)(bounds.minY * res));
        int maxY = Math.min(res - 1, (int)(bounds.maxY * res));
        int minZ = Math.max(0, (int)(bounds.minZ * res));
        int maxZ = Math.min(res - 1, (int)(bounds.maxZ * res));

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    float vx = x * unit;
                    float vy = y * unit;
                    float vz = z * unit;
                    if (triangleIntersectsVoxel(p0, p1, p2, vx, vy, vz, unit)) {
                        voxels[x][y][z] = true;
                    }
                }
            }
        }
    }

    private static VoxelShape buildVoxelShape(boolean[][][] voxels, int res) {
        VoxelShape shape = Shapes.empty();
        float unit = 1f / res;
        boolean found = false;

        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                for (int z = 0; z < res; z++) {
                    if (voxels[x][y][z]) {
                        found = true;
                        shape = Shapes.or(shape, Shapes.box(
                                x * unit, y * unit, z * unit,
                                (x + 1) * unit, (y + 1) * unit, (z + 1) * unit
                        ));
                    }
                }
            }
        }

        // Ensure at least one voxel is present for visibility
        if (!found) {
            shape = Shapes.box(0, 0, 0, unit, unit, unit);
        }

        return shape.optimize();
    }

    private static boolean triangleIntersectsVoxel(Vector3f v0, Vector3f v1, Vector3f v2, float vx, float vy, float vz, float size) {
        AABB voxelBox = new AABB(vx, vy, vz, vx + size, vy + size, vz + size);
        return intersectsTriangleAABB(v0, v1, v2, voxelBox);
    }

    private static boolean intersectsTriangleAABB(Vector3f v0, Vector3f v1, Vector3f v2, AABB box) {
        // Translate triangle so AABB is at origin
        float bx = (float) box.minX;
        float by = (float) box.minY;
        float bz = (float) box.minZ;
        float ex = (float) (box.maxX - box.minX);
        float ey = (float) (box.maxY - box.minY);
        float ez = (float) (box.maxZ - box.minZ);

        Vector3f c = new Vector3f(bx + ex / 2, by + ey / 2, bz + ez / 2);
        Vector3f h = new Vector3f(ex / 2, ey / 2, ez / 2);

        Vector3f tv0 = new Vector3f(v0).sub(c);
        Vector3f tv1 = new Vector3f(v1).sub(c);
        Vector3f tv2 = new Vector3f(v2).sub(c);

        // AABB-triangle SAT test
        return satTriangleBox(tv0, tv1, tv2, h);
    }

    private static boolean satTriangleBox(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f halfSize) {
        // Triangle edges
        Vector3f e0 = new Vector3f(v1).sub(v0);
        Vector3f e1 = new Vector3f(v2).sub(v1);
        Vector3f e2 = new Vector3f(v0).sub(v2);

        // 13 axis tests
        Vector3f[] tests = new Vector3f[] {
                new Vector3f(0, -e0.z, e0.y), new Vector3f(0, -e1.z, e1.y), new Vector3f(0, -e2.z, e2.y),
                new Vector3f(e0.z, 0, -e0.x), new Vector3f(e1.z, 0, -e1.x), new Vector3f(e2.z, 0, -e2.x),
                new Vector3f(-e0.y, e0.x, 0), new Vector3f(-e1.y, e1.x, 0), new Vector3f(-e2.y, e2.x, 0),
                new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), // box normals
                new Vector3f(e0).cross(new Vector3f(e1)) // triangle normal
        };

        for (Vector3f axis : tests) {
            float[] proj = new float[3];
            proj[0] = axis.dot(v0);
            proj[1] = axis.dot(v1);
            proj[2] = axis.dot(v2);
            float minT = Math.min(proj[0], Math.min(proj[1], proj[2]));
            float maxT = Math.max(proj[0], Math.max(proj[1], proj[2]));

            float r = halfSize.x * Math.abs(axis.x) + halfSize.y * Math.abs(axis.y) + halfSize.z * Math.abs(axis.z);
            if (minT > r || maxT < -r) return false;
        }

        return true;
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }

    /**
     * Returns the destroy speed (mining speed) of the camouflage block.
     * This is used for calculating how long it takes to mine the frame.
     */
    public float getDestroySpeed() {
        return this.camouflage.getDestroySpeed(null, null);
    }

    /**
     * Returns the explosion resistance (blast resistance) of the camouflage block.
     */
    public float getExplosionResistance() {
        return this.camouflage.getBlock().getExplosionResistance();
    }

    public static float getDestroySpeed(OmnishapeData data) {
        return data.camouflage().getDestroySpeed(null, null);
    }

    public static float getExplosionResistance(OmnishapeData data) {
        return data.camouflage().getBlock().getExplosionResistance();
    }
}