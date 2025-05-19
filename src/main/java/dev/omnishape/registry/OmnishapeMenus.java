package dev.omnishape.registry;

import dev.omnishape.Omnishape;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.menu.OmnibenchMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OmnishapeMenus {
    public static ExtendedScreenHandlerType<OmnibenchMenu, BlockPos> OMNIBENCH_MENU;

    public static void register() {
        OMNIBENCH_MENU = new ExtendedScreenHandlerType<>(
                (syncId, inventory, pos) -> {
                    BlockEntity be = inventory.player.level().getBlockEntity(pos);
                    if (be instanceof OmnibenchBlockEntity omnibench) {
                        return new OmnibenchMenu(syncId, inventory, omnibench);
                    }
                    return new OmnibenchMenu(syncId, inventory); // fallback
                },
                BlockPos.STREAM_CODEC // built-in codec for BlockPos
        );

        Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "omnibench"), OMNIBENCH_MENU);
    }
}