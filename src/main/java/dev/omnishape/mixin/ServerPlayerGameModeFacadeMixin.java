package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeData;
import dev.omnishape.api.facade.FacadeManager;
import dev.omnishape.api.facade.FacadeRaycast;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeFacadeMixin {

    @Shadow
    protected ServerLevel level;

    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(
            method = "destroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$destroyFacadeInsteadOfHost(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        FacadeData facade =
                FacadeManager.getFacadeOrNull(
                        level,
                        pos
                );

        if (facade == null) {
            return;
        }

        if (FacadeRaycast.raycastPlayer(
                player,
                level,
                pos,
                facade
        ) != FacadeRaycast.HitPart.FACADE) {
            return;
        }

        FacadeManager.removeFacade(
                level,
                pos,
                true
        );

        level.destroyBlockProgress(
                player.getId(),
                pos,
                -1
        );

        cir.setReturnValue(
                true
        );
    }
}