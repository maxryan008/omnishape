package dev.omnishape.client.mixin;

import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.api.facade.FacadeRaycast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeFacadeMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "destroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$doNotClientDestroyHost(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (minecraft.level == null
                || minecraft.player == null) {
            return;
        }

        if (!FacadeManager.hasFacade(
                minecraft.level,
                pos
        )) {
            return;
        }

        if (FacadeRaycast.raycastPlayer(
                minecraft.player,
                minecraft.level,
                pos
        ) != FacadeRaycast.HitPart.FACADE) {
            return;
        }

        /*
         * Optimistically remove the visual facade.
         *
         * The normal destroy packet still reaches the server, where
         * ServerPlayerGameModeFacadeMixin performs the authoritative removal.
         */
        FacadeManager.removeFacade(
                minecraft.level,
                pos,
                false
        );

        cir.setReturnValue(true);
    }
}