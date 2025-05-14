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

    private final SimpleContainer inventory = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            OmnibenchBlockEntity.this.setChanged();
        }
    };

    public OmnibenchBlockEntity(BlockPos pos, BlockState state) {
        super(OmnishapeBlockEntities.OMNIBENCH, pos, state);
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Omnibench");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, net.minecraft.world.entity.player.Player player) {
        return new OmnibenchMenu(syncId, inv, this);
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