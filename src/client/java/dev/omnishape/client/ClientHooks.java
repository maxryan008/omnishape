package dev.omnishape.client;

import dev.omnishape.network.packet.SetCornerC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class ClientHooks {
    public static void sendCornerUpdate(BlockPos pos, int index, Vector3f vec) {
        ClientPlayNetworking.send(new SetCornerC2SPacket(pos, index, vec));
    }
}
