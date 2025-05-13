package dev.omnishape.registry;

import dev.omnishape.Omnishape;
import dev.omnishape.menu.OmnibenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class OmnishapeMenus {
    public static MenuType<OmnibenchMenu> OMNIBENCH_MENU;

    public static void register() {
        OMNIBENCH_MENU = Registry.register(
                BuiltInRegistries.MENU,
                ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "omnibench"),
                new MenuType<>(OmnibenchMenu::new, FeatureFlags.DEFAULT_FLAGS)
        );
    }
}