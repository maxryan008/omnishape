package dev.omnishape;

import dev.omnishape.network.OmnishapePackets;
import dev.omnishape.registry.OmnishapeBlockEntities;
import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.impl.item.group.ItemGroupExtensions;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.List;

public class Omnishape implements ModInitializer {

    public static final String MOD_ID = "omnishape";

    @Override
    public void onInitialize() {
        OmnishapeBlocks.register();
        OmnishapeBlockEntities.register();
        OmnishapeMenus.register();
        OmnishapePackets.registerC2SPackets();

        CreativeModeTab.TAB_MISC.fillItemList(NonNullList.of(OmnishapeBlocks.OMNIBENCH.asItem().getDefaultInstance()));
        CreativeModeTab.TAB_BUILDING_BLOCKS.fillItemList(NonNullList.of(OmnishapeBlocks.FRAME_BLOCK.asItem().getDefaultInstance()));
    }
}
