package dev.omnishape.api.facade;

import dev.omnishape.network.packet.FacadeSyncS2CPacket;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FacadeManager {

    private FacadeManager() {
    }

    /**
     * Fast path intended for hot block-query code.
     *
     * Avoids allocating an Optional every time Minecraft asks for a block
     * shape, collision shape, mining speed, raycast, etc.
     */
    @Nullable
    public static FacadeData getFacadeOrNull(
            BlockGetter level,
            BlockPos pos
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FacadeHolder holder)) {
            return null;
        }

        return holder.omnishape$getFacade();
    }

    /**
     * Public convenience API for callers where Optional is useful.
     */
    public static Optional<FacadeData> getFacade(
            BlockGetter level,
            BlockPos pos
    ) {
        return Optional.ofNullable(
                getFacadeOrNull(
                        level,
                        pos
                )
        );
    }

    public static boolean hasFacade(
            BlockGetter level,
            BlockPos pos
    ) {
        return getFacadeOrNull(
                level,
                pos
        ) != null;
    }

    public static boolean canAcceptFacade(
            Level level,
            BlockPos pos
    ) {
        BlockState state =
                level.getBlockState(
                        pos
                );

        if (!FacadeRegistry.supports(state)) {
            return false;
        }

        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FacadeHolder holder)) {
            return false;
        }

        return !holder.omnishape$hasFacade();
    }

    public static boolean setFacade(
            Level level,
            BlockPos pos,
            FacadeData facade
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FacadeHolder holder)) {
            return false;
        }

        if (!FacadeRegistry.supports(
                level.getBlockState(
                        pos
                )
        )) {
            return false;
        }

        holder.omnishape$setFacade(
                facade
        );

        notifyChanged(
                level,
                pos,
                blockEntity
        );

        if (level instanceof ServerLevel serverLevel) {
            sync(
                    serverLevel,
                    pos,
                    facade
            );
        }

        return true;
    }

    public static boolean removeFacade(
            Level level,
            BlockPos pos,
            boolean drop
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FacadeHolder holder)) {
            return false;
        }

        FacadeData facade =
                holder.omnishape$getFacade();

        if (facade == null) {
            return false;
        }

        holder.omnishape$clearFacade();

        if (!level.isClientSide() && drop) {
            ItemStack stack =
                    facade.createItem();

            Block.popResource(
                    level,
                    pos,
                    stack
            );
        }

        notifyChanged(
                level,
                pos,
                blockEntity
        );

        if (level instanceof ServerLevel serverLevel) {
            sync(
                    serverLevel,
                    pos,
                    null
            );
        }

        return true;
    }

    /**
     * Client-side application of authoritative server facade data.
     */
    public static void applyClientSync(
            Level level,
            BlockPos pos,
            @Nullable FacadeData facade
    ) {
        BlockEntity blockEntity =
                level.getBlockEntity(
                        pos
                );

        if (!(blockEntity instanceof FacadeHolder holder)) {
            return;
        }

        holder.omnishape$setFacade(
                facade
        );

        notifyChanged(
                level,
                pos,
                blockEntity
        );
    }

    private static void notifyChanged(
            Level level,
            BlockPos pos,
            BlockEntity blockEntity
    ) {
        blockEntity.setChanged();

        BlockState state =
                level.getBlockState(
                        pos
                );

        level.sendBlockUpdated(
                pos,
                state,
                state,
                Block.UPDATE_ALL
        );

        level.getChunkSource()
                .getLightEngine()
                .checkBlock(
                        pos
                );
    }

    private static void sync(
            ServerLevel level,
            BlockPos pos,
            @Nullable FacadeData facade
    ) {
        FacadeSyncS2CPacket packet =
                new FacadeSyncS2CPacket(
                        pos,
                        facade
                );

        PlayerLookup.tracking(
                level,
                pos
        ).forEach(
                player ->
                        ServerPlayNetworking.send(
                                player,
                                packet
                        )
        );
    }
}