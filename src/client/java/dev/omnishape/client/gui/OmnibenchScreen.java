package dev.omnishape.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.omnishape.Omnishape;
import dev.omnishape.client.TextureUtils;
import dev.omnishape.client.mixin.AbstractContainerScreenAccessor;
import dev.omnishape.client.mixin.ScreenAccessor;
import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.registry.OmnishapeBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;

public class OmnibenchScreen extends AbstractContainerScreen<OmnibenchMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Omnishape.MOD_ID, "textures/gui/omnibench_background.png");

    private static final int TEXTURE_WIDTH = 320;
    private static final int TEXTURE_HEIGHT = 240;

    private static final int PANEL_X = 10;
    private static final int PANEL_Y = 10;
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 142;
    private final Vector4f[] projectedCorners = new Vector4f[8];
    private float rotX = 30f;
    private float rotY = 45f;
    private boolean dragging = false;
    private double lastMouseX, lastMouseY;
    // Which corner (0–7) is currently selected
    private int selectedCorner = -1;

    // Axis being dragged: 0=X, 1=Y, 2=Z, -1=none
    private int draggingAxis = -1;
    private Matrix4f lastMatrix = null;

    private EditBox xInput, yInput, zInput;
    private Vector3f dragStartCorner = null;
    private double dragStartMouseX = 0;
    private double dragStartMouseY = 0;
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
        int sliderY = this.topPos + 174;
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

        xInput = new EditBox(this.font, leftPos + 30, topPos + 20, 50, 14, Component.literal("X"));
        yInput = new EditBox(this.font, leftPos + 30, topPos + 38, 50, 14, Component.literal("Y"));
        zInput = new EditBox(this.font, leftPos + 30, topPos + 56, 50, 14, Component.literal("Z"));

        xInput.setResponder(str -> updateCornerFromText());
        yInput.setResponder(str -> updateCornerFromText());
        zInput.setResponder(str -> updateCornerFromText());

        xInput.setFilter(s -> s.matches("[-+]?[0-9]*\\.?[0-9]*"));
        yInput.setFilter(s -> s.matches("[-+]?[0-9]*\\.?[0-9]*"));
        zInput.setFilter(s -> s.matches("[-+]?[0-9]*\\.?[0-9]*"));

        addRenderableWidget(xInput);
        addRenderableWidget(yInput);
        addRenderableWidget(zInput);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawCenteredText(guiGraphics, this.title, this.leftPos + 241, this.topPos + 168);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int fixedX, int fixedY) {
        drawCenteredText(guiGraphics, text, fixedX, fixedY, 0xFFFFFF);
    }

    private void drawCenteredText(GuiGraphics guiGraphics, Component text, int fixedX, int fixedY, int colour) {
        int textWidth = this.font.width(text);
        int textX = fixedX - (textWidth / 2);
        int textY = fixedY - (this.font.lineHeight / 2); // optional: vertical centering

        guiGraphics.drawString(this.font, text, textX, textY, colour, false);
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
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (xInput.charTyped(chr, modifiers) || yInput.charTyped(chr, modifiers) || zInput.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (xInput.keyPressed(keyCode, scanCode, modifiers) || yInput.keyPressed(keyCode, scanCode, modifiers) || zInput.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.renderBg(guiGraphics, partialTick, mouseX, mouseY); // Draw your background PNG
        this.renderLabels(guiGraphics, mouseX, mouseY); // Draw titles

        // Draw slots and items (copied from your overridden render above)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.leftPos, this.topPos, 0.0F);
        this.hoveredSlot = null;

        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.isActive()) {
                this.renderSlot(guiGraphics, slot);
            }

            if (this.isHovering(slot, mouseX, mouseY) && slot.isActive()) {
                this.hoveredSlot = slot;
                if (slot.isHighlightable()) {
                    renderSlotHighlight(guiGraphics, slot.x, slot.y, 0);
                }
            }
        }
        guiGraphics.pose().popPose();

        // Draw corner editor background panel last (so it's on top)
        if (selectedCorner >= 0) {
            int boxX = leftPos + 8;
            int boxY = topPos + 8;
            int boxW = 75;
            int boxH = 64;

            int borderColor = switch (draggingAxis) {
                case 0 -> 0xFFFF5555; // red
                case 1 -> 0xFF55FF55; // green
                case 2 -> 0xFF5555FF; // blue
                default -> 0xFFAAAAAA; // gray
            };

            guiGraphics.fill(boxX - 2, boxY - 2, boxX + boxW + 2, boxY + boxH + 2, 0xFF000000);
            guiGraphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xFF222222);
            drawBorder(guiGraphics, boxX, boxY, boxW, boxH, borderColor);
            drawCenteredText(guiGraphics, Component.literal("Corner Editor"), boxX + 38, boxY + 7);
            drawCenteredText(guiGraphics, Component.literal("X"), boxX + 12, boxY + 20, 0xFFFF0000);
            drawCenteredText(guiGraphics, Component.literal("Y"), boxX + 12, boxY + 38, 0xFF00FF00);
            drawCenteredText(guiGraphics, Component.literal("Z"), boxX + 12, boxY + 56, 0xFF0000FF);
        }

        renderCube(guiGraphics); // 3D mesh last to Z-buffer properly

        // Manually render EditBoxes last so they’re on top
        if (selectedCorner >= 0) {
            xInput.render(guiGraphics, mouseX, mouseY, partialTick);
            yInput.render(guiGraphics, mouseX, mouseY, partialTick);
            zInput.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        AbstractContainerScreenAccessor accessorAbstract = (AbstractContainerScreenAccessor) this;
        ScreenAccessor accessorScreen = (ScreenAccessor) this;

        // Restore floating dragged item rendering (same as in AbstractContainerScreen)
        ItemStack itemStack = accessorAbstract.getDraggingItem().isEmpty() ? this.menu.getCarried() : accessorAbstract.getDraggingItem();
        if (!itemStack.isEmpty()) {
            int offset = accessorAbstract.getDraggingItem().isEmpty() ? 8 : 16;
            String label = null;

            if (!accessorAbstract.getDraggingItem().isEmpty() && accessorAbstract.isSplittingStack()) {
                itemStack = itemStack.copyWithCount(Mth.ceil(itemStack.getCount() / 2.0F));
            } else if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
                itemStack = itemStack.copyWithCount(accessorAbstract.getQuickCraftingRemainder());
                if (itemStack.isEmpty()) {
                    label = ChatFormatting.YELLOW + "0";
                }
            }

            accessorAbstract.callRenderFloatingItem(guiGraphics, itemStack, mouseX - 8, mouseY - offset, label);
        }

        // Restore snapback animation rendering
        if (!accessorAbstract.getSnapbackItem().isEmpty()) {
            float time = (float) (Util.getMillis() - accessorAbstract.getSnapbackTime()) / 100.0F;
            if (time >= 1.0F) {
                time = 1.0F;
                accessorAbstract.setSnapbackItem(ItemStack.EMPTY);
            }

            int dx = accessorAbstract.getSnapbackEnd().x - accessorAbstract.getSnapbackStartX();
            int dy = accessorAbstract.getSnapbackEnd().y - accessorAbstract.getSnapbackStartY();
            int x = accessorAbstract.getSnapbackStartX() + (int) (dx * time);
            int y = accessorAbstract.getSnapbackStartY() + (int) (dy * time);
            accessorAbstract.callRenderFloatingItem(guiGraphics, accessorAbstract.getSnapbackItem(), x, y, null);
        }

        // Manually render selected widgets like the slider
        for (var widget : accessorScreen.getRenderables()) {
            // Only render if visible
            if (widget instanceof AbstractSliderButton slider && slider.visible) {
                slider.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void slotClicked(Slot slot, int i, int j, ClickType clickType) {
        super.slotClicked(slot, i, j, clickType);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (xInput.mouseClicked(mouseX, mouseY, button)) {
            setFocused(xInput);
            xInput.setFocused(true);
            yInput.setFocused(false);
            zInput.setFocused(false);
            return true;
        } else if (yInput.mouseClicked(mouseX, mouseY, button)) {
            setFocused(yInput);
            yInput.setFocused(true);
            xInput.setFocused(false);
            zInput.setFocused(false);
            return true;
        } else if (zInput.mouseClicked(mouseX, mouseY, button)) {
            setFocused(zInput);
            zInput.setFocused(true);
            xInput.setFocused(false);
            yInput.setFocused(false);
            return true;
        }

        if (button == 0 && isInRenderPanel(mouseX, mouseY)) {
            xInput.setFocused(false);
            yInput.setFocused(false);
            zInput.setFocused(false);
            for (int i = 0; i < projectedCorners.length; i++) {
                if (selectedCorner >= 0) {
                    Vector3f base = new Vector3f(menu.getCorners()[selectedCorner]).sub(0.5f, 0.5f, 0.5f);
                    float arrowLength = 0.3f;

                    Matrix4f mat = lastMatrix; // extract this from renderCube or cache it

                    if (mat == null) {
                        continue;
                    }

                    Vector4f baseScreen = new Vector4f(base, 1f).mul(mat);
                    Vector4f xScreen = new Vector4f(base.x + arrowLength, base.y, base.z, 1f).mul(mat);
                    Vector4f yScreen = new Vector4f(base.x, base.y - arrowLength, base.z, 1f).mul(mat);
                    Vector4f zScreen = new Vector4f(base.x, base.y, base.z + arrowLength, 1f).mul(mat);

                    if (isMouseInsideAxis(mouseX, mouseY, baseScreen.x(), baseScreen.y(), xScreen.x(), xScreen.y(), 2f)) {
                        draggingAxis = 0;
                        dragStartCorner = new Vector3f(menu.getCorners()[selectedCorner]);
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        return true;
                    }
                    if (isMouseInsideAxis(mouseX, mouseY, baseScreen.x(), -baseScreen.y(), yScreen.x(), yScreen.y(), 2f)) {
                        draggingAxis = 1;
                        dragStartCorner = new Vector3f(menu.getCorners()[selectedCorner]);
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        return true;
                    }
                    if (isMouseInsideAxis(mouseX, mouseY, baseScreen.x(), baseScreen.y(), zScreen.x(), zScreen.y(), 2f)) {
                        draggingAxis = 2;
                        dragStartCorner = new Vector3f(menu.getCorners()[selectedCorner]);
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        return true;
                    }
                }

                Vector4f screenCorner = projectedCorners[i];
                float screenX = screenCorner.x();
                float screenY = screenCorner.y();
                float screenZ = screenCorner.z();

                // Only allow selecting visible corners (in front of the screen/cube)
                if (screenZ < 10 || screenZ > 100) continue; // too far or behind

                // Mini cube is 0.1 units in 3D space, scaled by 40px
                float halfSize = 0.1f * 40f / 2f;

                float left = screenX - halfSize;
                float right = screenX + halfSize;
                float top = screenY - halfSize;
                float bottom = screenY + halfSize;

                if (mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom) {
                    selectedCorner = (selectedCorner == i) ? -1 : i; // toggle selection
                    draggingAxis = -1;

                    syncCornerToTextFields();
                    return true;
                }
            }

            // If not clicking on any visible corner, allow dragging
            dragging = true;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingAxis >= 0 && selectedCorner >= 0 && dragStartCorner != null && lastMatrix != null) {
            // Determine world axis direction
            Vector3f axis = switch (draggingAxis) {
                case 0 -> new Vector3f(1, 0, 0);
                case 1 -> new Vector3f(0, 1, 0);
                default -> new Vector3f(0, 0, 1);
            };

            // Project axis into screen space (from origin)
            Vector4f axisScreen = new Vector4f(axis, 0).mul(lastMatrix);
            Vector2f screenAxis = new Vector2f(axisScreen.x(), axisScreen.y()).normalize();

            // Convert mouse drag to screen-space movement vector
            Vector2f mouseDelta = new Vector2f((float) dx, (float) dy); // Y is inverted in screen space

            // Project mouse delta onto axis screen direction
            float movementAmount = mouseDelta.dot(screenAxis) / 40f;

            // Apply movement
            Vector3f current = menu.getCorners()[selectedCorner];
            if (draggingAxis == 0)
                current.x = Mth.clamp(current.x + movementAmount, 0f, 1f);
            else if (draggingAxis == 1)
                current.y = Mth.clamp(current.y + movementAmount, 0f, 1f);
            else
                current.z = Mth.clamp(current.z + movementAmount, 0f, 1f);

            menu.getBlockEntity().setCorner(selectedCorner, current);

            dragStartCorner.set(current);
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;

            syncCornerToTextFields();

            return true;
        }

        if (dragging) {
            rotY += (float) dx;
            rotX += (float) dy;

            // Clamp X rotation to avoid flipping
            rotX = Mth.clamp(rotX, -90f, 90f);

            lastMouseX = mouseX;
            lastMouseY = mouseY;

            syncCornerToTextFields();

            return true;
        }

        syncCornerToTextFields();

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
        if (button == 0) {
            dragging = false;
            draggingAxis = -1;
            dragStartCorner = null;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderCube(GuiGraphics gui) {
        ItemStack stack = menu.getItem(OmnibenchMenu.CAMO_SLOT);
        Block block = OmnishapeBlocks.FRAME_BLOCK; // fallback

        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
            Block candidate = blockItem.getBlock();
            BlockState state = candidate.defaultBlockState();

            if (state.isSolidRender(Minecraft.getInstance().level, BlockPos.ZERO)) {
                block = candidate;
            }
        }

        BlockState state = block.defaultBlockState();

        HashMap<Direction, TextureAtlasSprite> camoSprites = TextureUtils.GetCamoSprites(state);

        for (Direction dir : Direction.values()) {
            if (camoSprites.get(dir) == null) {
                camoSprites.put(dir, Minecraft.getInstance().getBlockRenderer().getBlockModel(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState()).getQuads(OmnishapeBlocks.FRAME_BLOCK.defaultBlockState(), dir, RandomSource.create()).getFirst().getSprite());
            }
        }

        PoseStack pose = gui.pose();
        pose.pushPose();

        float centerX = this.leftPos + 160;
        float centerY = this.topPos + 81;

        pose.translate(centerX, centerY, 40);
        pose.scale(40f, 40f, 40f); // flip Y for GUI space
        pose.mulPose(Axis.XP.rotationDegrees(-rotX));
        pose.mulPose(Axis.XP.rotationDegrees(180));
        pose.mulPose(Axis.YP.rotationDegrees(-rotY));

        gui.flush(); // flush previous buffer before using direct draw

        RenderSystem.enableDepthTest();

        VertexConsumer buffer = gui.bufferSource().getBuffer(RenderType.solid());
        Matrix4f mat = pose.last().pose();
        lastMatrix = mat;

        // Each face: {corner0, corner1, corner2, corner3}, with matching canonical UVs: (0,0) → (1,0) → (1,1) → (0,1)
        int[][] faces = {
                {4, 5, 1, 0}, // TOP (Y+)
                {2, 3, 7, 6}, // BOTTOM (Y-)
                {1, 3, 2, 0}, // NORTH (Z-)
                {6, 7, 5, 4}, // SOUTH (Z+)
                {0, 2, 6, 4}, // WEST (X-)
                {5, 7, 3, 1}  // EAST (X+)
        };

        // For each face, define which corner components map to U/V axes.
        // Format: [U axis index, V axis index] — where 0=x, 1=y, 2=z
        int[][] uvAxes = {
                {0, 2}, // BACK   → X,Y
                {0, 2}, // FRONT  → X,Y
                {0, 1}, // TOP    → X,Z
                {0, 1}, // BOTTOM → X,Z
                {2, 1}, // LEFT   → Z,Y
                {2, 1}  // RIGHT  → Z,Y
        };

        for (Direction dir : Direction.values()) {
            //RenderSystem.setShaderTexture(0, camoSprites.get(dir).atlasLocation());
            float minU = camoSprites.get(dir).getU0();
            float maxU = camoSprites.get(dir).getU1();
            float minV = camoSprites.get(dir).getV0();
            float maxV = camoSprites.get(dir).getV1();
            float diffU = maxU - minU;
            float diffV = maxV - minV;

            int[] face = faces[dir.ordinal()];
            int uAxis = uvAxes[dir.ordinal()][0];
            int vAxis = uvAxes[dir.ordinal()][1];

            Vector3f[] vs = new Vector3f[4];
            for (int i = 0; i < 4; i++) {
                // Shift corners to [-0.5, 0.5] centered space
                vs[i] = new Vector3f(menu.getCorners()[face[i]]).sub(0.5f, 0.5f, 0.5f);
            }


            // Calculate bounds in U/V directions
            float minUx = Float.MAX_VALUE, maxUx = -Float.MAX_VALUE;
            float minVy = Float.MAX_VALUE, maxVy = -Float.MAX_VALUE;
            float[] us = new float[4], vs_ = new float[4];

            for (int i = 0; i < 4; i++) {
                float[] xyz = {vs[i].x, vs[i].y, vs[i].z};
                us[i] = xyz[uAxis];
                vs_[i] = xyz[vAxis];

                minUx = Math.min(minUx, us[i]);
                maxUx = Math.max(maxUx, us[i]);
                minVy = Math.min(minVy, vs_[i]);
                maxVy = Math.max(maxVy, vs_[i]);
            }

            float uRange = maxUx - minUx;
            float vRange = maxVy - minVy;
            if (uRange == 0 || vRange == 0) continue; // degenerate

            // Final UV assignment (normalized to [0,1] → scaled to texture)
            Vector2f[] uvs = new Vector2f[4];
            for (int i = 0; i < 4; i++) {
                float normU = (us[i] - minUx) / uRange;
                float normV = (vs_[i] - minVy) / vRange;

                normU = 1.0f - normU;
                normV = 1.0f - normV;

                uvs[i] = new Vector2f(minU + normU * diffU, minV + normV * diffV);
            }

            drawFace(buffer, mat,
                    vs[0].x, vs[0].y, vs[0].z, uvs[0].x, uvs[0].y,
                    vs[1].x, vs[1].y, vs[1].z, uvs[1].x, uvs[1].y,
                    vs[2].x, vs[2].y, vs[2].z, uvs[2].x, uvs[2].y,
                    vs[3].x, vs[3].y, vs[3].z, uvs[3].x, uvs[3].y
            );
        }

        for (int i = 0; i < menu.getCorners().length; i++) {
            Vector3f corner = menu.getCorners()[i];
            Vector4f pos = new Vector4f(corner.x - 0.5125f, corner.y - 0.5125f, corner.z - 0.5125f, 1f);
            renderMiniCube(gui, mat, corner.x - 0.5625f, corner.y - 0.5625f, corner.z - 0.5625f, 0.1f, isSelected(i));
            pos = mat.transform(pos);
            projectedCorners[i] = pos; // save in projected screen space
        }

        pose.popPose();
        RenderSystem.disableDepthTest();

        if (selectedCorner >= 0) {
            renderXYZArrows2D(gui, mat);
        }

        // Ensure it's flushed (if necessary)
        gui.flush();
    }

    private void renderXYZArrows2D(GuiGraphics gui, Matrix4f mat) {
        if (selectedCorner < 0) return;

        if (mat == null) return;

        Vector3f baseCorner = menu.getCorners()[selectedCorner];
        Vector3f base = new Vector3f(baseCorner).sub(0.5f, 0.5f, 0.5f);

        float arrowLength = 0.3f;

        Vector4f baseScreen = new Vector4f(base, 1.0f).mul(mat);
        Vector4f xScreen = new Vector4f(new Vector3f(base).add(arrowLength, 0, 0), 1.0f).mul(mat);
        Vector4f yScreen = new Vector4f(new Vector3f(base).add(0, arrowLength, 0), 1.0f).mul(mat);
        Vector4f zScreen = new Vector4f(new Vector3f(base).add(0, 0, arrowLength), 1.0f).mul(mat);

        float bx = baseScreen.x();
        float by = baseScreen.y();

        drawThickLine(gui, bx, by, xScreen.x(), xScreen.y(), 2f, 0xFFFF0000); // X - red
        drawThickLine(gui, bx, by, yScreen.x(), yScreen.y(), 2f, 0xFF00FF00); // Y - green
        drawThickLine(gui, bx, by, zScreen.x(), zScreen.y(), 2f, 0xFF0000FF); // Z - blue
    }

    private void drawThickLine(GuiGraphics gui, float x1, float y1, float x2, float y2, float thickness, int colorARGB) {
        float[] quad = getAxisQuad(x1, y1, x2, y2, thickness);
        if (quad == null) return;

        float ax = quad[0], ay = quad[1];
        float bx = quad[2], by = quad[3];
        float cx = quad[4], cy = quad[5];
        float dx_ = quad[6], dy_ = quad[7];

        Matrix4f mat = gui.pose().last().pose();
        VertexConsumer buffer = gui.bufferSource().getBuffer(RenderType.guiOverlay());

        int a = FastColor.ARGB32.alpha(colorARGB);
        int r = FastColor.ARGB32.red(colorARGB);
        int g = FastColor.ARGB32.green(colorARGB);
        int b = FastColor.ARGB32.blue(colorARGB);

        buffer.addVertex(mat, ax, ay, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, dx_, dy_, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, cx, cy, 0).setColor(r, g, b, a);
        buffer.addVertex(mat, bx, by, 0).setColor(r, g, b, a);
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

    private void drawFace(VertexConsumer buffer, Matrix4f mat,
                          float x1, float y1, float z1, float u1, float v1,
                          float x2, float y2, float z2, float u2, float v2,
                          float x3, float y3, float z3, float u3, float v3,
                          float x4, float y4, float z4, float u4, float v4) {
        buffer.addVertex(mat, x1, y1, z1).setColor(1f, 1f, 1f, 1f).setUv(u1, v1).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x2, y2, z2).setColor(1f, 1f, 1f, 1f).setUv(u2, v2).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x3, y3, z3).setColor(1f, 1f, 1f, 1f).setUv(u3, v3).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
        buffer.addVertex(mat, x4, y4, z4).setColor(1f, 1f, 1f, 1f).setUv(u4, v4).setUv1(0, 10).setUv2(240, 240).setNormal(0, 0, 0);
    }

    private void drawFace(VertexConsumer buffer, Matrix4f mat,
                          float x1, float y1, float z1,
                          float x2, float y2, float z2,
                          float x3, float y3, float z3,
                          float x4, float y4, float z4,
                          float r, float g, float b) {

        buffer.addVertex(mat, x1, y1, z1).setColor(r, g, b, 1f).setNormal(0, 0, 0).setUv(0, 0).setUv1(0, 10).setUv2(240, 240);
        buffer.addVertex(mat, x2, y2, z2).setColor(r, g, b, 1f).setNormal(0, 0, 0).setUv(0, 0).setUv1(0, 10).setUv2(240, 240);
        buffer.addVertex(mat, x3, y3, z3).setColor(r, g, b, 1f).setNormal(0, 0, 0).setUv(0, 0).setUv1(0, 10).setUv2(240, 240);
        buffer.addVertex(mat, x4, y4, z4).setColor(r, g, b, 1f).setNormal(0, 0, 0).setUv(0, 0).setUv1(0, 10).setUv2(240, 240);
    }

    private void renderMiniCube(GuiGraphics gui, Matrix4f parentMat, float x, float y, float z, float size, boolean selected) {
        Matrix4f mat = new Matrix4f(parentMat);
        mat.translate(x, y, z);
        mat.scale(size);

        float r = selected ? 0f : 1f;
        float g = selected ? 1f : 0f;
        float b = 0f;

        VertexConsumer buffer = gui.bufferSource().getBuffer(RenderType.gui());

        drawFace(buffer, mat, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, r, g, b); // Front
        drawFace(buffer, mat, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, r, g, b); // Back
        drawFace(buffer, mat, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, r, g, b); // Left
        drawFace(buffer, mat, 1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, r, g, b); // Right
        drawFace(buffer, mat, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, r, g, b); // Top
        drawFace(buffer, mat, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, r, g, b); // Bottom
    }

    private boolean isSelected(int i) {
        return selectedCorner == i;
    }

    private float[] getAxisQuad(float x1, float y1, float x2, float y2, float thickness) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len == 0) return null;

        float px = -dy / len;
        float py = dx / len;

        float halfWidth = thickness / 2.0f;
        float ox = px * halfWidth;
        float oy = py * halfWidth;

        return new float[]{
                x1 + ox, y1 + oy, // A
                x1 - ox, y1 - oy, // B
                x2 - ox, y2 - oy, // C
                x2 + ox, y2 + oy  // D
        };
    }

    private boolean isMouseInsideAxis(double mouseX, double mouseY, float x1, float y1, float x2, float y2, float thickness) {
        float[] quad = getAxisQuad(x1, y1, x2, y2, thickness);
        if (quad == null) return false;

        float ax = quad[0], ay = quad[1];
        float bx = quad[2], by = quad[3];
        float cx = quad[4], cy = quad[5];
        float dx_ = quad[6], dy_ = quad[7];

        return pointInTriangle(mouseX, mouseY, ax, ay, bx, by, cx, cy) ||
                pointInTriangle(mouseX, mouseY, ax, ay, cx, cy, dx_, dy_);
    }

    private boolean pointInTriangle(double px, double py,
                                    float ax, float ay,
                                    float bx, float by,
                                    float cx, float cy) {
        float v0x = cx - ax;
        float v0y = cy - ay;
        float v1x = bx - ax;
        float v1y = by - ay;
        float v2x = (float) px - ax;
        float v2y = (float) py - ay;

        float dot00 = v0x * v0x + v0y * v0y;
        float dot01 = v0x * v1x + v0y * v1y;
        float dot02 = v0x * v2x + v0y * v2y;
        float dot11 = v1x * v1x + v1y * v1y;
        float dot12 = v1x * v2x + v1y * v2y;

        float denom = dot00 * dot11 - dot01 * dot01;
        if (denom == 0) return false;

        float invDenom = 1f / denom;
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    private void drawBorder(GuiGraphics gui, int x, int y, int w, int h, int color) {
        gui.fill(x, y, x + w, y + 1, color);
        gui.fill(x, y + h - 1, x + w, y + h, color);
        gui.fill(x, y, x + 1, y + h, color);
        gui.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void syncCornerToTextFields() {
        if (selectedCorner < 0) return;
        Vector3f pos = menu.getCorners()[selectedCorner];
        xInput.setValue(String.format("%.3f", pos.x));
        yInput.setValue(String.format("%.3f", pos.y));
        zInput.setValue(String.format("%.3f", pos.z));
    }

    private void updateCornerFromText() {
        if (!xInput.isFocused() && !yInput.isFocused() && !zInput.isFocused()) return;
        try {
            float x = Mth.clamp(Float.parseFloat(xInput.getValue()), 0f, 1f);
            float y = Mth.clamp(Float.parseFloat(yInput.getValue()), 0f, 1f);
            float z = Mth.clamp(Float.parseFloat(zInput.getValue()), 0f, 1f);
            menu.getBlockEntity().setCorner(selectedCorner, new Vector3f(x, y, z));
        } catch (NumberFormatException ignored) {
        }
    }

    private boolean isHovering(Slot slot, double d, double e) {
        return this.isHovering(slot.x, slot.y, 16, 16, d, e);
    }

    protected boolean isHovering(int i, int j, int k, int l, double d, double e) {
        int m = this.leftPos;
        int n = this.topPos;
        d -= m;
        e -= n;
        return d >= i - 1 && d < i + k + 1 && e >= j - 1 && e < j + l + 1;
    }
}