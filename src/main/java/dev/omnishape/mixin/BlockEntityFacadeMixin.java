package dev.omnishape.mixin;

import dev.omnishape.api.facade.FacadeData;
import dev.omnishape.api.facade.FacadeHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntity.class)
public abstract class BlockEntityFacadeMixin
        implements FacadeHolder {

    @Unique
    private static final String OMNISHAPE_FACADE_KEY =
            "OmnishapeFacade";

    @Unique
    @Nullable
    private FacadeData omnishape$facade;

    @Shadow
    @Nullable
    protected Level level;

    @Shadow
    public abstract BlockPos getBlockPos();

    @Shadow
    public abstract BlockState getBlockState();

    @Shadow
    public abstract void setChanged();

    @Override
    public @Nullable FacadeData omnishape$getFacade() {
        return omnishape$facade;
    }

    @Override
    public void omnishape$setFacade(
            @Nullable FacadeData facade
    ) {
        this.omnishape$facade =
                facade;

        setChanged();
    }

    @Inject(
            method = "saveAdditional",
            at = @At("TAIL")
    )
    private void omnishape$saveFacade(
            CompoundTag tag,
            HolderLookup.Provider provider,
            CallbackInfo ci
    ) {
        if (omnishape$facade == null) {
            tag.remove(
                    OMNISHAPE_FACADE_KEY
            );

            return;
        }

        tag.put(
                OMNISHAPE_FACADE_KEY,
                omnishape$facade.toNbt()
        );
    }

    @Inject(
            method = "loadAdditional",
            at = @At("TAIL")
    )
    private void omnishape$loadFacade(
            CompoundTag tag,
            HolderLookup.Provider provider,
            CallbackInfo ci
    ) {
        if (!tag.contains(
                OMNISHAPE_FACADE_KEY,
                Tag.TAG_COMPOUND
        )) {
            omnishape$facade = null;
            return;
        }

        omnishape$facade =
                FacadeData.fromNbt(
                        tag.getCompound(
                                OMNISHAPE_FACADE_KEY
                        )
                );
    }

    /*
     * This catches the normal vanilla BE update-tag path as well as our
     * explicit facade networking.
     */
    @Inject(
            method = "getUpdateTag",
            at = @At("RETURN")
    )
    private void omnishape$appendFacadeUpdateTag(
            HolderLookup.Provider provider,
            CallbackInfoReturnable<CompoundTag> cir
    ) {
        if (omnishape$facade == null) {
            return;
        }

        cir.getReturnValue().put(
                OMNISHAPE_FACADE_KEY,
                omnishape$facade.toNbt()
        );
    }

    /*
     * If the host block itself disappears — player mining, TNT, etc. —
     * the facade is also dropped.
     *
     * A chunk unload does NOT drop it because the block at this position
     * is still the same block.
     */
    @Inject(
            method = "setRemoved",
            at = @At("HEAD")
    )
    private void omnishape$dropFacadeWithHost(
            CallbackInfo ci
    ) {
        if (omnishape$facade == null) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState current =
                serverLevel.getBlockState(
                        getBlockPos()
                );

        if (current.is(
                getBlockState().getBlock()
        )) {
            return;
        }

        Block.popResource(
                serverLevel,
                getBlockPos(),
                omnishape$facade.createItem()
        );

        omnishape$facade = null;
    }
}