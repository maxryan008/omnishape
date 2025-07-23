package dev.omnishape;

import net.minecraft.resources.ResourceLocation;

public interface Constant {
    String MOD_ID = "omnishape";

    static ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
    }

    interface Block {
        ResourceLocation OMNIBENCH = id("omnibench");
        ResourceLocation FRAME_BLOCK = id("frame_block");
    }

    interface Model {
        ResourceLocation FRAME_BLOCK = id("block/frame_block");
        ResourceLocation FRAME_ITEM = id("item/frame_block");
    }

    interface Nbt {
        String CORNERS = "Corners";
        String CAMO = "Camo";
        String PITCH = "Pitch";
        String YAW = "Yaw";
        String ROLL = "Roll";
        String X = "x";
        String Y = "y";
        String Z = "z";
    }
}
