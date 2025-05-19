package dev.omnishape.client.interfaces;

import net.minecraft.core.BlockPos;
import org.joml.Vector3f;

public interface ClientCornerSync {
    void sendCornerUpdateToServer(BlockPos pos, int index, Vector3f vec);
}
