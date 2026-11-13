package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.common.block.RepositoryBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.pickup.PickupUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedstorage.block.IStorageBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;

import java.util.HashSet;

public final class UpgradedRepositoryBlock extends RepositoryBlock implements IStorageBlock {
    static final int BOOK_STAGES = 10;
    static final IntegerProperty BOOKS = IntegerProperty.create("books", 0, BOOK_STAGES);
    private static final double SIDE_CONTACT_MARGIN = 0.001;
    private static final BooleanProperty TICKING = BooleanProperty.create("ticking");
    private final int inventorySlots;
    private final int upgradeSlots;

    public UpgradedRepositoryBlock(int inventorySlots, int upgradeSlots) {
        this.inventorySlots = inventorySlots;
        this.upgradeSlots = upgradeSlots;
        registerDefaultState(stateDefinition.any().setValue(TICKING, false).setValue(BOOKS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TICKING, BOOKS);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public UpgradedRepositoryBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradedRepositoryBlockEntity(pos, state);
    }

    @Override
    public int getNumberOfInventorySlots() {
        return inventorySlots;
    }

    @Override
    public int getNumberOfUpgradeSlots() {
        return upgradeSlots;
    }

    @Override
    public void setTicking(Level level, BlockPos pos, BlockState state, boolean ticking) {
        if (!ticking && level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            ticking = hasPickupUpgrade(repository);
        }
        if (state.getValue(TICKING) != ticking) {
            level.setBlock(pos, state.setValue(TICKING, ticking), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !state.getValue(TICKING) || type != ArsSophisticated.REPOSITORY_ENTITY.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> {
            var repository = (UpgradedRepositoryBlockEntity) entity;
            StorageBlockEntity.serverTick(world, pos, repository);
            if (hasPickupUpgrade(repository)) {
                // A solid face stops an item just outside the block, so entityInside never sees it.
                AABB contact = new AABB(pos).inflate(SIDE_CONTACT_MARGIN, 0, SIDE_CONTACT_MARGIN);
                for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, contact)) {
                    tryPickup(world, pos, item);
                }
            }
        };
    }

    private static boolean hasPickupUpgrade(UpgradedRepositoryBlockEntity repository) {
        return !repository.getStorageWrapper().getUpgradeHandler().getWrappersThatImplement(PickupUpgradeWrapper.class).isEmpty();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository && stack.has(DataComponents.CUSTOM_NAME)) {
            repository.setCustomName(stack.getHoverName());
        }
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player) ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        tryPickup(level, pos, entity);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        tryPickup(level, pos, entity);
    }

    private static void tryPickup(Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()
                && level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            ItemStack remaining = InventoryHelper.runPickupOnPickupResponseUpgrades(
                    level, repository.getStorageWrapper().getUpgradeHandler(), itemEntity.getItem().copy(), false);
            if (remaining.getCount() < itemEntity.getItem().getCount()) {
                itemEntity.setItem(remaining);
            }
        }
    }

    private boolean open(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository)) {
            return false;
        }
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider(
                            (id, inventory, opener) -> new StorageContainerMenu(id, opener, pos),
                            repository.getMenuDisplayName()),
                    buffer -> StorageContainerMenu.writeMenuData(buffer, player, pos));
            player.awardStat(Stats.OPEN_CHEST);
            PiglinAi.angerNearbyPiglins(player, true);
        }
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            repository.finishLoading();
            BlockState updated = state.setValue(BOOKS, repository.getBookStage());
            // Older worlds may already have the pickup upgrade installed with ticking disabled.
            if (!state.getValue(TICKING) && hasPickupUpgrade(repository)) {
                updated = updated.setValue(TICKING, true);
            }
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            }
            repository.recheckOpen();
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            repository.onNeighborChange(neighborPos);
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            repository.refreshFilters();
        }
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository) {
            return repository.getComparatorSignal();
        }
        return 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof UpgradedRepositoryBlockEntity repository
                && !repository.isBeingUpgraded()) {
            repository.closeMenusForThisBlock();
            repository.dropContents();
            RepositoryNetwork.refresh(server, pos, new HashSet<>());
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
