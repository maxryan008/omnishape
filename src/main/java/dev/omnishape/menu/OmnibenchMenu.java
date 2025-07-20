package dev.omnishape.menu;

import com.mojang.math.Vector3f;
import dev.omnishape.block.entity.OmnibenchBlockEntity;
import dev.omnishape.registry.OmnishapeBlocks;
import dev.omnishape.registry.OmnishapeMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class OmnibenchMenu extends AbstractContainerMenu {

    public static final int REF_SLOT = 0;
    public static final int NEW_SLOT = 1;
    public static final int CAMO_SLOT = 2;
    public static final int OUTPUT_SLOT = 3;
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
        this.addSlot(new Slot(internal, NEW_SLOT, 233, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isFrameBlock(itemStack);
            }
        });
        this.addSlot(new Slot(internal, CAMO_SLOT, 255, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isRenderableBlock(itemStack);
            }
        });
        this.addSlot(new Slot(internal, REF_SLOT, 211, 197) {
            @Override
            public boolean mayPlace(ItemStack itemStack) {
                return isFrameBlock(itemStack) && itemStack.getCount() == 1;
            }

            @Override
            public int getMaxStackSize() {
                return 1;
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

                // Write NBT data to output item
                CompoundTag tag = itemStack.getOrCreateTag();

                // Write camo block state
                BlockState camoState = Block.byItem(camo.getItem()).defaultBlockState();
                tag.put("CamoState", NbtUtils.writeBlockState(camoState));

                // Write corners if applicable
                if (menuBlockEntity != null) {
                    Vector3f[] original = menuBlockEntity.getCorners();
                    ListTag cornerList = new ListTag();
                    for (Vector3f v : original) {
                        CompoundTag vecTag = new CompoundTag();
                        vecTag.putFloat("x", v.x());
                        vecTag.putFloat("y", v.y());
                        vecTag.putFloat("z", v.z());
                        cornerList.add(vecTag);
                    }
                    tag.put("CornersState", cornerList);
                }

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

        ItemStack output = new ItemStack(newFrame.getItem(), count);
        CompoundTag tag = output.getOrCreateTag();

        // Write camo block state
        if (!camo.isEmpty()) {
            BlockState camoState = Block.byItem(camo.getItem()).defaultBlockState();
            tag.put("CamoState", NbtUtils.writeBlockState(camoState));
        }

        // Write corners
        if (menuBlockEntity != null) {
            Vector3f[] original = menuBlockEntity.getCorners();
            ListTag cornerList = new ListTag();
            for (Vector3f v : original) {
                CompoundTag vecTag = new CompoundTag();
                vecTag.putFloat("x", v.x());
                vecTag.putFloat("y", v.y());
                vecTag.putFloat("z", v.z());
                cornerList.add(vecTag);
            }
            tag.put("CornersState", cornerList);
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

            if (index < 27) {
                if (!this.moveItemStackTo(stack, 27, 36, false)) return ItemStack.EMPTY;
            } else if (!this.moveItemStackTo(stack, 0, 27, false)) {
                return ItemStack.EMPTY;
            }

            if (index >= 27) { // from player inventory
                if (isFrameBlock(stack)) {
                    if (!this.moveItemStackTo(stack, NEW_SLOT, CAMO_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isRenderableBlock(stack)) {
                    if (!this.moveItemStackTo(stack, CAMO_SLOT, CAMO_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
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