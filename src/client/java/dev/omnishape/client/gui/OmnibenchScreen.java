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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
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
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

    // 8 editable corners (relative to center)
    private final Vector3f[] corners = new Vector3f[8];
    private final Vector4f[] projectedCorners = new Vector4f[8];

    // Which corner (0–7) is currently selected
    private int selectedCorner = -1;

    // Axis being dragged: 0=X, 1=Y, 2=Z, -1=none
    private int draggingAxis = -1;
    private Matrix4f lastMatrix = null;

    public OmnibenchScreen(OmnibenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    private Vector3f dragStartCorner = null;
    private double dragStartMouseX = 0;
    private double dragStartMouseY = 0;

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

        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(
                    (i & 1) == 0 ? 0 : 1,
                    (i & 2) == 0 ? 0 : 1,
                    (i & 4) == 0 ? 0 : 1
            );
        }
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
            for (int i = 0; i < projectedCorners.length; i++) {
                if (selectedCorner >= 0) {
                    Vector3f base = new Vector3f(corners[selectedCorner]).sub(0.5f, 0.5f, 0.5f);
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
                        dragStartCorner = new Vector3f(corners[selectedCorner]);
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        return true;
                    }
                    if (isMouseInsideAxis(mouseX, mouseY, baseScreen.x(), baseScreen.y(), yScreen.x(), yScreen.y(), 2f)) {
                        draggingAxis = 1;
                        dragStartCorner = new Vector3f(corners[selectedCorner]);
                        dragStartMouseX = mouseX;
                        dragStartMouseY = mouseY;
                        return true;
                    }
                    if (isMouseInsideAxis(mouseX, mouseY, baseScreen.x(), baseScreen.y(), zScreen.x(), zScreen.y(), 2f)) {
                        draggingAxis = 2;
                        dragStartCorner = new Vector3f(corners[selectedCorner]);
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
            Vector3f current = corners[selectedCorner];
            if (draggingAxis == 0)
                current.x = Mth.clamp(current.x + movementAmount, 0f, 1f);
            else if (draggingAxis == 1)
                current.y = Mth.clamp(current.y + movementAmount, 0f, 1f);
            else
                current.z = Mth.clamp(current.z + movementAmount, 0f, 1f);

            dragStartCorner.set(current);
            dragStartMouseX = mouseX;
            dragStartMouseY = mouseY;

            return true;
        }

        if (dragging) {
            rotY += (float) dx;
            rotX += (float) dy;

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
        if (button == 0) {
            dragging = false;
            draggingAxis = -1;
            dragStartCorner = null;
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
        pose.scale(40f, 40f, 40f); // flip Y for GUI space
        pose.mulPose(Axis.XP.rotationDegrees(-rotX));
        pose.mulPose(Axis.YP.rotationDegrees(rotY));

        gui.flush(); // flush previous buffer before using direct draw

        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.enableDepthTest();

        VertexConsumer buffer = gui.bufferSource().getBuffer(RenderType.solid());
        Matrix4f mat = pose.last().pose();
        lastMatrix = mat;

        float u1 = sprite.getU0();
        float u2 = sprite.getU1();
        float u3 = sprite.getV0();
        float u4 = sprite.getV1();

        // These are the indices for the corners in the `corners[]` array
        int[][] faces = {
                {0, 1, 3, 2}, //BACK LEFT
                {6, 7, 5, 4}, //FRONT RIGHT
                {4, 5, 1, 0}, //TOP
                {2, 3, 7, 6}, //BOTTOM
                {0, 2, 6, 4}, //FRONT LEFT
                {5, 7, 3, 1} //FRONT RIGHT
        };

        for (int[] face : faces) {
            Vector3f v1 = new Vector3f(corners[face[0]]);
            Vector3f v2 = new Vector3f(corners[face[1]]);
            Vector3f v3 = new Vector3f(corners[face[2]]);
            Vector3f v4 = new Vector3f(corners[face[3]]);

            drawFace(buffer, mat,
                    v1.x, v1.y, v1.z,
                    v2.x, v2.y, v2.z,
                    v3.x, v3.y, v3.z,
                    v4.x, v4.y, v4.z,
                    u1, u3, u2, u4); // Or use color if you prefer
        }

        for (int i = 0; i < corners.length; i++) {
            Vector3f corner = corners[i];
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

        Vector3f baseCorner = corners[selectedCorner];
        Vector3f base = new Vector3f(baseCorner).sub(0.5f, 0.5f, 0.5f);

        float arrowLength = 0.3f;

        Vector4f baseScreen = new Vector4f(base, 1.0f).mul(mat);
        Vector4f xScreen = new Vector4f(new Vector3f(base).add(arrowLength, 0, 0), 1.0f).mul(mat);
        Vector4f yScreen = new Vector4f(new Vector3f(base).add(0, -arrowLength, 0), 1.0f).mul(mat);
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
        float b = selected ? 0f : 0f;

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
}