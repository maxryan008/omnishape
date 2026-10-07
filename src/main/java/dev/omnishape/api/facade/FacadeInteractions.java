package dev.omnishape.api.facade;

import dev.omnishape.BlockRotation;
import dev.omnishape.api.OmnishapeData;
import dev.omnishape.registry.OmnishapeBlocks;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

public final class FacadeInteractions {

    private FacadeInteractions() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(
                (player, level, hand, hitResult) -> {
                    ItemStack held =
                            player.getItemInHand(hand);

                    if (!held.is(
                            OmnishapeBlocks.FRAME_BLOCK.asItem()
                    )) {
                        return InteractionResult.PASS;
                    }

                    if (!OmnishapeData.canExtractFromItem(
                            held
                    )) {
                        return InteractionResult.PASS;
                    }

                    if (!FacadeManager.canAcceptFacade(
                            level,
                            hitResult.getBlockPos()
                    )) {
                        return InteractionResult.PASS;
                    }

                    /*
                     * Client consumes the interaction so the underlying cable
                     * does not also open a GUI / toggle state.
                     *
                     * The actual facade state remains server-authoritative.
                     */
                    if (level.isClientSide()) {
                        return InteractionResult.SUCCESS;
                    }

                    UseOnContext useContext =
                            new UseOnContext(
                                    player,
                                    hand,
                                    hitResult
                            );

                    BlockPlaceContext placementContext =
                            new BlockPlaceContext(
                                    useContext
                            );

                    BlockRotation rotation =
                            BlockRotation.fromPlacementContext(
                                    placementContext
                            );

                    OmnishapeData shape =
                            OmnishapeData.extractFromItem(
                                    held
                            );

                    FacadeData facade =
                            new FacadeData(
                                    shape,
                                    rotation
                            );

                    if (!FacadeManager.setFacade(
                            level,
                            hitResult.getBlockPos(),
                            facade
                    )) {
                        return InteractionResult.PASS;
                    }

                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }

                    return InteractionResult.SUCCESS;
                }
        );
    }
}