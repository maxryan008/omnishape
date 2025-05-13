package dev.omnishape.registry;

import dev.omnishape.Omnishape;
import dev.omnishape.block.OmnibenchBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class OmnishapeBlocks {
    public static final Block OMNIBENCH = new OmnibenchBlock();

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "omnibench"), OMNIBENCH);
        Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "omnibench"), new BlockItem(OMNIBENCH, new Item.Properties()));
    }
}
