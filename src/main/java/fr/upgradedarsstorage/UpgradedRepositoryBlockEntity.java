package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.api.item.inv.FilterSet;
import com.hollingsworth.arsnouveau.api.item.inv.IMapInventory;
import com.hollingsworth.arsnouveau.common.items.ItemScroll.SortPref;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.SophisticatedOpenersCounter;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.HashSet;
import java.util.function.Predicate;

public final class UpgradedRepositoryBlockEntity extends StorageBlockEntity implements IMapInventory {
    private FilterSet filters = new FilterSet.ListSet();
    private boolean needsInitialRefresh = true;

    private final SophisticatedOpenersCounter openers = new SophisticatedOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof StorageContainerMenu menu) {
                return menu.getStorageBlockEntity() == UpgradedRepositoryBlockEntity.this;
            }
            return player.containerMenu instanceof StorageSettingsContainerMenu menu && menu.getBlockPosition().equals(worldPosition);
        }
    };

    public UpgradedRepositoryBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state, ArsSophisticated.REPOSITORY_ENTITY.get());
    }

    @Override
    public SophisticatedOpenersCounter getOpenersCounter() {
        return openers;
    }

    @Override
    protected String getStorageType() {
        return ChestBlockEntity.STORAGE_TYPE;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            needsInitialRefresh = true;
            refreshFilters();
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!isRemoved() && level instanceof ServerLevel server && server.getServer().isSameThread()) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    int getBookStage() {
        var inventory = getStorageWrapper().getInventoryHandler();
        int occupiedSlots = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                occupiedSlots++;
            }
        }
        return occupiedSlots == 0 ? 0 : Math.max(1, occupiedSlots * UpgradedRepositoryBlock.BOOK_STAGES / inventory.getSlots());
    }

    void finishLoading() {
        if (needsInitialRefresh && level instanceof ServerLevel server) {
            needsInitialRefresh = false;
            refreshFilters();
            RepositoryNetwork.refresh(server, worldPosition, new HashSet<>());
        }
    }

    void refreshFilters() {
        filters = FilterSet.forPosition(level, worldPosition);
    }

    @Override
    public ItemStack insertStack(ItemStack stack, boolean simulate) {
        if (getInsertionPreference(stack) == SortPref.INVALID) {
            return stack;
        }
        return getStorageWrapper().getInventoryForInputOutput().insertItem(stack, simulate);
    }

    @Override
    public boolean hasExistingSlotsForInsertion(ItemStack stack) {
        var inventory = getStorageWrapper().getInventoryHandler();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).getItem() == stack.getItem()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack extractByItem(Item item, int count, boolean simulate, Predicate<ItemStack> filter) {
        var inventory = getStorageWrapper().getInventoryForInputOutput();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty() && stack.is(item) && filter.test(stack)) {
                ItemStack extracted = inventory.extractItem(slot, Math.min(count, stack.getMaxStackSize()), simulate);
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public SortPref getInsertionPreference(ItemStack stack) {
        SortPref preference = filters.getHighestPreference(stack);
        if (preference == SortPref.LOW && hasExistingSlotsForInsertion(stack)) {
            return SortPref.HIGH;
        }
        return preference;
    }

    int getComparatorSignal() {
        return InventoryHelper.getAnalogOutputSignal(getStorageWrapper().getInventoryForInputOutput());
    }
}
