package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.common.block.RepositoryBlock;
import com.hollingsworth.arsnouveau.common.block.tile.RepositoryCatalogTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayDeque;
import java.util.Set;

public final class RepositoryNetwork {
    private RepositoryNetwork() {
    }

    public static void refresh(ServerLevel level, BlockPos start, Set<BlockPos> visited) {
        var pending = new ArrayDeque<BlockPos>();
        pending.add(start);
        visited.add(start);
        while (!pending.isEmpty()) {
            BlockPos pos = pending.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.add(neighbor) || !level.isLoaded(neighbor)) {
                    continue;
                }
                if (level.getBlockState(neighbor).getBlock() instanceof RepositoryBlock) {
                    pending.addLast(neighbor);
                } else if (level.getBlockEntity(neighbor) instanceof RepositoryCatalogTile catalog) {
                    catalog.invalidateNetwork();
                }
            }
        }
    }
}
