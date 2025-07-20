package dev.omnishape.registry;

import dev.omnishape.Omnishape;
import dev.omnishape.block.FrameBlock;
import dev.omnishape.block.OmnibenchBlock;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class OmnishapeBlocks {
    public static final Block OMNIBENCH = new OmnibenchBlock();
    public static final Block FRAME_BLOCK = new FrameBlock(BlockBehaviour.Properties.of(Material.METAL).color(MaterialColor.COLOR_LIGHT_GRAY).strength(2F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());

    public static void register() {
        Registry.register(Registry.BLOCK, new ResourceLocation(Omnishape.MOD_ID, "omnibench"), OMNIBENCH);
        Registry.register(Registry.ITEM, new ResourceLocation(Omnishape.MOD_ID, "omnibench"), new BlockItem(OMNIBENCH, new Item.Properties()));

        Registry.register(Registry.BLOCK, new ResourceLocation(Omnishape.MOD_ID, "frame_block"), FRAME_BLOCK);
        Registry.register(Registry.ITEM, new ResourceLocation(Omnishape.MOD_ID, "frame_block"), new BlockItem(FRAME_BLOCK, new Item.Properties()));
    }
}
