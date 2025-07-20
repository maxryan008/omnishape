package dev.omnishape.client.interfaces;

import com.mojang.math.Vector3f;
import net.minecraft.core.BlockPos;

public interface ClientCornerSync {
    void sendCornerUpdateToServer(BlockPos pos, int index, Vector3f vec);
}
