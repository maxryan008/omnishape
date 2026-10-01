package dev.omnishape.menu;

import com.mojang.datafixers.util.Pair;
import dev.omnishape.Constant;
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

public class OmnibenchMenu extends AbstractContainerMenu {

    public static final int REF_SLOT = 0;
    public static final int NEW_SLOT = 1;
    public static final int CAMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

    public static final int PLAYER_INV_SLOT_START = 4;
    public static final int PLAYER_INV_SLOT_END = 40;

    public static final int HOTBAR_SLOT_START = 31;
    public static final int HOTBAR_SLOT_END = 40;

    private static final Pair<ResourceLocation, ResourceLocation> REF_ICON =
            Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.id("slot/reference"));

    private static final Pair<ResourceLocation, ResourceLocation> NEW_ICON =
            Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.id("slot/frame"));

    private static final Pair<ResourceLocation, ResourceLocation> CAMO_ICON =
            Pair.of(InventoryMenu.BLOCK_ATLAS, Constant.id("slot/camoflauge"));

    private final Container internal;
    private final OmnibenchBlockEntity menuBlockEntity;

    public OmnibenchMenu(
            int syncId,
            Inventory inv,
            OmnibenchBlockEntity blockEntity
    ) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);

        this.internal = blockEntity.getInventory();
        this.menuBlockEntity = blockEntity;

        init(inv);
    }

    public OmnibenchMenu(
            int syncId,
            Inventory inv
    ) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);

        this.menuBlockEntity = null;
        this.internal = new SimpleContainer(4);

        init(inv);
    }

    private static Vector3f[] defaultCube() {
        Vector3f[] corners = new Vector3f[8];

        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(
                    i & 1,
                    i >> 1 & 1,
                    i >> 2 & 1
            );
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
                return canCraft();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                consumeIngredients();
                super.onTake(player, itemStack);
            }
        });

        // Player inventory
        int baseX = 5;
        int baseY = 161;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(
                        inv,
                        col + row * 9 + 9,
                        baseX + col * 18,
                        baseY + row * 18
                ));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(
                    inv,
                    col,
                    baseX + col * 18,
                    baseY + 58
            ));
        }

        updateOutputSlot();
    }

    private boolean canCraft() {
        ItemStack newFrame = internal.getItem(NEW_SLOT);
        ItemStack camo = internal.getItem(CAMO_SLOT);

        return !newFrame.isEmpty()
                && !camo.isEmpty()
                && isFrameBlock(newFrame)
                && isRenderableBlock(camo);
    }

    private void consumeIngredients() {
        if (!canCraft()) {
            return;
        }

        setSuppressed(true);

        try {
            ItemStack newFrame = internal.getItem(NEW_SLOT);
            ItemStack camo = internal.getItem(CAMO_SLOT);

            newFrame.shrink(1);
            camo.shrink(1);

            if (newFrame.isEmpty()) {
                internal.setItem(NEW_SLOT, ItemStack.EMPTY);
            } else {
                internal.setItem(NEW_SLOT, newFrame);
            }

            if (camo.isEmpty()) {
                internal.setItem(CAMO_SLOT, ItemStack.EMPTY);
            } else {
                internal.setItem(CAMO_SLOT, camo);
            }
        } finally {
            setSuppressed(false);
        }

        updateOutputSlot();
    }

    public void updateOutputSlot() {
        if (!canCraft()) {
            suppressedSetItem(
                    OUTPUT_SLOT,
                    ItemStack.EMPTY
            );

            return;
        }

        ItemStack newFrame = internal.getItem(NEW_SLOT);
        ItemStack camo = internal.getItem(CAMO_SLOT);

        /*
         * The result slot represents exactly one craft.
         *
         * Even if there are 64 frames and 64 camouflage blocks,
         * the slot displays one result just like a vanilla crafting
         * table result slot.
         */
        ItemStack output = new ItemStack(
                newFrame.getItem(),
                1
        );

        BlockState camoState = Block.byItem(
                camo.getItem()
        ).defaultBlockState();

        output.set(
                OmnishapeComponents.CAMO_STATE,
                camoState
        );

        Vector3f[] original = getCorners();

        List<Vector3f> cornerList = new ArrayList<>(
                original.length
        );

        for (Vector3f corner : original) {
            cornerList.add(
                    new Vector3f(corner)
            );
        }

        output.set(
                OmnishapeComponents.CORNERS_STATE,
                cornerList
        );

        suppressedSetItem(
                OUTPUT_SLOT,
                output
        );
    }

    private void suppressedSetItem(
            int slot,
            ItemStack itemStack
    ) {
        setSuppressed(true);

        try {
            internal.setItem(
                    slot,
                    itemStack
            );
        } finally {
            setSuppressed(false);
        }
    }

    private void setSuppressed(boolean suppressed) {
        if (menuBlockEntity != null) {
            menuBlockEntity.setSuppressUpdates(
                    suppressed
            );
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
    public ItemStack quickMoveStack(
            Player player,
            int index
    ) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(index);

        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        /*
         * Output slot.
         *
         * This is deliberately handled separately from every other slot.
         *
         * The output itself always contains exactly one item. Minecraft's
         * quick-move handling can repeatedly call this method while a valid
         * result continues to exist, giving crafting-table-style shift click
         * behaviour.
         */
        if (index == OUTPUT_SLOT) {
            if (!canCraft()) {
                return ItemStack.EMPTY;
            }

            if (!this.moveItemStackTo(
                    stack,
                    PLAYER_INV_SLOT_START,
                    HOTBAR_SLOT_END,
                    true
            )) {
                return ItemStack.EMPTY;
            }

            /*
             * The stack in the result slot may have been reduced to zero by
             * moveItemStackTo(). Do not use that mutated count to determine
             * ingredient consumption.
             *
             * One successful output transfer always equals one craft.
             */
            slot.onTake(
                    player,
                    original
            );

            return original;
        }

        /*
         * Omnibench input slots -> player inventory.
         */
        if (index >= REF_SLOT && index <= CAMO_SLOT) {
            if (!this.moveItemStackTo(
                    stack,
                    PLAYER_INV_SLOT_START,
                    HOTBAR_SLOT_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        /*
         * Player inventory / hotbar -> Omnibench.
         */
        else if (index >= PLAYER_INV_SLOT_START
                && index < HOTBAR_SLOT_END) {

            if (isFrameBlock(stack)) {
                /*
                 * Try the NEW frame slot first.
                 *
                 * Because REF_SLOT is limited to one item and is primarily
                 * used as the shape reference, shift-clicking a stack of
                 * frames should preferentially supply the crafting input.
                 */
                if (!this.moveItemStackTo(
                        stack,
                        NEW_SLOT,
                        NEW_SLOT + 1,
                        false
                )) {
                    /*
                     * If the new-frame slot cannot accept it, allow the
                     * reference slot as a fallback.
                     */
                    if (!this.moveItemStackTo(
                            stack,
                            REF_SLOT,
                            REF_SLOT + 1,
                            false
                    )) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (isRenderableBlock(stack)) {
                if (!this.moveItemStackTo(
                        stack,
                        CAMO_SLOT,
                        CAMO_SLOT + 1,
                        false
                )) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(
                    ItemStack.EMPTY
            );
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(
                player,
                stack
        );

        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (menuBlockEntity != null) {
            menuBlockEntity.clearMenuReference();
        }
    }

    private boolean isFrameBlock(ItemStack stack) {
        return stack.getItem()
                == OmnishapeBlocks.FRAME_BLOCK.asItem();
    }

    private boolean isRenderableBlock(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Block block = Block.byItem(
                stack.getItem()
        );

        BlockState state = block.defaultBlockState();

        return state.isSolidRender(
                null,
                BlockPos.ZERO
        );
    }

    public Vector3f[] getCorners() {
        return menuBlockEntity != null
                ? menuBlockEntity.getCorners()
                : defaultCube();
    }

    public OmnibenchBlockEntity getBlockEntity() {
        return this.menuBlockEntity;
    }

    public Container getContainer() {
        return this.internal;
    }
}