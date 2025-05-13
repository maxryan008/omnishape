package dev.omnishape.menu;

import dev.omnishape.registry.OmnishapeMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class OmnibenchMenu extends AbstractContainerMenu {

    public OmnibenchMenu(int syncId, Inventory inv) {
        super(OmnishapeMenus.OMNIBENCH_MENU, syncId);

        // Position slots relative to desired bottom-aligned inventory
        // Adjust vertical base Y to place it lower on the screen
        int baseX = 0;
        int baseY = 140; // Increased Y from default 84 to push inventory lower

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