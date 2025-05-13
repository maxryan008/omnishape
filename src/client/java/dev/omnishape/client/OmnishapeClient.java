package dev.omnishape.client;

import dev.omnishape.client.gui.OmnibenchScreen;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class OmnishapeClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(OmnishapeMenus.OMNIBENCH_MENU, OmnibenchScreen::new);
    }
}
