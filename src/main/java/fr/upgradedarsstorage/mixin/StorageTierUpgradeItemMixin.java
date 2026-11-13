package fr.upgradedarsstorage.mixin;

import fr.upgradedarsstorage.RepositoryTierUpgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.p3pp3rf1y.sophisticatedstorage.item.StorageTierUpgradeItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StorageTierUpgradeItem.class)
abstract class StorageTierUpgradeItemMixin {
    @Shadow
    public abstract StorageTierUpgradeItem.TierUpgrade getTier();

    @Inject(method = "tryUpgradeStorage(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;",
            at = @At("HEAD"), cancellable = true)
    private void upgradedArsStorage$upgradeRepository(ItemStack stack, Level level, BlockPos pos, BlockState state,
                                                       Player player, CallbackInfoReturnable<InteractionResult> callback) {
        if (level instanceof ServerLevel server && player != null) {
            InteractionResult result = RepositoryTierUpgrades.tryUpgrade(getTier(), stack, server, pos, state, player);
            if (result != InteractionResult.PASS) {
                callback.setReturnValue(result);
            }
        }
    }
}
