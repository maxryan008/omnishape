package dev.omnishape.api.facade;

import dev.omnishape.BlockRotation;
import dev.omnishape.api.OmnishapeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;

public record FacadeData(
        OmnishapeData omnishape,
        BlockRotation rotation
) {

    private static final String SHAPE_KEY =
            "Shape";

    private static final String ROTATION_KEY =
            "Rotation";

    public FacadeData {
        if (omnishape == null) {
            throw new IllegalArgumentException(
                    "Facade OmniShape data cannot be null"
            );
        }

        if (rotation == null) {
            rotation = BlockRotation.IDENTITY;
        }
    }

    public VoxelShape getShape() {
        Matrix3f matrix =
                new Matrix3f().rotateXYZ(
                        (float) Math.toRadians(rotation.pitch),
                        (float) Math.toRadians(rotation.yaw),
                        (float) Math.toRadians(rotation.roll)
                );

        return omnishape.generateShape(matrix);
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