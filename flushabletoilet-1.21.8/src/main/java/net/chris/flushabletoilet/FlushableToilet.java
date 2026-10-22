package net.chris.flushabletoilet;

import net.chris.flushabletoilet.block.ToiletBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlushableToilet implements ModInitializer {
	public static final String MOD_ID = "flushabletoilet";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier TOILET_ID = Identifier.of(MOD_ID, "toilet");
	public static final RegistryKey<Block> TOILET_BLOCK_KEY = RegistryKey.of(RegistryKeys.BLOCK, TOILET_ID);
	public static final RegistryKey<Item> TOILET_ITEM_KEY = RegistryKey.of(RegistryKeys.ITEM, TOILET_ID);

	public static final Block TOILET = Registry.register(Registries.BLOCK, TOILET_BLOCK_KEY,
			new ToiletBlock(AbstractBlock.Settings.create()
					.registryKey(TOILET_BLOCK_KEY)
					.mapColor(MapColor.WHITE)
					.strength(1.5f, 6.0f)
					.requiresTool()
					.sounds(BlockSoundGroup.STONE)
					.pistonBehavior(PistonBehavior.BLOCK)
					.nonOpaque()));

	public static final Item TOILET_ITEM = Registry.register(Registries.ITEM, TOILET_ITEM_KEY,
			new BlockItem(TOILET, new Item.Settings()
					.registryKey(TOILET_ITEM_KEY)
					.useBlockPrefixedTranslationKey()));

	@Override
	public void onInitialize() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(TOILET_ITEM));
		LOGGER.info("Flushable Toilet installed. Please wash your hands.");
	}
}
