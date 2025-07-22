package dev.omnishape.menu;

import com.mojang.datafixers.util.Pair;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeComponents;
import dev.omnishape.registry.OmnishapeMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static dev.omnishape.Omnishape.id;

public class OmnibenchMenu extends AbstractContainerMenu {

    public static final int REF_SLOT = 0;
    public static final int NEW_SLOT = 1;
    public static final int CAMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
    public static final int PLAYER_INV_SLOT_START = 4;
    public static final int PLAYER_INV_SLOT_END = 40;
    public static final int HOTBAR_SLOT_START = 31;
    public static final int HOTBAR_SLOT_END = 40;
    private static final Pair<ResourceLocation, ResourceLocation> REF_ICON = Pair.of(InventoryMenu.BLOCK_ATLAS, id("slot/reference"));
    private static final Pair<ResourceLocation, ResourceLocation> NEW_ICON = Pair.of(InventoryMenu.BLOCK_ATLAS, id("slot/frame"));
    private static final Pair<ResourceLocation, ResourceLocation> CAMO_ICON = Pair.of(InventoryMenu.BLOCK_ATLAS, id("slot/camoflauge"));
    private final Container internal;
    private final OmnibenchBlockEntity menuBlockEntity;

    public OmnibenchMenu(int syncId, Inventory inv, OmnibenchBlockEntity blockEntity) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);
        this.internal = blockEntity.getInventory();
        this.menuBlockEntity = blockEntity;
        init(inv);
    }

    public OmnibenchMenu(int syncId, Inventory inv) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);
        this.menuBlockEntity = null;
        this.internal = new SimpleContainer(4);
        init(inv);
    }

    private static Vector3f[] defaultCube() {
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f((i & 1), (i >> 1 & 1), (i >> 2 & 1));
        }
        return corners;
    }

    private void init(Inventory inv) {
        this.addSlot(new Slot(internal, REF_SLOT, 211, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isFrameBlock(itemStack);
            }

            @Nullable
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return REF_ICON;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        this.addSlot(new Slot(internal, NEW_SLOT, 233, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isFrameBlock(itemStack);
            }

            @Nullable
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return NEW_ICON;
            }
        });

        this.addSlot(new Slot(internal, CAMO_SLOT, 255, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isRenderableBlock(itemStack);
            }

            @Nullable
            @Override
            public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return CAMO_ICON;
            }
        });

        this.addSlot(new Slot(internal, OUTPUT_SLOT, 233, 219) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !getItem().isEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                // Get ingredients
                ItemStack camo = internal.getItem(CAMO_SLOT);

                // Subtract ingredients
                internal.setItem(NEW_SLOT, decrement(internal.getItem(NEW_SLOT), itemStack.getCount()));
                internal.setItem(CAMO_SLOT, decrement(internal.getItem(CAMO_SLOT), itemStack.getCount()));

                BlockState camoState = Block.byItem(camo.getItem()).defaultBlockState();
                itemStack.set(OmnishapeComponents.CAMO_STATE, camoState);

                if (menuBlockEntity != null) {
                    Vector3f[] original = menuBlockEntity.getCorners();
                    List<Vector3f> cornerList = new ArrayList<>();
                    for (Vector3f v : original) {
                        cornerList.add(new Vector3f(v)); // copy
                    }
                    itemStack.set(OmnishapeComponents.CORNERS_STATE, cornerList);
                }

                super.onTake(player, itemStack);
            }
        });

        // Player inventory
        int baseX = 5;
        int baseY = 161;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, baseX + col * 18, baseY + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, baseX + col * 18, baseY + 58));
        }
    }

    private ItemStack decrement(ItemStack stack, int amount) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        copy.shrink(amount);
        return copy.getCount() > 0 ? copy : ItemStack.EMPTY;
    }

    public void updateOutputSlot() {
        ItemStack newFrame = internal.getItem(NEW_SLOT);
        ItemStack camo = internal.getItem(CAMO_SLOT);

        if (newFrame.isEmpty() && camo.isEmpty()) {
            suppressedSetItem(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }

        int count;
        if (camo.isEmpty()) {
            count = newFrame.getCount();
        } else {
            count = Math.min(newFrame.getCount(), camo.getCount());
        }

        ItemStack output = new ItemStack(newFrame.getItem(), 1);
        BlockState camoState = Block.byItem(camo.getItem()).defaultBlockState();
        output.set(OmnishapeComponents.CAMO_STATE, camoState);

        if (menuBlockEntity != null) {
            Vector3f[] original = menuBlockEntity.getCorners();
            List<Vector3f> cornerList = new ArrayList<>();
            for (Vector3f v : original) {
                cornerList.add(new Vector3f(v)); // copy
            }
            output.set(OmnishapeComponents.CORNERS_STATE, cornerList);
        }

        suppressedSetItem(OUTPUT_SLOT, output);
    }

    private void suppressedSetItem(int slot, ItemStack itemStack) {
        if (menuBlockEntity instanceof OmnibenchBlockEntity be) {
            be.setSuppressUpdates(true);
        }

        internal.setItem(slot, itemStack);

        if (menuBlockEntity instanceof OmnibenchBlockEntity be) {
            be.setSuppressUpdates(false);
        }
    }

    public ItemStack getItem(int i) {
        return internal.getItem(i);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();

            if (index <= OUTPUT_SLOT) {
                if (!(this.moveItemStackTo(stack, HOTBAR_SLOT_START, HOTBAR_SLOT_END, false)
                        || this.moveItemStackTo(stack, PLAYER_INV_SLOT_START, PLAYER_INV_SLOT_END, false))) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= PLAYER_INV_SLOT_START && index < HOTBAR_SLOT_END) { // from player inventory
                if (isFrameBlock(stack)) {
                    if (!this.moveItemStackTo(stack, REF_SLOT, NEW_SLOT + 1, true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isRenderableBlock(stack)) {
                    if (!this.moveItemStackTo(stack, CAMO_SLOT, CAMO_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            slot.onTake(player, stack);
        }

        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (internal instanceof OmnibenchBlockEntity blockEntity) {
            blockEntity.clearMenuReference();
        }
    }

    private boolean isFrameBlock(ItemStack stack) {
        return stack.getItem() == OmnishapeBlocks.FRAME_BLOCK.asItem();
    }

    private boolean isRenderableBlock(ItemStack stack) {
        BlockState state = Block.byItem(stack.getItem()).defaultBlockState();
        return state.isSolidRender(null, BlockPos.ZERO);
    }

    public Vector3f[] getCorners() {
        return menuBlockEntity != null ? menuBlockEntity.getCorners() : defaultCube();
    }

    public OmnibenchBlockEntity getBlockEntity() {
        return this.menuBlockEntity;
    }

    public Container getContainer() {
        return this.internal;
    }
}