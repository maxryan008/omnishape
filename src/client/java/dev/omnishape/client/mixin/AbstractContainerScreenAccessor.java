package dev.omnishape.client.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("draggingItem")
    ItemStack getDraggingItem();

    @Accessor("isSplittingStack")
    boolean isSplittingStack();

    @Accessor("quickCraftingRemainder")
    int getQuickCraftingRemainder();

    @Accessor("snapbackItem")
    ItemStack getSnapbackItem();

    @Accessor("snapbackItem")
    void setSnapbackItem(ItemStack stack);

    @Accessor("snapbackTime")
    long getSnapbackTime();

    @Accessor("snapbackEnd")
    Slot getSnapbackEnd();

    @Accessor("snapbackStartX")
    int getSnapbackStartX();

    @Accessor("snapbackStartY")
    int getSnapbackStartY();

    @Invoker("renderFloatingItem")
    void callRenderFloatingItem(GuiGraphics graphics, ItemStack stack, int x, int y, String countText);
}