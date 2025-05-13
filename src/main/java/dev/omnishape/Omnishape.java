package dev.omnishape;

import dev.omnishape.registry.OmnishapeBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class Omnishape implements ModInitializer {

    public static final String MOD_ID = "omnishape";

    @Override
    public void onInitialize() {
        OmnishapeBlocks.register();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(OmnishapeBlocks.OMNIBENCH);
        });
    }
}
