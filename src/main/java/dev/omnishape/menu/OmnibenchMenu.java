package dev.omnishape.menu;

import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.registry.OmnishapeMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OmnibenchMenu extends AbstractContainerMenu {

    private final Container internal;
    private final OmnibenchBlockEntity menuBlockEntity;

    public static final int REF_SLOT = 0;
    public static final int NEW_SLOT = 1;
    public static final int CAMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;

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

    private void init(Inventory inv) {
        this.addSlot(new Slot(internal, NEW_SLOT, 233, 197));
        this.addSlot(new Slot(internal, CAMO_SLOT, 255, 197));
        this.addSlot(new Slot(internal, REF_SLOT, 211, 197));
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
                // Subtract ingredients
                internal.setItem(NEW_SLOT, decrement(internal.getItem(NEW_SLOT), itemStack.getCount()));
                internal.setItem(CAMO_SLOT, decrement(internal.getItem(CAMO_SLOT), itemStack.getCount()));
                // Recalculate output
                updateOutputSlot();
                super.onTake(player, itemStack);
            }
        });

        //Player inventory
        int baseX = 5;
        int baseY = 161;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, baseX + col * 18, baseY + row * 18));
            }
        }
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
        ItemStack ref = internal.getItem(REF_SLOT);

        if (newFrame.isEmpty() || camo.isEmpty()) {
            suppressedSetItem(OUTPUT_SLOT, ItemStack.EMPTY);
            return;
        }

        int count = Math.min(newFrame.getCount(), camo.getCount());

        // Placeholder item until FrameBlock exists
        ItemStack output = new ItemStack(newFrame.getItem(), count);
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

            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
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
}