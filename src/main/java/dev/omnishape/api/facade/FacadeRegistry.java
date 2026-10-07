package dev.omnishape.api.facade;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class FacadeRegistry {

    private static final List<Predicate<BlockState>> ENTRIES =
            new CopyOnWriteArrayList<>();

    private FacadeRegistry() {
    }

    /**
     * Registers one specific block as accepting OmniShape facades.
     *
     * Typical third-party usage:
     *
     * FacadeRegistry.register(ModBlocks.CABLE);
     */
    public static void register(Block block) {
        register(state -> state.is(block));
    }

    /**
     * Registers an arbitrary block-state predicate.
     *
     * This is useful for mods with several related cable/pipe blocks:
     *
     * FacadeRegistry.register(state -> state.is(ModTags.CABLES));
     */
    public static void register(Predicate<BlockState> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException(
                    "Facade predicate cannot be null"
            );
        }

        ENTRIES.add(predicate);
    }

    public static boolean supports(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }

        for (Predicate<BlockState> predicate : ENTRIES) {
            if (predicate.test(state)) {
                return true;
            }
        }

        return false;
    }
}