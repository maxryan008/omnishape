package dev.omnishape.block;

import dev.omnishape.BlockRotation;
import dev.omnishape.BlockRotationProperty;
import dev.omnishape.block.entity.FrameBlockEntity;
import dev.omnishape.registry.OmnishapeComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.List;

public class FrameBlock extends Block implements EntityBlock {
    public static final BlockRotationProperty ROTATION = new BlockRotationProperty("rotation");

    public FrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, BlockRotation.IDENTITY));
    }

    private static final int FRAME_HITBOX_RESOLUTION = 8;

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!(level.getBlockEntity(pos) instanceof FrameBlockEntity be)) return;

        if (stack.has(OmnishapeComponents.CAMO_STATE)) {
            BlockState camo = stack.get(OmnishapeComponents.CAMO_STATE);
            if (camo != null) {
                be.setCamo(camo);
            }
        }

        if (stack.has(OmnishapeComponents.CORNERS_STATE)) {
            List<Vector3f> corners = stack.get(OmnishapeComponents.CORNERS_STATE);
            if (!corners.isEmpty()) {
                be.setCorners(corners);
            }
        }

        be.setChanged();
        level.blockUpdated(pos, this);
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        super.onPlace(blockState, level, blockPos, blockState2, bl);
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

            Matrix3f rotationMatrix = new Matrix3f()
                    .rotateXYZ(
                            (float) Math.toRadians(rot.pitch),
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

    private static final int[][] FACE_INDICES = {
            {0, 1, 3, 2}, // BACK (Z-)
            {6, 7, 5, 4}, // FRONT (Z+)
            {4, 5, 1, 0}, // TOP (Y+)
            {2, 3, 7, 6}, // BOTTOM (Y-)
            {0, 2, 6, 4}, // LEFT (X-)
            {5, 7, 3, 1}  // RIGHT (X+)
    };

    public static VoxelShape generateShapeFromCorners(Vector3f[] corners, Matrix3f rotationMatrix) {

        Vector3f[] rotatedCorners = new Vector3f[8];
        for (int i = 0; i < corners.length; i++) {
            rotatedCorners[i] = new Vector3f(corners[i]);
            rotatedCorners[i].x = 1.0f - rotatedCorners[i].x;  // Apply mirror if needed
            rotatedCorners[i].sub(0.5f, 0.5f, 0.5f);           // Move to origin
            rotationMatrix.transform(rotatedCorners[i]);       // Rotate
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

                    float fillRatio = insideCount / (float)(samplesPerAxis * samplesPerAxis * samplesPerAxis);
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
    private static boolean intersectsTriangle(Vector3f orig, Vector3f dir, Vector3f v0, Vector3f v1, Vector3f v2) {
        final float EPSILON = 1e-6f;
        Vector3f edge1 = new Vector3f(), edge2 = new Vector3f();
        v1.sub(v0, edge1);
        v2.sub(v0, edge2);

        Vector3f h = new Vector3f();
        dir.cross(edge2, h);
        float a = edge1.dot(h);

        if (a > -EPSILON && a < EPSILON) return false; // Ray is parallel

        float f = 1.0f / a;
        Vector3f s = new Vector3f();
        orig.sub(v0, s);

        float u = f * s.dot(h);
        if (u < 0.0f || u > 1.0f) return false;

        Vector3f q = new Vector3f();
        s.cross(edge1, q);
        float v = f * dir.dot(q);
        if (v < 0.0f || u + v > 1.0f) return false;

        float t = f * edge2.dot(q);
        return t > EPSILON;
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
                int x = (int)(p.x * FRAME_HITBOX_RESOLUTION);
                int y = (int)(p.y * FRAME_HITBOX_RESOLUTION);
                int z = (int)(p.z * FRAME_HITBOX_RESOLUTION);
                if (x >= 0 && y >= 0 && z >= 0 && x < FRAME_HITBOX_RESOLUTION && y < FRAME_HITBOX_RESOLUTION && z < FRAME_HITBOX_RESOLUTION) {
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

    @Override
    protected VoxelShape getVisualShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return getShape(blockState, blockGetter, blockPos, collisionContext);
    }

    @Override
    protected boolean isPathfindable(BlockState blockState, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState blockState) {
        return false;
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return false;
    }

    @Override
    public boolean hasDynamicShape() {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return super.getRenderShape(blockState);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        BlockEntity be = blockGetter.getBlockEntity(blockPos);
        if (be instanceof FrameBlockEntity frame) {
            BlockRotation rot = blockState.getValue(FrameBlock.ROTATION);

            Matrix3f rotationMatrix = new Matrix3f()
                    .rotateXYZ(
                            (float) Math.toRadians(rot.pitch),
                            (float) Math.toRadians(rot.yaw),
                            (float) Math.toRadians(rot.roll)
                    );

            return frame.getOrBuildShape(rotationMatrix);
        }
        return Shapes.block(); // fallback
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return getInteractionShape(blockState, blockGetter, blockPos);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
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
    public ItemStack getCloneItemStack(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
        BlockEntity be = levelReader.getBlockEntity(blockPos);
        if (be instanceof FrameBlockEntity frame) {
            ItemStack stack = new ItemStack(this);

            // Copy data
            stack.set(OmnishapeComponents.CAMO_STATE, frame.getCamo());

            List<Vector3f> cornerList = new java.util.ArrayList<>();
            for (Vector3f v : frame.getCorners()) {
                cornerList.add(new Vector3f(v));
            }
            stack.set(OmnishapeComponents.CORNERS_STATE, cornerList);

            return stack;
        }
        return super.getCloneItemStack(levelReader, blockPos, blockState);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof FrameBlockEntity frame) {
            ItemStack stack = new ItemStack(this);
            stack.set(OmnishapeComponents.CAMO_STATE, frame.getCamo());

            List<Vector3f> cornerList = new java.util.ArrayList<>();
            for (Vector3f v : frame.getCorners()) {
                cornerList.add(new Vector3f(v));
            }
            stack.set(OmnishapeComponents.CORNERS_STATE, cornerList);

            return List.of(stack);
        }
        return super.getDrops(blockState, builder);
    }
}
