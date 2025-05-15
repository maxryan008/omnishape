package dev.omnishape.block.entity;

import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.registry.OmnishapeBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OmnibenchBlockEntity extends BlockEntity implements MenuProvider {

    private boolean suppressUpdates = false;

    private final SimpleContainer inventory = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!suppressUpdates) {
                OmnibenchBlockEntity.this.setChanged();
                if (currentMenu != null) {
                    currentMenu.updateOutputSlot();
                }
            }
        }
    };

    private OmnibenchMenu currentMenu = null;

    public OmnibenchBlockEntity(BlockPos pos, BlockState state) {
        super(OmnishapeBlockEntities.OMNIBENCH, pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public void setSuppressUpdates(boolean suppress) {
        this.suppressUpdates = suppress;
    }

    public boolean isSuppressingUpdates() {
        return suppressUpdates;
    }

    public void clearMenuReference() {
        this.currentMenu = null;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Omnibench");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, net.minecraft.world.entity.player.Player player) {
        OmnibenchMenu menu = new OmnibenchMenu(syncId, inv, this);
        this.currentMenu = menu;
        return menu;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, inventory.getItems(), provider);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        ContainerHelper.loadAllItems(compoundTag, inventory.getItems(), provider);
    }
}