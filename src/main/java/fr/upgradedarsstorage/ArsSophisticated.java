package fr.upgradedarsstorage;

import com.hollingsworth.arsnouveau.setup.registry.BlockRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

@Mod(ArsSophisticated.MOD_ID)
public final class ArsSophisticated {
    public static final String MOD_ID = "ars_sophisticated";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final DeferredBlock<UpgradedRepositoryBlock> COPPER_REPOSITORY =
            BLOCKS.register("copper_repository", () -> new UpgradedRepositoryBlock(72, 1));
    public static final DeferredBlock<UpgradedRepositoryBlock> IRON_REPOSITORY =
            BLOCKS.register("iron_repository", () -> new UpgradedRepositoryBlock(81, 1));
    public static final DeferredBlock<UpgradedRepositoryBlock> GOLD_REPOSITORY =
            BLOCKS.register("gold_repository", () -> new UpgradedRepositoryBlock(108, 2));
    public static final DeferredBlock<UpgradedRepositoryBlock> DIAMOND_REPOSITORY =
            BLOCKS.register("diamond_repository", () -> new UpgradedRepositoryBlock(144, 3));
    public static final DeferredBlock<UpgradedRepositoryBlock> NETHERITE_REPOSITORY =
            BLOCKS.register("netherite_repository", () -> new UpgradedRepositoryBlock(168, 4));

    public static final Supplier<BlockItem> COPPER_REPOSITORY_ITEM = ITEMS.registerSimpleBlockItem(COPPER_REPOSITORY);
    public static final Supplier<BlockItem> IRON_REPOSITORY_ITEM = ITEMS.registerSimpleBlockItem(IRON_REPOSITORY);
    public static final Supplier<BlockItem> GOLD_REPOSITORY_ITEM = ITEMS.registerSimpleBlockItem(GOLD_REPOSITORY);
    public static final Supplier<BlockItem> DIAMOND_REPOSITORY_ITEM = ITEMS.registerSimpleBlockItem(DIAMOND_REPOSITORY);
    public static final Supplier<BlockItem> NETHERITE_REPOSITORY_ITEM = ITEMS.registerSimpleBlockItem(NETHERITE_REPOSITORY);

    // Keep a shared block entity type for every tier.
    public static final Supplier<BlockEntityType<UpgradedRepositoryBlockEntity>> REPOSITORY_ENTITY =
            BLOCK_ENTITIES.register("iron_repository",
                    () -> BlockEntityType.Builder.of(UpgradedRepositoryBlockEntity::new, COPPER_REPOSITORY.get(),
                            IRON_REPOSITORY.get(), GOLD_REPOSITORY.get(), DIAMOND_REPOSITORY.get(),
                            NETHERITE_REPOSITORY.get()).build(null));

    public ArsSophisticated(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(this::addCreativeItems);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        var type = REPOSITORY_ENTITY.get();
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, UpgradedRepositoryBlockEntity::getExternalItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, UpgradedRepositoryBlockEntity::getExternalFluidHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, UpgradedRepositoryBlockEntity::getExternalEnergyStorage);
        event.registerBlockEntity(CapabilityRegistry.MAP_INV_CAP, type, (repository, side) -> repository);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        boolean arsTab = event.getTabKey().equals(CreativeTabRegistry.BLOCKS.getKey());
        if (!arsTab && !event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            return;
        }

        ItemStack previous = arsTab ? BlockRegistry.REPOSITORY.get().asItem().getDefaultInstance() : ItemStack.EMPTY;
        for (var repository : List.of(COPPER_REPOSITORY_ITEM, IRON_REPOSITORY_ITEM, GOLD_REPOSITORY_ITEM,
                DIAMOND_REPOSITORY_ITEM, NETHERITE_REPOSITORY_ITEM)) {
            ItemStack stack = repository.get().getDefaultInstance();
            if (event.getParentEntries().contains(previous) && event.getSearchEntries().contains(previous)) {
                event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } else {
                event.accept(stack);
            }
            previous = stack;
        }
    }
}
