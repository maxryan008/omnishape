package dev.omnishape.client;

import com.mojang.math.Vector3f;
import dev.omnishape.network.packet.SetCornerC2SPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class ClientHooks {
    public static void sendCornerUpdate(BlockPos pos, int index, Vector3f vec) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        new SetCornerC2SPacket(pos, index, vec).write(buf);
        ClientPlayNetworking.send(SetCornerC2SPacket.ID, buf);
    }
}
