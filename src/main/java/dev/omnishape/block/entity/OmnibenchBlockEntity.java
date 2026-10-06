package dev.omnishape.block.entity;

import dev.omnishape.Constant;
import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.registry.OmnishapeBlockEntities;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class OmnibenchBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private final Vector3f[] corners = new Vector3f[8];
    private boolean suppressUpdates = false;
    private OmnibenchMenu currentMenu = null;

    private final SimpleContainer inventory = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();

            if (!suppressUpdates) {
                OmnibenchBlockEntity.this.setChanged();

                if (currentMenu != null) {
                    currentMenu.updateOutputSlot();
                }
            }
        }
    };

    public OmnibenchBlockEntity(BlockPos pos, BlockState state) {
        super(OmnishapeBlockEntities.OMNIBENCH, pos, state);

        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(
                    i & 1,
                    i >> 1 & 1,
                    i >> 2 & 1
            );
        }
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public void setSuppressUpdates(boolean suppress) {
        this.suppressUpdates = suppress;
    }

    public void clearMenuReference() {
        this.currentMenu = null;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Omnibench");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        this.currentMenu = new OmnibenchMenu(syncId, inv, this);
        return this.currentMenu;
    }

    public Vector3f[] getCorners() {
        return corners;
    }

    public boolean hasMenu() {
        return this.currentMenu != null;
    }

    public OmnibenchMenu getMenu() {
        return this.currentMenu;
    }

    public Vector3f getCorner(int i) {
        return corners[i];
    }

    public void setCorner(int index, Vector3f value) {
        if (index < 0 || index >= corners.length || value == null) {
            return;
        }

        if (!Float.isFinite(value.x)
                || !Float.isFinite(value.y)
                || !Float.isFinite(value.z)) {
            return;
        }

        Vector3f target = new Vector3f(
                Mth.clamp(value.x, 0.0f, 1.0f),
                Mth.clamp(value.y, 0.0f, 1.0f),
                Mth.clamp(value.z, 0.0f, 1.0f)
        );

        Vector3f constrained = constrainCornerMovement(index, target);

        corners[index].set(constrained);

        setChanged();
        inventory.setChanged();

        if (level != null && level.isClientSide()) {
            sendCornerUpdateToServer(
                    worldPosition,
                    index,
                    new Vector3f(constrained)
            );
        }
    }

    public void resetCorners() {
        for (int i = 0; i < corners.length; i++) {
            corners[i].set(
                    i & 1,
                    i >> 1 & 1,
                    i >> 2 & 1
            );
        }

        setChanged();
        inventory.setChanged();
    }

    /*
     * The six quads of the OmniShape block.
     *
     * Each quad is split into:
     *
     *     0 ----- 1
     *     |     / |
     *     |   /   |
     *     | /     |
     *     3 ----- 2
     *
     * Triangle 1: 0, 1, 2
     * Triangle 2: 0, 2, 3
     */
    private static final int[][] FACES = {
            {4, 5, 1, 0}, // -Y
            {2, 3, 7, 6}, // +Y

            {1, 3, 2, 0}, // -Z
            {6, 7, 5, 4}, // +Z

            {0, 2, 6, 4}, // -X
            {5, 7, 3, 1}  // +X
    };

    /*
     * The two coordinates used to determine winding for each face.
     *
     * X faces are viewed in Y/Z.
     * Y faces are viewed in X/Z.
     * Z faces are viewed in X/Y.
     *
     * Axis:
     * 0 = X
     * 1 = Y
     * 2 = Z
     */
    private static final int[][] FACE_PROJECTION_AXES = {
            {0, 2}, // -Y -> X/Z
            {0, 2}, // +Y -> X/Z

            {0, 1}, // -Z -> X/Y
            {0, 1}, // +Z -> X/Y

            {1, 2}, // -X -> Y/Z
            {1, 2}  // +X -> Y/Z
    };

    /**
     * Constrains a corner movement so none of the triangles making up the
     * six outside faces can reverse their original winding.
     *
     * A triangle is allowed to collapse completely into a line.
     *
     * It is not allowed to continue through that line, because doing so
     * would reverse the ordering of its three corners and therefore turn
     * that portion of the outside face inside-out.
     */
    private Vector3f constrainCornerMovement(int movedCorner, Vector3f target) {
        Vector3f start = corners[movedCorner];

        /*
         * maxT describes how far we can travel from:
         *
         * start -> target
         *
         * 0 = do not move
         * 1 = reach the requested target completely
         */
        double maxT = 1.0;

        for (int faceIndex = 0; faceIndex < FACES.length; faceIndex++) {
            int[] face = FACES[faceIndex];

            int axisU = FACE_PROJECTION_AXES[faceIndex][0];
            int axisV = FACE_PROJECTION_AXES[faceIndex][1];

            /*
             * First triangle:
             *
             * face[0] -> face[1] -> face[2]
             */
            maxT = Math.min(
                    maxT,
                    getMaximumTriangleMovement(
                            movedCorner,
                            target,
                            face[0],
                            face[1],
                            face[2],
                            axisU,
                            axisV
                    )
            );

            /*
             * Second triangle:
             *
             * face[0] -> face[2] -> face[3]
             */
            maxT = Math.min(
                    maxT,
                    getMaximumTriangleMovement(
                            movedCorner,
                            target,
                            face[0],
                            face[2],
                            face[3],
                            axisU,
                            axisV
                    )
            );

            if (maxT <= 0.0) {
                return new Vector3f(start);
            }
        }

        if (maxT >= 1.0) {
            return target;
        }

        return new Vector3f(
                (float) (start.x + (target.x - start.x) * maxT),
                (float) (start.y + (target.y - start.y) * maxT),
                (float) (start.z + (target.z - start.z) * maxT)
        );
    }

    /**
     * Returns the maximum fraction of the requested movement that can occur
     * before this triangle reverses its original winding.
     *
     * Returning 1 means this triangle imposes no restriction.
     *
     * Returning 0 means the triangle is already exactly on its boundary and
     * the requested movement would immediately reverse it.
     */
    private double getMaximumTriangleMovement(
            int movedCorner,
            Vector3f target,
            int aIndex,
            int bIndex,
            int cIndex,
            int axisU,
            int axisV
    ) {
        /*
         * If the corner being moved is not part of this triangle, then this
         * triangle cannot change as a result of the movement.
         */
        if (movedCorner != aIndex
                && movedCorner != bIndex
                && movedCorner != cIndex) {
            return 1.0;
        }

        /*
         * Work out which winding this triangle had in the original,
         * undeformed cube.
         *
         * This is the winding that must be preserved forever.
         */
        double referenceArea = signedAreaOriginal(
                aIndex,
                bIndex,
                cIndex,
                axisU,
                axisV
        );

        if (referenceArea == 0.0) {
            throw new IllegalStateException(
                    "OmniShape face triangle has zero area in the default cube: "
                            + aIndex + ", " + bIndex + ", " + cIndex
            );
        }

        double referenceSign = Math.signum(referenceArea);

        /*
         * Signed area at the corner's current position.
         */
        double startArea = signedArea(
                aIndex,
                bIndex,
                cIndex,
                movedCorner,
                corners[movedCorner],
                axisU,
                axisV
        ) * referenceSign;

        /*
         * Signed area if the corner reached the requested target.
         */
        double targetArea = signedArea(
                aIndex,
                bIndex,
                cIndex,
                movedCorner,
                target,
                axisU,
                axisV
        ) * referenceSign;

        /*
         * Positive:
         *     same winding as the original triangle.
         *
         * Zero:
         *     all three projected points are collinear.
         *     This is explicitly VALID.
         *
         * Negative:
         *     winding has reversed.
         */
        if (targetArea >= 0.0) {
            return 1.0;
        }

        /*
         * If we're already exactly on the line and the requested movement
         * goes to the invalid side, we cannot move in that direction at all.
         */
        if (startArea <= 0.0) {
            return 0.0;
        }

        /*
         * Only one corner is moving, so the signed area changes linearly
         * between the start and target positions.
         *
         * Solve:
         *
         * startArea + t(targetArea - startArea) = 0
         *
         * giving the exact point where the three corners become collinear.
         */
        return startArea / (startArea - targetArea);
    }

    /**
     * Calculates the signed 2D area of a triangle after projecting it onto
     * the two axes appropriate for its face.
     *
     * The moved corner can be substituted with a proposed position without
     * modifying the actual stored corners.
     */
    private double signedArea(
            int aIndex,
            int bIndex,
            int cIndex,
            int movedCorner,
            Vector3f movedPosition,
            int axisU,
            int axisV
    ) {
        Vector3f a = aIndex == movedCorner
                ? movedPosition
                : corners[aIndex];

        Vector3f b = bIndex == movedCorner
                ? movedPosition
                : corners[bIndex];

        Vector3f c = cIndex == movedCorner
                ? movedPosition
                : corners[cIndex];

        double au = getAxis(a, axisU);
        double av = getAxis(a, axisV);

        double bu = getAxis(b, axisU);
        double bv = getAxis(b, axisV);

        double cu = getAxis(c, axisU);
        double cv = getAxis(c, axisV);

        return (bu - au) * (cv - av)
                - (bv - av) * (cu - au);
    }

    /**
     * Calculates the winding of a triangle in the original 1x1x1 cube.
     *
     * We derive the original coordinates directly from the corner index:
     *
     * bit 0 = X
     * bit 1 = Y
     * bit 2 = Z
     */
    private static double signedAreaOriginal(
            int aIndex,
            int bIndex,
            int cIndex,
            int axisU,
            int axisV
    ) {
        double au = getOriginalAxis(aIndex, axisU);
        double av = getOriginalAxis(aIndex, axisV);

        double bu = getOriginalAxis(bIndex, axisU);
        double bv = getOriginalAxis(bIndex, axisV);

        double cu = getOriginalAxis(cIndex, axisU);
        double cv = getOriginalAxis(cIndex, axisV);

        return (bu - au) * (cv - av)
                - (bv - av) * (cu - au);
    }

    private static float getAxis(Vector3f vector, int axis) {
        return switch (axis) {
            case 0 -> vector.x;
            case 1 -> vector.y;
            case 2 -> vector.z;
            default -> throw new IllegalArgumentException(
                    "Invalid axis: " + axis
            );
        };
    }

    private static int getOriginalAxis(int cornerIndex, int axis) {
        return cornerIndex >> axis & 1;
    }

    public void sendCornerUpdateToServer(BlockPos pos, int index, Vector3f vec) {
        // This should be overridden client side eventually
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);

        ContainerHelper.saveAllItems(
                compoundTag,
                inventory.getItems(),
                provider
        );

        SaveCorners(compoundTag, corners);
    }

    static void SaveCorners(CompoundTag compoundTag, Vector3f[] corners) {
        ListTag cornerList = new ListTag();

        for (Vector3f vec : corners) {
            CompoundTag vecTag = new CompoundTag();

            vecTag.putFloat(Constant.Nbt.X, vec.x);
            vecTag.putFloat(Constant.Nbt.Y, vec.y);
            vecTag.putFloat(Constant.Nbt.Z, vec.z);

            cornerList.add(vecTag);
        }

        compoundTag.put(Constant.Nbt.CORNERS, cornerList);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithFullMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);

        ContainerHelper.loadAllItems(
                compoundTag,
                inventory.getItems(),
                provider
        );

        ListTag list = compoundTag.getList(
                Constant.Nbt.CORNERS,
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < list.size() && i < corners.length; i++) {
            CompoundTag vecTag = list.getCompound(i);

            corners[i].set(
                    vecTag.getFloat(Constant.Nbt.X),
                    vecTag.getFloat(Constant.Nbt.Y),
                    vecTag.getFloat(Constant.Nbt.Z)
            );
        }
    }

    @Override
    public Object getScreenOpeningData(ServerPlayer serverPlayer) {
        return this.getBlockPos();
    }
}