package fr.upgradedarsstorage.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.RepositoryTile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fr.upgradedarsstorage.RepositoryNetwork;
import fr.upgradedarsstorage.UpgradedRepositoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

@Mixin(RepositoryTile.class)
abstract class RepositoryTileMixin {
    @WrapOperation(method = "invalidateNetwork(Ljava/util/Set;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity upgradedArsStorage$refreshAcrossUpgradedRepositories(Level level, BlockPos pos,
                                                                           Operation<BlockEntity> original,
                                                                           @Local(argsOnly = true) Set<BlockPos> visited) {
        BlockEntity neighbor = original.call(level, pos);
        if (neighbor instanceof UpgradedRepositoryBlockEntity && level instanceof ServerLevel server) {
            RepositoryNetwork.refresh(server, pos, visited);
        }
        return neighbor;
    }
}
