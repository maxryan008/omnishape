package dev.omnishape.client.gui;

import dev.omnishape.menu.OmnibenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.awt.*;

public class OmnibenchScreen extends AbstractContainerScreen<OmnibenchMenu> {
    private AbstractSliderButton detailSlider;

    public OmnibenchScreen(OmnibenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        // Set GUI to bottom-left with margin
        // Set the GUI size to 90% of screen
        this.imageWidth = (int) (this.width * 0.9);
        this.imageHeight = (int) (this.height * 0.9);

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        super.init();

        // Slider top-left corner, just under title
        int sliderX = 20;
        int sliderY = 25;

        detailSlider = new AbstractSliderButton(sliderX, sliderY, 100, 20,
                Component.literal("Detail: 1"), 0f) {
            @Override
            protected void updateMessage() {
                int level = (int)(value * 3 + 1);
                setMessage(Component.literal("Detail: " + level));
            }

            @Override
            protected void applyValue() {
                int level = (int)(value * 3 + 1);
                // TODO: Apply level to your block config (if needed)
            }
        };
        this.addRenderableWidget(detailSlider);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 0, 0, 0xFFFFFF);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 0, imageHeight - 100, 0xFFFFFF);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        //empty
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}