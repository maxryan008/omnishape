package dev.omnishape.block.entity;

import dev.omnishape.menu.OmnibenchMenu;
import dev.omnishape.registry.OmnishapeBlockEntities;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class OmnibenchBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {

    private final Vector3f[] corners = new Vector3f[8];
    private boolean suppressUpdates = false;
    private OmnibenchMenu currentMenu = null;
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

    public OmnibenchBlockEntity(BlockPos pos, BlockState state) {
        super(OmnishapeBlockEntities.OMNIBENCH, pos, state);

        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f((i & 1), (i >> 1 & 1), (i >> 2 & 1));
        }
    }

    public SimpleContainer getInventory() {
        return inventory;
    }

    public void setSuppressUpdates(boolean suppress) {
        this.suppressUpdates = suppress;
    }

    public void clearMenuReference() {
        this.currentMenu = null;
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.literal("Omnibench");
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, net.minecraft.world.entity.player.Player player) {
        this.currentMenu = new OmnibenchMenu(syncId, inv, this);
        return this.currentMenu;
    }

    public Vector3f[] getCorners() {
        return corners;
    }

    public boolean hasMenu() {
        return (this.currentMenu != null);
    }

    public OmnibenchMenu getMenu() {
        return this.currentMenu;
    }

    public Vector3f getCorner(int i) {
        return corners[i];
    }

    public void setCorner(int i, Vector3f value) {
        corners[i].set(value);
        setChanged();

        inventory.setChanged();

        if (level != null && level.isClientSide()) {
            sendCornerUpdateToServer(worldPosition, i, value);
        }
    }

    private void sendCornerUpdateToServer(BlockPos pos, int index, Vector3f vec) {
        // This should be overridden client side eventually
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, inventory.getItems(), provider);
        SaveCorners(compoundTag, corners);
    }

    static void SaveCorners(CompoundTag compoundTag, Vector3f[] corners) {
        ListTag cornerList = new ListTag();
        for (Vector3f vec : corners) {
            CompoundTag vecTag = new CompoundTag();
            vecTag.putFloat("x", vec.x);
            vecTag.putFloat("y", vec.y);
            vecTag.putFloat("z", vec.z);
            cornerList.add(vecTag);
        }
        compoundTag.put("Corners", cornerList);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithFullMetadata(provider);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        ContainerHelper.loadAllItems(compoundTag, inventory.getItems(), provider);
        ListTag list = compoundTag.getList("Corners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size() && i < 8; i++) {
            CompoundTag vecTag = list.getCompound(i);
            corners[i].set(vecTag.getFloat("x"), vecTag.getFloat("y"), vecTag.getFloat("z"));
        }
    }

    @Override
    public Object getScreenOpeningData(ServerPlayer serverPlayer) {
        return this.getBlockPos();
    }
}