package dev.omnishape;

import dev.omnishape.network.OmnishapePackets;
import dev.omnishape.registry.OmnishapeBlockEntities;
import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeComponents;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;

public class Omnishape implements ModInitializer {
    @Override
    public void onInitialize() {
        OmnishapeBlocks.register();
        OmnishapeBlockEntities.register();
        OmnishapeMenus.register();
        OmnishapeComponents.register();
        OmnishapePackets.registerC2SPackets();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.accept(OmnishapeBlocks.OMNIBENCH);
        });

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(OmnishapeBlocks.FRAME_BLOCK);
        });
    }
}
