package dev.omnishape.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.omnishape.Omnishape;
import dev.omnishape.menu.OmnibenchMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class OmnibenchScreen extends AbstractContainerScreen<OmnibenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "textures/gui/omnibench_background.png");

    private static final int TEXTURE_WIDTH = 320;
    private static final int TEXTURE_HEIGHT = 240;

    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 10;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 142;

    private float rotX = 30f;
    private float rotY = 45f;

    private boolean dragging = false;
    private double lastMouseX, lastMouseY;

    public OmnibenchScreen(OmnibenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    protected void init() {
        this.imageWidth = TEXTURE_WIDTH;
        this.imageHeight = TEXTURE_HEIGHT;

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
                TEXTURE_WIDTH, TEXTURE_HEIGHT
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderCube(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        super.slotClicked(slot, i, j, clickType);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isInRenderPanel(mouseX, mouseY)) {
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging) {
            rotY += (float) (mouseX - lastMouseX);
            rotX += (float) (mouseY - lastMouseY);

            // Clamp X rotation to avoid flipping
            rotX = Mth.clamp(rotX, -90f, 90f);

            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    private boolean isInRenderPanel(double mouseX, double mouseY) {
        int screenX = this.leftPos + PANEL_X;
        int screenY = this.topPos + PANEL_Y;

        return mouseX >= screenX && mouseX <= screenX + PANEL_WIDTH &&
                mouseY >= screenY && mouseY <= screenY + PANEL_HEIGHT;
    }

    @Override
    public void mouseMoved(double d, double e) {
        super.mouseMoved(d, e);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderCube(GuiGraphics gui) {
        ItemStack stack = menu.getSlot(0).getItem();
        Block block = Blocks.IRON_BLOCK; // fallback

        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
            Block candidate = blockItem.getBlock();
            BlockState state = candidate.defaultBlockState();

            if (state.isSolidRender(Minecraft.getInstance().level, BlockPos.ZERO)) {
                block = candidate;
            }
        }

        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);

        TextureAtlasSprite sprite = model.getParticleIcon();

        PoseStack pose = gui.pose();
        pose.pushPose();

        float centerX = this.leftPos + 160;
        float centerY = this.topPos + 81;

        pose.translate(centerX, centerY, 40);
        pose.scale(40f, -40f, 40f); // flip Y for GUI space
        pose.mulPose(Axis.XP.rotationDegrees(-rotX));
        pose.mulPose(Axis.YP.rotationDegrees(-rotY));

        gui.flush(); // flush previous buffer before using direct draw

        RenderSystem.setShaderTexture(0, sprite.atlasLocation());

        VertexConsumer buffer = gui.bufferSource().getBuffer(RenderType.solid());
        Matrix4f mat = pose.last().pose();

        float u1 = sprite.getU0();
        float u2 = sprite.getU1();
        float v1 = sprite.getV0();
        float v2 = sprite.getV1();

        drawFace(buffer, mat, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, u1, v1, u2, v2); // Front face
        drawFace(buffer, mat, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, u1, v1, u2, v2); // Back face
        drawFace(buffer, mat, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, u1, v1, u2, v2); // Left face
        drawFace(buffer, mat, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, u1, v1, u2, v2); // Right face
        drawFace(buffer, mat, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, u1, v1, u2, v2); // Top face
        drawFace(buffer, mat, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, u1, v1, u2, v2); // Bottom face

        pose.popPose();

        // Ensure it's flushed (if necessary)
        gui.flush();
    }

    private void drawFace(VertexConsumer buffer, Matrix4f mat,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float u1, float v1, float u2, float v2) {
        buffer.addVertex(mat, x1 - 0.5f, y1 - 0.5f, z1 - 0.5f).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x2 - 0.5f, y2 - 0.5f, z2 - 0.5f).setColor(1f, 1f, 1f, 1f).setUv(u2, v1).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x3 - 0.5f, y3 - 0.5f, z3 - 0.5f).setColor(1f, 1f, 1f, 1f).setUv(u2, v2).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x4 - 0.5f, y4 - 0.5f, z4 - 0.5f).setColor(1f, 1f, 1f, 1f).setUv(u1, v2).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
    }
}