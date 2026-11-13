package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.client.container.StoredItemStack;
import com.hollingsworth.arsnouveau.common.block.tile.CraftingLecternTile;
import com.hollingsworth.arsnouveau.common.block.tile.RepositoryCatalogTile;
import com.hollingsworth.arsnouveau.common.block.tile.RepositoryTile;
import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModItems;

import java.util.List;

@GameTestHolder(ArsSophisticated.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RepositoryTests {
    private static final String RECIPE_NAMESPACE = "upgraded_ars_storage";
    private static final BlockPos CATALOG = new BlockPos(1, 1, 1);
    private static final BlockPos IRON = CATALOG.east();
    private static final BlockPos REGULAR = IRON.east();
    private static final BlockPos SECOND_IRON = REGULAR.east();
    private static final BlockPos LECTERN = CATALOG.south();

    @GameTest(template = "repository_test")
    public static void pickupUpgradeCollectsDroppedItemsLocally(GameTestHelper helper) {
        var block = ArsSophisticated.IRON_REPOSITORY.get();
        helper.setBlock(IRON, block);
        helper.setBlock(SECOND_IRON, block);
        var level = helper.getLevel();
        var upgradedPos = helper.absolutePos(IRON);
        var neighborPos = helper.absolutePos(SECOND_IRON);
        var upgraded = (UpgradedRepositoryBlockEntity) level.getBlockEntity(upgradedPos);
        var neighbor = (UpgradedRepositoryBlockEntity) level.getBlockEntity(neighborPos);
        var storage = upgraded.getStorageWrapper();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PICKUP_UPGRADE.get()));

        var drop = new ItemEntity(level, upgradedPos.getX() + 0.5, upgradedPos.getY() + 1.5,
                upgradedPos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 3));
        var neighborDrop = new ItemEntity(level, neighborPos.getX() + 0.5, neighborPos.getY() + 1.5,
                neighborPos.getZ() + 0.5, new ItemStack(Items.DIRT));
        drop.setDeltaMovement(Vec3.ZERO);
        neighborDrop.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(drop);
        level.addFreshEntity(neighborDrop);

        helper.runAfterDelay(20, () -> {
            ItemStack stored = storage.getInventoryHandler().getStackInSlot(0);
            helper.assertTrue(stored.is(Items.COBBLESTONE) && stored.getCount() == 3,
                    "the pickup upgrade collects a stack dropped onto the repository");
            helper.assertTrue(drop.isRemoved(), "collected items disappear from the world");
            helper.assertTrue(!neighborDrop.isRemoved()
                            && neighbor.getStorageWrapper().getInventoryHandler().getStackInSlot(0).isEmpty(),
                    "the repository without a pickup upgrade leaves its item alone");
            helper.succeed();
        });
    }

    @GameTest(template = "repository_test")
    public static void pickupUpgradeCollectsItemsThatHitTheSide(GameTestHelper helper) {
        helper.setBlock(IRON, ArsSophisticated.IRON_REPOSITORY.get());
        var level = helper.getLevel();
        var pos = helper.absolutePos(IRON);
        var repository = (UpgradedRepositoryBlockEntity) level.getBlockEntity(pos);
        var storage = repository.getStorageWrapper();
        storage.getUpgradeHandler().setStackInSlot(0, new ItemStack(ModItems.PICKUP_UPGRADE.get()));

        var thrownItem = new ItemEntity(level, pos.getX() - 0.55, pos.getY() + 0.4,
                pos.getZ() + 0.5, new ItemStack(Items.COBBLESTONE, 3));
        thrownItem.setNoGravity(true);
        thrownItem.setDeltaMovement(0.3, 0, 0);
        level.addFreshEntity(thrownItem);

        var nearbyItem = new ItemEntity(level, pos.getX() + 1.35, pos.getY() + 0.4,
                pos.getZ() + 0.5, new ItemStack(Items.DIRT));
        nearbyItem.setNoGravity(true);
        nearbyItem.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(nearbyItem);

        helper.runAfterDelay(20, () -> {
            ItemStack stored = storage.getInventoryHandler().getStackInSlot(0);
            helper.assertTrue(thrownItem.isRemoved() && stored.is(Items.COBBLESTONE) && stored.getCount() == 3,
                    "the repository collects an item thrown into its side");
            helper.assertTrue(!nearbyItem.isRemoved(), "items with a gap to the repository stay outside");
            helper.succeed();
        });
    }

    @GameTest(template = "repository_test")
    public static void stackUpgradeIsLocal(GameTestHelper helper) {
        var block = ArsSophisticated.IRON_REPOSITORY.get();
        helper.setBlock(CATALOG, BlockRegistry.REPOSITORY_CONTROLLER.get());
        helper.setBlock(IRON, block);
        helper.setBlock(REGULAR, BlockRegistry.REPOSITORY.get());
        helper.setBlock(SECOND_IRON, block);
        helper.setBlock(LECTERN, BlockRegistry.CRAFTING_LECTERN.get());
        helper.runAfterDelay(3, () -> {
            UpgradedRepositoryBlockEntity iron = helper.getBlockEntity(IRON);
            var wrapper = iron.getStorageWrapper();
            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var menu = new StorageContainerMenu(1, player, helper.absolutePos(IRON));
            helper.assertValueEqual(menu.getNumberOfStorageInventorySlots(), 81, "inventory slots");
            helper.assertValueEqual(menu.getNumberOfUpgradeSlots(), 1, "upgrade slots");

            var level = helper.getLevel();
            var catalog = level.getCapability(CapabilityRegistry.MAP_INV_CAP, helper.absolutePos(CATALOG), null);
            var ironItems = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(IRON), null);
            var regularItems = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(REGULAR), null);
            var otherIronItems = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(SECOND_IRON), null);
            helper.assertTrue(catalog != null && ironItems != null && regularItems != null && otherIronItems != null,
                    "the catalog and all three inventories must expose their capabilities");
            int regularLimit = regularItems.getSlotLimit(0);
            int ironLimit = otherIronItems.getSlotLimit(0);

            var upgrade = ModItems.STACK_UPGRADE_TIER_1.get();
            menu.setCarried(new ItemStack(upgrade));
            menu.clicked(menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP, player);
            helper.assertTrue(menu.getCarried().isEmpty() && wrapper.getUpgradeHandler().getStackInSlot(0).is(upgrade),
                    "the Sophisticated Storage menu must accept the stack upgrade");
            ironItems = level.getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(IRON), null);

            ItemStack cobblestone = new ItemStack(Items.COBBLESTONE, 64);
            helper.assertTrue(ironItems.insertItem(0, cobblestone.copy(), false).isEmpty(), "seed the upgraded repository");
            helper.assertTrue(regularItems.insertItem(0, cobblestone.copy(), false).isEmpty(), "seed the regular repository");
            helper.assertTrue(otherIronItems.insertItem(0, cobblestone.copy(), false).isEmpty(), "seed the second Iron Repository");
            helper.assertTrue(catalog.insertStack(cobblestone, true).isEmpty(), "simulated catalog deposit must fit");
            helper.assertValueEqual(ironItems.getStackInSlot(0).getCount(), 64, "simulated deposit leaves contents intact");
            CraftingLecternTile lectern = helper.getBlockEntity(LECTERN);
            lectern.addHandlerPos(helper.absolutePos(CATALOG));
            lectern.updateItems();
            helper.assertTrue(lectern.pushStack(cobblestone.copy(), "").isEmpty(),
                    "a deposit from the linked lectern must fit");
            lectern.updateItems();
            long listedItems = lectern.getStacks("").values().stream().mapToLong(Long::longValue).sum();
            helper.assertValueEqual(listedItems, 256L, "the linked lectern sees the full count across the catalog");
            helper.assertValueEqual(ironItems.getStackInSlot(0).getCount(), 128, "upgraded stack after catalog deposit");
            helper.assertValueEqual(regularItems.getStackInSlot(0).getCount(), 64, "regular repository stack after catalog deposit");
            helper.assertValueEqual(otherIronItems.getStackInSlot(0).getCount(), 64, "second Iron Repository stack after catalog deposit");
            helper.assertValueEqual(regularItems.getSlotLimit(0), regularLimit, "regular repository slot limit");
            helper.assertValueEqual(otherIronItems.getSlotLimit(0), ironLimit, "second Iron Repository slot limit");
            helper.assertValueEqual(ironItems.getSlotLimit(0), 128, "upgraded repository slot limit");

            menu.clicked(menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP, player);
            helper.assertFalse(wrapper.getUpgradeHandler().getStackInSlot(0).isEmpty(),
                    "removing a stack upgrade while a stack exceeds its normal limit must be blocked");

            var registries = level.registryAccess();
            var saved = iron.saveWithFullMetadata(registries);
            helper.assertValueEqual(saved.getString("id"), "ars_sophisticated:iron_repository", "block entity ID");
            var loaded = (UpgradedRepositoryBlockEntity) BlockEntity.loadStatic(
                    iron.getBlockPos(), iron.getBlockState(), saved, registries);
            helper.assertTrue(loaded != null, "saved repository must load");
            helper.assertValueEqual(loaded.getStorageWrapper().getInventoryHandler().getStackInSlot(0).getCount(), 128,
                    "saved count larger than one vanilla stack");
            helper.assertTrue(loaded.getStorageWrapper().getUpgradeHandler().getStackInSlot(0).is(upgrade),
                    "saved upgrade");

            helper.assertValueEqual(catalog.extractByItem(Items.COBBLESTONE, 64, true).getCount(), 64, "simulated withdrawal");
            var withdrawn = lectern.pullStack(new StoredItemStack(new ItemStack(Items.COBBLESTONE)), 64, "");
            helper.assertTrue(withdrawn != null && withdrawn.getQuantity() == 64, "withdrawal from the linked lectern");
            helper.assertValueEqual(ironItems.getStackInSlot(0).getCount(), 64, "upgraded stack after withdrawal");
            menu.clicked(menu.getFirstUpgradeSlot(), 0, ClickType.PICKUP, player);
            helper.assertTrue(wrapper.getUpgradeHandler().getStackInSlot(0).isEmpty(), "the upgrade can now be removed");
            helper.succeed();
        });
    }

    @GameTest(template = "repository_test")
    public static void mixedCatalogTracksRepositoryChanges(GameTestHelper helper) {
        helper.setBlock(CATALOG, BlockRegistry.REPOSITORY_CONTROLLER.get());
        helper.setBlock(IRON, ArsSophisticated.IRON_REPOSITORY.get());
        helper.setBlock(REGULAR, BlockRegistry.REPOSITORY.get());
        helper.setBlock(SECOND_IRON, BlockRegistry.REPOSITORY.get());
        helper.runAfterDelay(3, () -> {
            RepositoryCatalogTile catalog = helper.getBlockEntity(CATALOG);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 81 + 54 + 54, "initial mixed catalog size");
            helper.setBlock(SECOND_IRON, Blocks.AIR);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 81 + 54, "catalog size after removing the far repository");
            var regular = BlockRegistry.REPOSITORY.get();
            helper.setBlock(SECOND_IRON, regular);
            regular.setPlacedBy(helper.getLevel(), helper.absolutePos(SECOND_IRON), helper.getBlockState(SECOND_IRON),
                    null, new ItemStack(regular));
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 81 + 54 + 54, "catalog size after placing the far repository");
            helper.setBlock(IRON, Blocks.AIR);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 0, "catalog size after removing the bridge");
            helper.setBlock(IRON, ArsSophisticated.IRON_REPOSITORY.get());
            helper.runAfterDelay(3, () -> {
                helper.assertValueEqual(catalog.getControllerInv().getSlots(), 81 + 54 + 54, "catalog size after replacing the bridge");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "repository_test")
    public static void recipeAndBlockDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipeId = ResourceLocation.fromNamespaceAndPath(RECIPE_NAMESPACE, "iron_repository");
        helper.assertTrue(level.getRecipeManager().byKey(recipeId).isPresent(), "Iron Repository crafting recipe must load");
        helper.setBlock(IRON, ArsSophisticated.IRON_REPOSITORY.get());
        UpgradedRepositoryBlockEntity iron = helper.getBlockEntity(IRON);
        var wrapper = iron.getStorageWrapper();
        var upgrade = ModItems.STACK_UPGRADE_TIER_1.get();
        wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(upgrade));
        wrapper.getInventoryHandler().insertItem(0, new ItemStack(Items.COBBLESTONE, 128), false);
        level.destroyBlock(helper.absolutePos(IRON), true);
        helper.assertItemEntityCountIs(ArsSophisticated.IRON_REPOSITORY_ITEM.get(), IRON, 2, 1);
        helper.assertItemEntityCountIs(upgrade, IRON, 2, 1);
        helper.assertItemEntityCountIs(Items.COBBLESTONE, IRON, 2, 128);
        helper.succeed();
    }

    @GameTest(template = "repository_test")
    public static void allTiersJoinTheSameCatalog(GameTestHelper helper) {
        var tiers = List.of(ArsSophisticated.COPPER_REPOSITORY, ArsSophisticated.IRON_REPOSITORY,
                ArsSophisticated.GOLD_REPOSITORY, ArsSophisticated.DIAMOND_REPOSITORY,
                ArsSophisticated.NETHERITE_REPOSITORY);
        var positions = List.of(IRON, REGULAR, SECOND_IRON, SECOND_IRON.east(), SECOND_IRON.east().south());
        var level = helper.getLevel();
        helper.setBlock(CATALOG, BlockRegistry.REPOSITORY_CONTROLLER.get());
        for (int index = 0; index < tiers.size(); index++) {
            var tier = tiers.get(index);
            var recipeId = ResourceLocation.fromNamespaceAndPath(RECIPE_NAMESPACE, tier.getId().getPath());
            helper.assertTrue(level.getRecipeManager().byKey(recipeId).isPresent(), "missing recipe for " + tier.getId());
            helper.setBlock(positions.get(index), tier.get());
        }

        helper.runAfterDelay(3, () -> {
            RepositoryCatalogTile catalog = helper.getBlockEntity(CATALOG);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 72 + 81 + 108 + 144 + 168, "total slots across all tiers");

            var player = helper.makeMockPlayer(GameType.SURVIVAL);
            var upgrade = ModItems.STACK_UPGRADE_TIER_1.get();
            int[] slotsByTier = {72, 81, 108, 144, 168};
            for (int index = 0; index < tiers.size(); index++) {
                BlockPos pos = helper.absolutePos(positions.get(index));
                int expectedSlots = slotsByTier[index];
                int expectedUpgrades = Math.max(1, index);
                var menu = new StorageContainerMenu(index + 1, player, pos);
                helper.assertValueEqual(menu.getNumberOfStorageInventorySlots(), expectedSlots, "storage slots for " + tiers.get(index).getId());
                helper.assertValueEqual(menu.getNumberOfUpgradeSlots(), expectedUpgrades, "upgrade slots for " + tiers.get(index).getId());
                int columns = index < 2 ? 9 : 12;
                helper.assertValueEqual(menu.getNumberOfRows() * columns, expectedSlots, "complete rows for " + tiers.get(index).getId());

                if (index == tiers.size() - 1) {
                    menu.setCarried(new ItemStack(upgrade));
                    menu.clicked(menu.getFirstUpgradeSlot() + expectedUpgrades - 1, 0, ClickType.PICKUP, player);
                    var upgrades = menu.getStorageBlockEntity().getStorageWrapper().getUpgradeHandler();
                    helper.assertTrue(upgrades.getStackInSlot(expectedUpgrades - 1).is(upgrade),
                            "the fourth Netherite Repository slot accepts a stack upgrade");
                }
                menu.removed(player);
            }

            var combined = catalog.getControllerInv();
            helper.assertValueEqual(combined.getSlotLimit(0), 64, "Copper Repository limit");
            helper.assertValueEqual(combined.getSlotLimit(72), 64, "Iron Repository limit");
            helper.assertValueEqual(combined.getSlotLimit(72 + 81), 64, "Gold Repository limit");
            helper.assertValueEqual(combined.getSlotLimit(72 + 81 + 108), 64, "Diamond Repository limit");
            helper.assertValueEqual(combined.getSlotLimit(combined.getSlots() - 1), 128, "Netherite Repository limit");
            helper.succeed();
        });
    }

    @GameTest(template = "repository_test")
    public static void repositoriesReportTheAddon(GameTestHelper helper) {
        var tiers = List.of(ArsSophisticated.COPPER_REPOSITORY, ArsSophisticated.IRON_REPOSITORY,
                ArsSophisticated.GOLD_REPOSITORY, ArsSophisticated.DIAMOND_REPOSITORY,
                ArsSophisticated.NETHERITE_REPOSITORY);
        for (var tier : tiers) {
            ItemStack stack = tier.get().asItem().getDefaultInstance();
            helper.assertValueEqual(stack.getItem().getCreatorModId(stack), "ars_sophisticated", "JEI item owner");
        }

        var addon = ModList.get().getModContainerById("ars_sophisticated").orElseThrow();
        helper.assertValueEqual(addon.getModInfo().getDisplayName(), "Ars Sophisticated", "addon display name");
        ItemStack original = BlockRegistry.REPOSITORY.get().asItem().getDefaultInstance();
        helper.assertValueEqual(original.getItem().getCreatorModId(original), "ars_nouveau", "original Repository owner");
        helper.succeed();
    }

    @GameTest(template = "repository_test")
    public static void tierRecipesRequireEightMaterials(GameTestHelper helper) {
        var repositories = List.of(BlockRegistry.REPOSITORY.get().asItem(), ArsSophisticated.COPPER_REPOSITORY_ITEM.get(),
                ArsSophisticated.IRON_REPOSITORY_ITEM.get(), ArsSophisticated.GOLD_REPOSITORY_ITEM.get(),
                ArsSophisticated.DIAMOND_REPOSITORY_ITEM.get());
        var materials = List.of(Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND);
        var level = helper.getLevel();
        for (int tier = 0; tier < materials.size(); tier++) {
            Item output = repositories.get(tier + 1);
            var grid = NonNullList.withSize(9, new ItemStack(materials.get(tier)));
            grid.set(4, new ItemStack(repositories.get(tier)));
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level);
            helper.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(level.registryAccess()).is(output),
                    "eight materials around the previous Repository must produce " + output);
            grid.set(0, ItemStack.EMPTY);
            helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level).isEmpty(),
                    "the Repository recipe requires every corner");
        }
        helper.succeed();
    }

    @GameTest(template = "repository_test")
    public static void tierUpgradePreservesInventoryAndCatalog(GameTestHelper helper) {
        helper.setBlock(CATALOG, BlockRegistry.REPOSITORY_CONTROLLER.get());
        helper.setBlock(IRON, BlockRegistry.REPOSITORY.get());
        helper.setBlock(REGULAR, ArsSophisticated.IRON_REPOSITORY.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var name = Component.literal("Catalog bridge");
        RepositoryTile basic = helper.getBlockEntity(IRON);
        var namedBlock = new ItemStack(BlockRegistry.REPOSITORY.get());
        namedBlock.set(DataComponents.CUSTOM_NAME, name);
        basic.applyComponentsFromItemStack(namedBlock);
        basic.setItem(53, new ItemStack(Items.DIAMOND, 12));
        helper.runAfterDelay(3, () -> {
            RepositoryCatalogTile catalog = helper.getBlockEntity(CATALOG);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 54 + 81, "initial catalog size");

            ItemStack wrongUpgrade = new ItemStack(ModItems.COPPER_TO_IRON_TIER_UPGRADE.get());
            helper.assertValueEqual(clickWithUpgrade(helper, player, wrongUpgrade), InteractionResult.PASS, "wrong starting tier");
            helper.assertValueEqual(wrongUpgrade.getCount(), 1, "incompatible upgrade is kept");
            helper.assertBlockPresent(BlockRegistry.REPOSITORY.get(), IRON);

            ItemStack basicToCopper = new ItemStack(ModItems.BASIC_TO_COPPER_TIER_UPGRADE.get(), 2);
            helper.assertValueEqual(clickWithUpgrade(helper, player, basicToCopper), InteractionResult.SUCCESS, "Basic to Copper");
            helper.assertValueEqual(basicToCopper.getCount(), 1, "one upgrade is consumed");
            helper.assertBlockPresent(ArsSophisticated.COPPER_REPOSITORY.get(), IRON);
            UpgradedRepositoryBlockEntity copper = helper.getBlockEntity(IRON);
            helper.assertValueEqual(copper.getCustomName(), name, "name transferred from Ars Nouveau");
            var wrapper = copper.getStorageWrapper();
            helper.assertValueEqual(wrapper.getInventoryHandler().getStackInSlot(53).getCount(), 12, "original slot is preserved");
            var stackUpgrade = ModItems.STACK_UPGRADE_TIER_1.get();
            wrapper.getUpgradeHandler().setStackInSlot(0, new ItemStack(stackUpgrade));
            wrapper.getInventoryHandler().setStackInSlot(71, new ItemStack(Items.COBBLESTONE, 128));

            helper.assertValueEqual(clickWithUpgrade(helper, player, wrongUpgrade), InteractionResult.SUCCESS, "Copper to Iron");
            helper.assertTrue(wrongUpgrade.isEmpty(), "the Copper to Iron upgrade is consumed");
            helper.assertBlockPresent(ArsSophisticated.IRON_REPOSITORY.get(), IRON);

            var creative = helper.makeMockPlayer(GameType.CREATIVE);
            GameType.CREATIVE.updatePlayerAbilities(creative.getAbilities());
            ItemStack directUpgrade = new ItemStack(ModItems.IRON_TO_NETHERITE_TIER_UPGRADE.get());
            helper.assertValueEqual(clickWithUpgrade(helper, creative, directUpgrade), InteractionResult.SUCCESS, "Iron directly to Netherite");
            helper.assertValueEqual(directUpgrade.getCount(), 1, "creative players keep their upgrade");
            helper.assertBlockPresent(ArsSophisticated.NETHERITE_REPOSITORY.get(), IRON);
            UpgradedRepositoryBlockEntity netherite = helper.getBlockEntity(IRON);
            var storage = netherite.getStorageWrapper();
            helper.assertValueEqual(netherite.getCustomName(), name, "custom name survives repeated upgrades");
            helper.assertValueEqual(storage.getInventoryHandler().getSlots(), 168, "new storage capacity");
            helper.assertValueEqual(storage.getUpgradeHandler().getSlots(), 4, "new upgrade capacity");
            helper.assertValueEqual(storage.getInventoryHandler().getStackInSlot(53).getCount(), 12, "Ars Nouveau items survive repeated upgrades");
            helper.assertValueEqual(storage.getInventoryHandler().getStackInSlot(71).getCount(), 128, "oversized stack is preserved");
            helper.assertTrue(storage.getUpgradeHandler().getStackInSlot(0).is(stackUpgrade), "installed stack upgrade is preserved");
            helper.assertItemEntityNotPresent(Items.DIAMOND, IRON, 2);
            helper.assertItemEntityNotPresent(Items.COBBLESTONE, IRON, 2);
            helper.assertItemEntityNotPresent(stackUpgrade, IRON, 2);
            helper.assertValueEqual(catalog.getControllerInv().getSlots(), 168 + 81, "upgraded bridge stays connected to the catalog");
            helper.succeed();
        });
    }

    private static InteractionResult clickWithUpgrade(GameTestHelper helper, Player player, ItemStack upgrade) {
        BlockPos pos = helper.absolutePos(IRON);
        player.setItemInHand(InteractionHand.MAIN_HAND, upgrade);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        return upgrade.onItemUseFirst(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
    }

    @GameTest(template = "repository_test")
    public static void oldDiamondAndNetheriteInventoriesExpand(GameTestHelper helper) {
        helper.setBlock(IRON, ArsSophisticated.DIAMOND_REPOSITORY.get());
        helper.setBlock(REGULAR, ArsSophisticated.NETHERITE_REPOSITORY.get());
        helper.runAfterDelay(3, () -> {
            loadLegacyRepository(helper, IRON, 135);
            loadLegacyRepository(helper, REGULAR, 162);
            helper.runAfterDelay(3, () -> {
                assertExpandedRepository(helper, IRON, 135, 144);
                assertExpandedRepository(helper, REGULAR, 162, 168);
                helper.succeed();
            });
        });
    }

    private static void loadLegacyRepository(GameTestHelper helper, BlockPos pos, int oldSlots) {
        UpgradedRepositoryBlockEntity repository = helper.getBlockEntity(pos);
        var storage = repository.getStorageWrapper();
        repository.changeStorageSize(oldSlots - storage.getNumberOfInventorySlots(), 0);
        storage.getUpgradeHandler().setStackInSlot(1, new ItemStack(ModItems.STACK_UPGRADE_TIER_1.get()));
        storage.getInventoryHandler().setStackInSlot(oldSlots - 1, new ItemStack(Items.COBBLESTONE, 128));

        var level = helper.getLevel();
        var registries = level.registryAccess();
        var saved = repository.saveWithFullMetadata(registries);
        int savedSlots = saved.getCompound(StorageBlockEntity.STORAGE_WRAPPER_TAG).getInt("numberOfInventorySlots");
        helper.assertValueEqual(savedSlots, oldSlots, "fixture uses the original saved capacity");
        var restored = (UpgradedRepositoryBlockEntity) BlockEntity.loadStatic(
                repository.getBlockPos(), repository.getBlockState(), saved, registries);
        helper.assertTrue(restored != null, "the old block entity must load");
        level.setBlockEntity(restored);
    }

    private static void assertExpandedRepository(GameTestHelper helper, BlockPos pos, int oldSlots, int expectedSlots) {
        UpgradedRepositoryBlockEntity repository = helper.getBlockEntity(pos);
        var storage = repository.getStorageWrapper();
        var inventory = storage.getInventoryHandler();
        helper.assertValueEqual(inventory.getSlots(), expectedSlots, "size after migration");
        helper.assertValueEqual(inventory.getStackInSlot(oldSlots - 1).getCount(), 128, "existing stack after migration");
        helper.assertTrue(storage.getUpgradeHandler().getStackInSlot(1).is(ModItems.STACK_UPGRADE_TIER_1.get()),
                "existing stack upgrade after migration");
        helper.assertTrue(inventory.getStackInSlot(expectedSlots - 1).isEmpty(), "new slots start empty");
        var registries = helper.getLevel().registryAccess();
        var saved = repository.saveWithFullMetadata(registries);
        var restored = (UpgradedRepositoryBlockEntity) BlockEntity.loadStatic(
                repository.getBlockPos(), repository.getBlockState(), saved, registries);
        helper.assertTrue(restored != null, "the migrated block entity must reload");
        helper.assertValueEqual(restored.getStorageWrapper().getInventoryHandler().getSlots(), expectedSlots, "saved expanded size");
    }

    @GameTest(template = "repository_test")
    public static void booksFollowEachRepositoryInventory(GameTestHelper helper) {
        var block = ArsSophisticated.IRON_REPOSITORY.get();
        var level = helper.getLevel();
        var pos = helper.absolutePos(IRON);
        var books = UpgradedRepositoryBlock.BOOKS;
        helper.setBlock(IRON, block);
        helper.setBlock(REGULAR, ArsSophisticated.GOLD_REPOSITORY.get());

        helper.startSequence()
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 0);
                    var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    items.insertItem(0, new ItemStack(Items.COBBLESTONE), true);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 0);
                    var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    items.insertItem(0, new ItemStack(Items.COBBLESTONE), false);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 1);
                    helper.assertBlockProperty(REGULAR, books, 0);
                    UpgradedRepositoryBlockEntity repository = helper.getBlockEntity(IRON);
                    var inventory = repository.getStorageWrapper().getInventoryHandler();
                    for (int slot = 1; slot < inventory.getSlots(); slot++) {
                        inventory.setStackInSlot(slot, new ItemStack(Items.COBBLESTONE));
                    }
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 10);
                    var items = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                    for (int slot = (items.getSlots() + 1) / 2; slot < items.getSlots(); slot++) {
                        items.extractItem(slot, 1, false);
                    }
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 5);
                    UpgradedRepositoryBlockEntity repository = helper.getBlockEntity(IRON);
                    var registries = level.registryAccess();
                    var saved = repository.saveWithFullMetadata(registries);
                    var oldState = repository.getBlockState().setValue(books, 0);
                    helper.setBlock(IRON, oldState);
                    var restored = BlockEntity.loadStatic(pos, oldState, saved, registries);
                    helper.assertTrue(restored != null, "repository saved without its visual fill must load");
                    level.setBlockEntity(restored);
                })
                .thenExecuteAfter(3, () -> {
                    helper.assertBlockProperty(IRON, books, 5);
                    UpgradedRepositoryBlockEntity repository = helper.getBlockEntity(IRON);
                    repository.clearContent();
                })
                .thenExecuteAfter(3, () -> helper.assertBlockProperty(IRON, books, 0))
                .thenSucceed();
    }

    @GameTest(template = "repository_test")
    public static void repositoriesAppearTogetherInArsCreativeTab(GameTestHelper helper) {
        var parameters = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, false, helper.getLevel().registryAccess());
        var arsTab = CreativeTabRegistry.BLOCKS.get();
        arsTab.buildContents(parameters);

        var repositories = List.of(ArsSophisticated.COPPER_REPOSITORY_ITEM.get(), ArsSophisticated.IRON_REPOSITORY_ITEM.get(),
                ArsSophisticated.GOLD_REPOSITORY_ITEM.get(), ArsSophisticated.DIAMOND_REPOSITORY_ITEM.get(),
                ArsSophisticated.NETHERITE_REPOSITORY_ITEM.get());
        var entries = arsTab.getDisplayItems().stream().map(ItemStack::getItem).toList();
        int base = entries.indexOf(BlockRegistry.REPOSITORY.get().asItem());
        helper.assertTrue(base >= 0 && base + repositories.size() < entries.size(),
                "the base Repository and five tiers must be in the Ars Nouveau tab");
        helper.assertValueEqual(entries.subList(base + 1, base + 1 + repositories.size()), repositories,
                "the new tiers appear in order after the base Repository");

        var functionalTab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(CreativeModeTabs.FUNCTIONAL_BLOCKS);
        functionalTab.buildContents(parameters);
        helper.assertTrue(functionalTab.getDisplayItems().stream().map(ItemStack::getItem).toList().containsAll(repositories),
                "the repositories remain available in Functional Blocks");
        helper.succeed();
    }
}
