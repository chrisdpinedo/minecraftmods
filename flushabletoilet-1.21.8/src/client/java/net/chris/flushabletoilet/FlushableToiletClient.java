package net.chris.flushabletoilet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.world.BiomeColors;

public class FlushableToiletClient implements ClientModInitializer {
	private static final int DEFAULT_WATER_COLOR = 0x3F76E4;

	@Override
	public void onInitializeClient() {
		// Tint the bowl water (tintindex 0) with the local biome's water color.
		ColorProviderRegistry.BLOCK.register((state, world, pos, tintIndex) ->
				world != null && pos != null ? BiomeColors.getWaterColor(world, pos) : DEFAULT_WATER_COLOR,
				FlushableToilet.TOILET);
	}
}
