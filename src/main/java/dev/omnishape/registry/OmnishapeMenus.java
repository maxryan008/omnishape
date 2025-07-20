package dev.omnishape.registry;

import dev.omnishape.Constant;
import dev.omnishape.Omnishape;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.menu.OmnibenchMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntity;

public class OmnishapeMenus {
    public static ExtendedScreenHandlerType<OmnibenchMenu> OMNIBENCH_MENU;

    public static void register() {
        OMNIBENCH_MENU = new ExtendedScreenHandlerType<>(
                (syncId, inventory, buf) -> {
                    BlockEntity be = inventory.player.level.getBlockEntity(buf.readBlockPos());
                    if (be instanceof OmnibenchBlockEntity omnibench) {
                        return new OmnibenchMenu(syncId, inventory, omnibench);
                    }
                    return new OmnibenchMenu(syncId, inventory); // fallback
                }
        );

        Registry.register(Registry.MENU, Constant.id("omnibench"), OMNIBENCH_MENU);
    }
}