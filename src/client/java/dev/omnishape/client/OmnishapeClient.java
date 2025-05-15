package dev.omnishape.client;

import dev.omnishape.client.gui.OmnibenchScreen;
import dev.omnishape.client.model.OmnishapeModelLoader;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.gui.screens.MenuScreens;

public class OmnishapeClient implements ClientModInitializer {



    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(OmnishapeModelLoader.INSTANCE);
        MenuScreens.register(OmnishapeMenus.OMNIBENCH_MENU, OmnibenchScreen::new);
    }
}
