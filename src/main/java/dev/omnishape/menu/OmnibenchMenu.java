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

    public OmnibenchMenu(int syncId, Inventory inv, OmnibenchBlockEntity blockEntity) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);

        // Internal container with 1 slot
        this.internal = blockEntity.getInventory();

        init(inv);
    }



    public OmnibenchMenu(int syncId, Inventory inv) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);

        // Internal container with 1 slot
        this.internal = new SimpleContainer(1);

        init(inv);
    }

    private void init(Inventory inv) {
        this.addSlot(new Slot(internal, 0, 233, 219)); // Slot index 0, GUI position (50, 50)

        // Position slots relative to desired bottom-aligned inventory
        int baseX = 5;
        int baseY = 161;

        // Player inventory (3 rows of 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, baseX + col * 18, baseY + row * 18));
            }
        }

        // Player hotbar (1 row of 9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, baseX + col * 18, baseY + 58));
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
}