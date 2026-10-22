package net.chris.flushabletoilet;

import net.chris.flushabletoilet.block.ToiletBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlushableToilet implements ModInitializer {
	public static final String MOD_ID = "flushabletoilet";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier TOILET_ID = Identifier.fromNamespaceAndPath(MOD_ID, "toilet");
	public static final ResourceKey<Block> TOILET_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, TOILET_ID);
	public static final ResourceKey<Item> TOILET_ITEM_KEY = ResourceKey.create(Registries.ITEM, TOILET_ID);

	// Vanilla keeps its tab keys private, so build the key for the Functional Blocks tab ourselves.
	private static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("functional_blocks"));

	public static final Block TOILET = Registry.register(BuiltInRegistries.BLOCK, TOILET_BLOCK_KEY,
			new ToiletBlock(BlockBehaviour.Properties.of()
					.setId(TOILET_BLOCK_KEY)
					.mapColor(MapColor.SNOW)
					.strength(1.5f, 6.0f)
					.requiresCorrectToolForDrops()
					.sound(SoundType.STONE)
					.pushReaction(PushReaction.IMMOVEABLE)
					.noOcclusion()));

	public static final Item TOILET_ITEM = Registry.register(BuiltInRegistries.ITEM, TOILET_ITEM_KEY,
			new BlockItem(TOILET, new Item.Properties()
					.setId(TOILET_ITEM_KEY)
					.useBlockDescriptionPrefix()));

	@Override
	public void onInitialize() {
		CreativeModeTabEvents.modifyOutputEvent(FUNCTIONAL_BLOCKS).register(output -> output.accept(TOILET_ITEM));
		LOGGER.info("Flushable Toilet installed. Please wash your hands.");
	}
}
