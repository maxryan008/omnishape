package dev.omnishape.block.entity;

import com.mojang.math.Matrix3f;
import com.mojang.math.Vector3f;
import dev.omnishape.block.FrameBlock;
import dev.omnishape.registry.OmnishapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class FrameBlockEntity extends BlockEntity {
    private VoxelShape cachedShape = null;
    private final Vector3f[] corners = new Vector3f[8];
    private BlockState camoState = Blocks.AIR.defaultBlockState(); // Default fallback
    public FrameBlockEntity(BlockPos pos, BlockState state) {
        super(OmnishapeBlockEntities.FRAME_BLOCK, pos, state);
        for (int i = 0; i < 8; i++) corners[i] = new Vector3f((i & 1), (i >> 1 & 1), (i >> 2 & 1));
    }

    public BlockState getCamo() {
        return camoState;
    }

    public void setCamo(BlockState state) {
        this.camoState = state;
    }

    public Vector3f[] getCorners() {
        return corners;
    }

    public void setCorners(List<Vector3f> corners) {
        for (int i = 0; i < Math.min(this.corners.length, corners.size()); i++) {
            this.corners[i] = corners.get(i);
        }
        this.cachedShape = null; // invalidate cache
        setChanged();
    }

    public void setCorner(int index, Vector3f value) {
        corners[index] = value;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        OmnibenchBlockEntity.SaveCorners(tag, corners);

        tag.put("Camo", NbtUtils.writeBlockState(camoState));
    }

    @Override
    public void load(CompoundTag tag) {
        ListTag list = tag.getList("Corners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < 8; i++) {
            CompoundTag vecTag = list.getCompound(i);
            corners[i] = new Vector3f(vecTag.getFloat("x"), vecTag.getFloat("y"), vecTag.getFloat("z"));
        }

        camoState = NbtUtils.readBlockState(tag.getCompound("Camo"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        OmnibenchBlockEntity.SaveCorners(tag, corners);

        tag.put("Camo", NbtUtils.writeBlockState(camoState));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public VoxelShape getOrBuildShape(Matrix3f rotationMatrix) {
        if (cachedShape == null) {
            cachedShape = FrameBlock.generateShapeFromCorners(corners, rotationMatrix); // or static method in helper
        }
        return cachedShape;
    }
}
