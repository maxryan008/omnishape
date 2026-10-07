package dev.omnishape.api.facade;

import dev.omnishape.BlockRotation;
import dev.omnishape.api.OmnishapeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Vector3f;

public final class FacadeData {

    private static final String SHAPE_KEY =
            "Shape";

    private static final String ROTATION_KEY =
            "Rotation";

    private final OmnishapeData omnishape;
    private final BlockRotation rotation;

    /*
     * Generating an OmniShape VoxelShape is expensive:
     *
     * - rotate the vertices
     * - rasterise triangles
     * - perform triangle/AABB tests
     * - construct voxel boxes
     * - union them
     * - optimise the final shape
     *
     * Facade geometry is immutable, so this should happen exactly once.
     */
    private final VoxelShape shape;

    /*
     * Minecraft queries selection and collision shapes extremely frequently.
     *
     * Cache the final:
     *
     *     host shape ∪ facade shape
     *
     * We cache against the host VoxelShape by identity rather than by
     * BlockState because some blocks generate their shape dynamically.
     *
     * If the host returns a different shape instance, the cache naturally
     * invalidates.
     */
    private VoxelShape cachedSelectionHostShape;
    private VoxelShape cachedSelectionCombinedShape;

    private VoxelShape cachedCollisionHostShape;
    private VoxelShape cachedCollisionCombinedShape;

    public FacadeData(
            OmnishapeData omnishape,
            BlockRotation rotation
    ) {
        if (omnishape == null) {
            throw new IllegalArgumentException(
                    "Facade OmniShape data cannot be null"
            );
        }

        if (rotation == null) {
            rotation = BlockRotation.IDENTITY;
        }

        /*
         * OmnishapeData contains a mutable Vector3f[].
         *
         * Copy it so this facade becomes an immutable snapshot of the frame
         * item at the instant it was attached.
         */
        Vector3f[] sourceCorners =
                omnishape.corners();

        Vector3f[] copiedCorners =
                new Vector3f[sourceCorners.length];

        for (int i = 0; i < sourceCorners.length; i++) {
            copiedCorners[i] =
                    new Vector3f(
                            sourceCorners[i]
                    );
        }

        this.omnishape =
                new OmnishapeData(
                        omnishape.camouflage(),
                        copiedCorners
                );

        this.rotation =
                rotation;

        Matrix3f matrix =
                new Matrix3f().rotateXYZ(
                        (float) Math.toRadians(
                                rotation.pitch
                        ),
                        (float) Math.toRadians(
                                rotation.yaw
                        ),
                        (float) Math.toRadians(
                                rotation.roll
                        )
                );

        /*
         * This is the expensive operation. Do it once.
         */
        this.shape =
                this.omnishape.generateShape(
                        matrix
                );
    }

    public OmnishapeData omnishape() {
        return omnishape;
    }

    public BlockRotation rotation() {
        return rotation;
    }

    /**
     * Returns the already-generated facade geometry.
     */
    public VoxelShape getShape() {
        return shape;
    }

    /**
     * Returns:
     *
     *     host selection shape ∪ facade shape
     *
     * without rebuilding that union every frame.
     */
    public VoxelShape getSelectionShape(
            VoxelShape hostShape
    ) {
        if (cachedSelectionHostShape == hostShape
                && cachedSelectionCombinedShape != null) {

            return cachedSelectionCombinedShape;
        }

        VoxelShape combined =
                Shapes.or(
                        hostShape,
                        shape
                ).optimize();

        cachedSelectionHostShape =
                hostShape;

        cachedSelectionCombinedShape =
                combined;

        return combined;
    }

    /**
     * Returns:
     *
     *     host collision shape ∪ facade shape
     *
     * without rebuilding that union continuously.
     */
    public VoxelShape getCollisionShape(
            VoxelShape hostShape
    ) {
        if (cachedCollisionHostShape == hostShape
                && cachedCollisionCombinedShape != null) {

            return cachedCollisionCombinedShape;
        }

        VoxelShape combined =
                Shapes.or(
                        hostShape,
                        shape
                ).optimize();

        cachedCollisionHostShape =
                hostShape;

        cachedCollisionCombinedShape =
                combined;

        return combined;
    }

    public ItemStack createItem() {
        return omnishape.createItem();
    }

    public CompoundTag toNbt() {
        CompoundTag tag =
                new CompoundTag();

        tag.put(
                SHAPE_KEY,
                omnishape.toNbt()
        );

        tag.put(
                ROTATION_KEY,
                rotation.toTag()
        );

        return tag;
    }

    public static FacadeData fromNbt(
            CompoundTag tag
    ) {
        OmnishapeData shape =
                OmnishapeData.fromNbt(
                        tag.getCompound(
                                SHAPE_KEY
                        )
                );

        BlockRotation rotation =
                BlockRotation.fromTag(
                        tag.getCompound(
                                ROTATION_KEY
                        )
                );

        return new FacadeData(
                shape,
                rotation
        );
    }
}