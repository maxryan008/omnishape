package dev.omnishape.client.gui;

import dev.omnishape.Omnishape;
import dev.omnishape.menu.OmnibenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.awt.*;

public class OmnibenchScreen extends AbstractContainerScreen<OmnibenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "textures/gui/omnibench_background.png");

    private final int textureWidth = 320;
    private final int textureHeight = 240;

    public OmnibenchScreen(OmnibenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        float scale = getSlotScale();

        this.imageWidth = textureWidth;
        this.imageHeight = textureHeight;

        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        super.init();

        int sliderX = this.leftPos + 191;
        int sliderY = this.topPos + 178;
        int sliderWidth = 100;
        int sliderHeight = 18;

        AbstractSliderButton detailSlider = new AbstractSliderButton(sliderX, sliderY, sliderWidth, sliderHeight,
                Component.literal("Detail: 1"), 0f) {
            @Override
            protected void updateMessage() {
                int level = (int) (value * 3 + 1);
                setMessage(Component.literal("Detail: " + level));
            }

            @Override
            protected void applyValue() {
                int level = (int) (value * 3 + 1);
                // TODO: Apply level to your config
            }
        };
        this.addRenderableWidget(detailSlider);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawCenteredText(guiGraphics, this.title, 241, 168);
        drawCenteredText(guiGraphics, Component.literal("Camouflage Block"), 241, 208);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int fixedX, int fixedY) {
        int textWidth = this.font.width(text);
        int textX = fixedX - (textWidth / 2);
        int textY = fixedY - (this.font.lineHeight / 2); // optional: vertical centering

        guiGraphics.drawString(this.font, text, textX, textY, 0xFFFFFF, false);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderBackground(guiGraphics, i, j, f);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        guiGraphics.blit(
                TEXTURE,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight,
                textureWidth, textureHeight
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private float getSlotScale() {
        float scale = Math.min(
                this.width / (textureWidth + 20.0f),  // +margin
                this.height / (textureHeight + 20.0f)
        );
        return Math.min(scale, 1.0f); // never upscale past 1x
    }
}