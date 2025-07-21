package dev.omnishape.api;

import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;

public record OmnishapeData(BlockState camouflage, Vector3f[] corners) {
    public static final int CORNER_COUNT = 8;

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("Camo", BlockState.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, this.camouflage())
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
        BlockState camo = BlockState.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Camo"))
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
            rasterizeQuad(mask, corners[face[0]], corners[face[1]], corners[face[2]], corners[face[3]]);
        }
    }

    private static VoxelShape buildVoxelShape(boolean[][][] voxels, int res) {
        VoxelShape shape = Shapes.empty();
        float unit = 1f / res;

        for (int x = 0; x < res; x++) {
            for (int y = 0; y < res; y++) {
                for (int z = 0; z < res; z++) {
                    if (voxels[x][y][z]) {
                        shape = Shapes.or(shape, Shapes.box(
                                x * unit, y * unit, z * unit,
                                (x + 1) * unit, (y + 1) * unit, (z + 1) * unit
                        ));
                    }
                }
            }
        }

        return shape.optimize();
    }

    private static void rasterizeQuad(boolean[][][] grid, Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3) {
        int steps = 32;
        int res = grid.length;
        for (int i = 0; i <= steps; i++) {
            float t1 = i / (float) steps;
            Vector3f a = lerp(v0, v1, t1);
            Vector3f b = lerp(v3, v2, t1);
            for (int j = 0; j <= steps; j++) {
                float t2 = j / (float) steps;
                Vector3f p = lerp(a, b, t2);
                int x = (int) (p.x * res);
                int y = (int) (p.y * res);
                int z = (int) (p.z * res);
                if (x >= 0 && x < res && y >= 0 && y < res && z >= 0 && z < res) {
                    grid[x][y][z] = true;
                }
            }
        }
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
        );
    }
}