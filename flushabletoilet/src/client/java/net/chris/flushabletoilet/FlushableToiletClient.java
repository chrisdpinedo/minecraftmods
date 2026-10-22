package net.chris.flushabletoilet;

import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

public class FlushableToiletClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Tint the bowl water (tintindex 0) with the local biome's water color, like a cauldron.
		BlockColorRegistry.register(List.of(BlockTintSources.water()), FlushableToilet.TOILET);
	}
}
