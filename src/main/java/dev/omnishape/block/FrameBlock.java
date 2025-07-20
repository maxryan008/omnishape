package dev.omnishape.block;

import com.mojang.math.Matrix3f;
import com.mojang.math.Vector3f;
import dev.omnishape.BlockRotation;
import dev.omnishape.BlockRotationProperty;
import dev.omnishape.block.entity.FrameBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mojang.math.Constants.EPSILON;

public class FrameBlock extends Block implements EntityBlock {
    public static final BlockRotationProperty ROTATION = new BlockRotationProperty("rotation");
    private static final int FRAME_HITBOX_RESOLUTION = 8;
    private static final int[][] FACE_INDICES = {
            {0, 1, 3, 2}, // BACK (Z-)
            {6, 7, 5, 4}, // FRONT (Z+)
            {4, 5, 1, 0}, // TOP (Y+)
            {2, 3, 7, 6}, // BOTTOM (Y-)
            {0, 2, 6, 4}, // LEFT (X-)
            {5, 7, 3, 1}  // RIGHT (X+)
    };

    public FrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, BlockRotation.IDENTITY));
    }

    public static VoxelShape generateShapeFromCorners(Vector3f[] corners, Matrix3f rotationMatrix) {

        Vector3f[] rotatedCorners = new Vector3f[8];
        for (int i = 0; i < corners.length; i++) {
            rotatedCorners[i] = corners[i];
            rotatedCorners[i].set(1.0f - rotatedCorners[i].x(), rotatedCorners[i].y(), rotatedCorners[i].z()); // Apply mirror if needed
            rotatedCorners[i].sub(new Vector3f(0.5f, 0.5f, 0.5f));           // Move to origin
            rotatedCorners[i].transform(rotationMatrix);       // Rotate
            rotatedCorners[i].add(0.5f, 0.5f, 0.5f);           // Move back
        }

        final int RES = FRAME_HITBOX_RESOLUTION;
        final float unit = 1f / RES;
        final float threshold = 0.6f;
        final int samplesPerAxis = 3; // 3x3x3 = 27 sample points
        final float sampleStep = unit / samplesPerAxis;

        boolean[][][] voxel = new boolean[RES][RES][RES];

        // Rasterize surface into a mask (same as before)
        boolean[][][] mask = new boolean[RES][RES][RES];
        for (int[] face : FACE_INDICES) {
            rasterizeQuad(mask, rotatedCorners[face[0]], rotatedCorners[face[1]],
                    rotatedCorners[face[2]], rotatedCorners[face[3]]);
        }

        // Check fill ratio for each voxel
        for (int x = 0; x < RES; x++) {
            for (int y = 0; y < RES; y++) {
                for (int z = 0; z < RES; z++) {
                    int insideCount = 0;

                    for (int i = 0; i < samplesPerAxis; i++) {
                        for (int j = 0; j < samplesPerAxis; j++) {
                            for (int k = 0; k < samplesPerAxis; k++) {
                                float sx = x * unit + i * sampleStep + sampleStep / 2;
                                float sy = y * unit + j * sampleStep + sampleStep / 2;
                                float sz = z * unit + k * sampleStep + sampleStep / 2;

                                if (pointInsideMesh(sx, sy, sz, rotatedCorners)) {
                                    insideCount++;
                                }
                            }
                        }
                    }

                    float fillRatio = insideCount / (float) (samplesPerAxis * samplesPerAxis * samplesPerAxis);
                    if (fillRatio >= threshold) {
                        voxel[x][y][z] = true;
                    }
                }
            }
        }

        // Convert to shape
        VoxelShape shape = Shapes.empty();
        for (int x = 0; x < RES; x++) {
            for (int y = 0; y < RES; y++) {
                for (int z = 0; z < RES; z++) {
                    if (voxel[x][y][z]) {
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

    private static boolean pointInsideMesh(float x, float y, float z, Vector3f[] corners) {
        Vector3f origin = new Vector3f(x, y, z);
        Vector3f dir = new Vector3f(1, 0, 0); // Arbitrary direction

        int intersections = 0;
        for (int[] face : FACE_INDICES) {
            // Each quad = 2 triangles
            if (intersectsTriangle(origin, dir, corners[face[0]], corners[face[1]], corners[face[2]])) intersections++;
            if (intersectsTriangle(origin, dir, corners[face[0]], corners[face[2]], corners[face[3]])) intersections++;
        }

        return intersections % 2 == 1; // Odd == inside
    }

    // Möller–Trumbore intersection algorithm
    public static boolean intersectsTriangle(Vector3f orig, Vector3f dir, Vector3f v0, Vector3f v1, Vector3f v2) {
        Vector3f edge1 = subtract(v1, v0);
        Vector3f edge2 = subtract(v2, v0);

        Vector3f h = cross(dir, edge2);
        float a = dot(edge1, h);
        if (a > -EPSILON && a < EPSILON) return false;

        float f = 1.0f / a;
        Vector3f s = subtract(orig, v0);
        float u = f * dot(s, h);
        if (u < 0.0f || u > 1.0f) return false;

        Vector3f q = cross(s, edge1);
        float v = f * dot(dir, q);
        if (v < 0.0f || u + v > 1.0f) return false;

        float t = f * dot(edge2, q);
        return t > EPSILON;
    }

    private static Vector3f subtract(Vector3f a, Vector3f b) {
        return new Vector3f(a.x() - b.x(), a.y() - b.y(), a.z() - b.z());
    }

    private static Vector3f cross(Vector3f a, Vector3f b) {
        float x = a.y() * b.z() - a.z() * b.y();
        float y = a.z() * b.x() - a.x() * b.z();
        float z = a.x() * b.y() - a.y() * b.x();
        return new Vector3f(x, y, z);
    }

    private static float dot(Vector3f a, Vector3f b) {
        return a.x() * b.x() + a.y() * b.y() + a.z() * b.z();
    }

    private static void rasterizeQuad(boolean[][][] grid, Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3) {
        int steps = 32;
        for (int i = 0; i <= steps; i++) {
            float t1 = i / (float) steps;
            Vector3f a = lerp(v0, v1, t1);
            Vector3f b = lerp(v3, v2, t1);

            for (int j = 0; j <= steps; j++) {
                float t2 = j / (float) steps;
                Vector3f p = lerp(a, b, t2);
                int x = (int) (p.x() * FRAME_HITBOX_RESOLUTION);
                int y = (int) (p.y() * FRAME_HITBOX_RESOLUTION);
                int z = (int) (p.z() * FRAME_HITBOX_RESOLUTION);
                if (x >= 0 && y >= 0 && z >= 0 && x < FRAME_HITBOX_RESOLUTION && y < FRAME_HITBOX_RESOLUTION && z < FRAME_HITBOX_RESOLUTION) {
                    grid[x][y][z] = true;
                }
            }
        }
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t) {
        return new Vector3f(
                a.x() + (b.x() - a.x()) * t,
                a.y() + (b.y() - a.y()) * t,
                a.z() + (b.z() - a.z()) * t
        );
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof FrameBlockEntity be)) return;

        CompoundTag tag = stack.getTag();
        if (tag != null) {
            if (tag.contains("CamoState")) {
                BlockState camo = NbtUtils.readBlockState(tag.getCompound("CamoState"));
                if (camo != null) {
                    be.setCamo(camo);
                }
            }

            if (tag.contains("CornersState", Tag.TAG_LIST)) {
                ListTag list = tag.getList("CornersState", Tag.TAG_COMPOUND);
                List<Vector3f> corners = new ArrayList<>(list.size());

                for (Tag cornerTag : list) {
                    if (cornerTag instanceof CompoundTag ct) {
                        float x = ct.getFloat("x");
                        float y = ct.getFloat("y");
                        float z = ct.getFloat("z");
                        corners.add(new Vector3f(x, y, z));
                    }
                }

                if (!corners.isEmpty()) {
                    be.setCorners(corners);
                }
            }
        }

        be.setChanged();
        level.updateNeighborsAt(pos, this);
    }

    @Override
    public float getExplosionResistance() {
        return 2000F; // Needed only if you override explosion behavior
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FrameBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FrameBlockEntity frame) {
            BlockRotation rot = state.getValue(FrameBlock.ROTATION);

            Matrix3f rotationMatrix = Matrix3f.createScaleMatrix((
                    float) Math.toRadians(rot.pitch),
                    (float) Math.toRadians(rot.yaw),
                    (float) Math.toRadians(rot.roll)
            );

            return frame.getOrBuildShape(rotationMatrix);
        }
        return Shapes.block(); // fallback
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context); // same as visual
    }

    @Override
    public boolean isPathfindable(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return false;
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }



    @Override
    public @NotNull VoxelShape getInteractionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        BlockEntity be = blockGetter.getBlockEntity(blockPos);
        if (be instanceof FrameBlockEntity frame) {
            BlockRotation rot = blockState.getValue(FrameBlock.ROTATION);

            Matrix3f rotationMatrix = Matrix3f.createScaleMatrix(
                    (float) Math.toRadians(rot.pitch),
                    (float) Math.toRadians(rot.yaw),
                    (float) Math.toRadians(rot.roll)
            );

            return frame.getOrBuildShape(rotationMatrix);
        }
        return Shapes.block(); // fallback
    }

    @Override
    public @NotNull VoxelShape getBlockSupportShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return getInteractionShape(blockState, blockGetter, blockPos);
    }

    @Override
    public @NotNull VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return getInteractionShape(blockState, blockGetter, blockPos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockRotation rotation = BlockRotation.fromPlacementContext(context);
        return defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public StateDefinition<Block, BlockState> getStateDefinition() {
        return super.getStateDefinition();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof FrameBlockEntity frame) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = new CompoundTag();

            // Save camo block state
            BlockState camo = frame.getCamo();
            if (camo != null) {
                tag.put("CamoState", NbtUtils.writeBlockState(camo));
            }

            // Save corners
            List<Vector3f> corners = List.of(frame.getCorners());
            if (corners != null && !corners.isEmpty()) {
                ListTag cornerList = new ListTag();
                for (Vector3f vec : corners) {
                    CompoundTag vecTag = new CompoundTag();
                    vecTag.putFloat("x", vec.x());
                    vecTag.putFloat("y", vec.y());
                    vecTag.putFloat("z", vec.z());
                    cornerList.add(vecTag);
                }
                tag.put("CornersState", cornerList);
            }

            stack.setTag(tag);
            return stack;
        }

        return super.getCloneItemStack(level, pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof FrameBlockEntity frame) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = new CompoundTag();

            // Store camo block state
            BlockState camo = frame.getCamo();
            if (camo != null) {
                tag.put("CamoState", NbtUtils.writeBlockState(camo));
            }

            // Store corners
            List<Vector3f> corners = List.of(frame.getCorners());
            if (!corners.isEmpty()) {
                ListTag cornerList = new ListTag();
                for (Vector3f vec : corners) {
                    CompoundTag vecTag = new CompoundTag();
                    vecTag.putFloat("x", vec.x());
                    vecTag.putFloat("y", vec.y());
                    vecTag.putFloat("z", vec.z());
                    cornerList.add(vecTag);
                }
                tag.put("CornersState", cornerList);
            }

            stack.setTag(tag);
            return Collections.singletonList(stack);
        }

        return super.getDrops(state, builder);
    }
}
