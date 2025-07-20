package dev.omnishape.client.mixin;

import com.mojang.math.Vector3f;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.client.ClientHooks;
import dev.omnishape.client.interfaces.ClientCornerSync;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(OmnibenchBlockEntity.class)
public class OmnibenchBlockEntityMixin implements ClientCornerSync {
    @Override
    public void sendCornerUpdateToServer(BlockPos pos, int index, Vector3f vec) {
        ClientHooks.sendCornerUpdate(pos, index, vec);
    }
}