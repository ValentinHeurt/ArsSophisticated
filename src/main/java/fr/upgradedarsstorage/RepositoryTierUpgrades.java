package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.common.block.tile.RepositoryTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem.TierUpgrade;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

import static fr.upgradedarsstorage.ArsSophisticated.*;
import static java.util.Map.entry;
import static net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem.TierUpgrade.*;

public final class RepositoryTierUpgrades {
    private static final Map<TierUpgrade, Transition> TRANSITIONS = Map.ofEntries(
            entry(BASIC_TO_COPPER, new Transition(BlockRegistry.REPOSITORY, COPPER_REPOSITORY)),
            entry(BASIC_TO_IRON, new Transition(BlockRegistry.REPOSITORY, IRON_REPOSITORY)),
            entry(BASIC_TO_GOLD, new Transition(BlockRegistry.REPOSITORY, GOLD_REPOSITORY)),
            entry(BASIC_TO_DIAMOND, new Transition(BlockRegistry.REPOSITORY, DIAMOND_REPOSITORY)),
            entry(BASIC_TO_NETHERITE, new Transition(BlockRegistry.REPOSITORY, NETHERITE_REPOSITORY)),
            entry(COPPER_TO_IRON, new Transition(COPPER_REPOSITORY, IRON_REPOSITORY)),
            entry(COPPER_TO_GOLD, new Transition(COPPER_REPOSITORY, GOLD_REPOSITORY)),
            entry(COPPER_TO_DIAMOND, new Transition(COPPER_REPOSITORY, DIAMOND_REPOSITORY)),
            entry(COPPER_TO_NETHERITE, new Transition(COPPER_REPOSITORY, NETHERITE_REPOSITORY)),
            entry(IRON_TO_GOLD, new Transition(IRON_REPOSITORY, GOLD_REPOSITORY)),
            entry(IRON_TO_DIAMOND, new Transition(IRON_REPOSITORY, DIAMOND_REPOSITORY)),
            entry(IRON_TO_NETHERITE, new Transition(IRON_REPOSITORY, NETHERITE_REPOSITORY)),
            entry(GOLD_TO_DIAMOND, new Transition(GOLD_REPOSITORY, DIAMOND_REPOSITORY)),
            entry(GOLD_TO_NETHERITE, new Transition(GOLD_REPOSITORY, NETHERITE_REPOSITORY)),
            entry(DIAMOND_TO_NETHERITE, new Transition(DIAMOND_REPOSITORY, NETHERITE_REPOSITORY)));

    private RepositoryTierUpgrades() {
    }

    public static InteractionResult tryUpgrade(TierUpgrade tier, ItemStack stack, ServerLevel level, BlockPos pos,
                                               BlockState state, Player player) {
        Transition transition = TRANSITIONS.get(tier);
        if (transition == null || !state.is(transition.from().get()) || !level.getBlockState(pos).is(state.getBlock())) {
            return InteractionResult.PASS;
        }
        if (stack.isEmpty() || !level.mayInteract(player, pos) || !player.getAbilities().mayBuild) {
            return InteractionResult.FAIL;
        }

        UpgradedRepositoryBlock target = transition.to().get();
        var replacement = target.newBlockEntity(pos, target.withPropertiesOf(state));
        boolean upgraded = switch (level.getBlockEntity(pos)) {
            case RepositoryTile original -> upgradeBasic(level, pos, player, original, replacement);
            case UpgradedRepositoryBlockEntity original -> upgradeStorage(level, pos, original, replacement, target);
            case null, default -> false;
        };
        if (!upgraded) {
            return InteractionResult.FAIL;
        }

        replacement.setChanged();
        RepositoryNetwork.refresh(level, pos, new HashSet<>());
        level.updateNeighbourForOutputSignal(pos, target);
        level.sendBlockUpdated(pos, state, replacement.getBlockState(), Block.UPDATE_CLIENTS);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    private static boolean upgradeBasic(ServerLevel level, BlockPos pos, Player player, RepositoryTile original,
                                        UpgradedRepositoryBlockEntity replacement) {
        if (!original.canOpen(player)) {
            return false;
        }
        original.unpackLootTable(player);
        var contents = NonNullList.withSize(original.getContainerSize(), ItemStack.EMPTY);
        for (int slot = 0; slot < contents.size(); slot++) {
            // Ars Nouveau drops the old inventory during block replacement.
            contents.set(slot, original.removeItemNoUpdate(slot));
        }
        if (!level.setBlockAndUpdate(pos, replacement.getBlockState())) {
            for (int slot = 0; slot < contents.size(); slot++) {
                original.setItem(slot, contents.get(slot));
            }
            return false;
        }

        level.setBlockEntity(replacement);
        replacement.setCustomName(original.getCustomName());
        var inventory = replacement.getStorageWrapper().getInventoryHandler();
        for (int slot = 0; slot < contents.size(); slot++) {
            if (!contents.get(slot).isEmpty()) {
                inventory.setStackInSlot(slot, contents.get(slot));
            }
        }
        return true;
    }

    private static boolean upgradeStorage(ServerLevel level, BlockPos pos, UpgradedRepositoryBlockEntity original,
                                          UpgradedRepositoryBlockEntity replacement, UpgradedRepositoryBlock target) {
        if (original.isOpen() || !original.canUpgradeStorageTier(level)) {
            return false;
        }

        replacement.setBeingUpgraded(true);
        replacement.loadWithComponents(original.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        original.closeMenusForThisBlock();
        original.setBeingUpgraded(true);
        try {
            if (!level.setBlockAndUpdate(pos, replacement.getBlockState())) {
                return false;
            }
            level.setBlockEntity(replacement);
        } finally {
            original.setBeingUpgraded(false);
            replacement.setBeingUpgraded(false);
        }

        int inventorySlots = target.getNumberOfInventorySlots();
        int upgradeSlots = target.getNumberOfUpgradeSlots();
        if (replacement.isLinkedStorage()) {
            if (!replacement.completePrimaryLinkedStorageTierUpgrade(level, inventorySlots, upgradeSlots)) {
                throw new IllegalStateException("Could not resize the primary linked Repository after upgrading");
            }
        } else {
            var wrapper = replacement.getStorageWrapper();
            replacement.changeStorageSize(inventorySlots - wrapper.getInventoryHandler().getSlots(),
                    upgradeSlots - wrapper.getUpgradeHandler().getSlots());
        }
        return true;
    }

    private record Transition(Supplier<? extends Block> from, Supplier<UpgradedRepositoryBlock> to) {
    }
}
