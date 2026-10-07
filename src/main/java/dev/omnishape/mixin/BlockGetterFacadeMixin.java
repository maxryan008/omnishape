package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockGetter.class)
public interface BlockGetterFacadeMixin {

    @Inject(
            method = "getLightEmission",
            at = @At("HEAD"),
            cancellable = true
    )
    private void omnishape$facadeLightEmission(
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        BlockGetter level =
                (BlockGetter) (Object) this;

        FacadeManager.getFacade(
                level,
                pos
        ).ifPresent(
                facade ->
                        cir.setReturnValue(
                                facade.omnishape()
                                        .camouflage()
                                        .getLightEmission()
                        )
        );
    }
}