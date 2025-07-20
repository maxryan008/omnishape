package dev.omnishape.network.packet;

import com.mojang.math.Vector3f;
import dev.omnishape.Omnishape;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class SetCornerC2SPacket {
    public static final ResourceLocation ID = new ResourceLocation(Omnishape.MOD_ID, "set_corner_c2s");

    private final BlockPos pos;
    private final int index;
    private final Vector3f vec;

    public SetCornerC2SPacket(BlockPos pos, int index, Vector3f vec) {
        this.pos = pos;
        this.index = index;
        this.vec = vec;
    }

    public BlockPos pos() {
        return pos;
    }

    public int index() {
        return index;
    }

    public Vector3f vec() {
        return vec;
    }

    public static SetCornerC2SPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int index = buf.readInt();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        return new SetCornerC2SPacket(pos, index, new Vector3f(x, y, z));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(index);
        buf.writeFloat(vec.x());
        buf.writeFloat(vec.y());
        buf.writeFloat(vec.z());
    }
}