package dev.omnishape.registry;

import dev.omnishape.Constant;
import dev.omnishape.block.FrameBlock;
import dev.omnishape.block.OmnibenchBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class OmnishapeBlocks {
    public static final Block OMNIBENCH = new OmnibenchBlock();
    public static final Block FRAME_BLOCK = new FrameBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(2F).sound(SoundType.METAL).noOcclusion().forceSolidOn());

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, Constant.Block.OMNIBENCH, OMNIBENCH);
        Registry.register(BuiltInRegistries.ITEM, Constant.Block.OMNIBENCH, new BlockItem(OMNIBENCH, new Item.Properties()));

        Registry.register(BuiltInRegistries.BLOCK, Constant.Block.FRAME_BLOCK, FRAME_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, Constant.Block.FRAME_BLOCK, new BlockItem(FRAME_BLOCK, new Item.Properties()));
    }
}
