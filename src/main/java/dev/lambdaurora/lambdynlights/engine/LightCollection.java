/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine;

import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupCollectionEntry;
import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupEntry;
import dev.lambdaurora.lambdynlights.engine.source.DynamicLightSource;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public record LightCollection(Collection<LightCollectionEntry> entries) implements DynamicLightSource {
	public Stream<SpatialLookupEntry> splitIntoDynamicLightEntries() {
		record Data(LongList position, ByteList luminance) {}
		var cellKeyToData = new Int2ObjectOpenHashMap<Data>();

		for (var entry : this.entries) {
			int cellKey = DynamicLightingEngine.hashAt(entry.x(), entry.y(), entry.z());

			var data = cellKeyToData.computeIfAbsent(cellKey, k -> new Data(new LongArrayList(), new ByteArrayList()));

			data.position.add(BlockPos.asLong(entry.x(), entry.y(), entry.z()));
			data.luminance.add((byte) entry.luminance());
		}

		return cellKeyToData.int2ObjectEntrySet()
				.stream()
				.map(entry -> new SpatialLookupCollectionEntry(
						entry.getIntKey(),
						entry.getValue().position.toLongArray(),
						entry.getValue().luminance.toByteArray()
				));
	}

	@Override
	public LongSet getDynamicLightChunksToRebuild(boolean forced) {
		return null;
	}

	public static LightCollection cuboid(int startX, int startY, int startZ, int endX, int endY, int endZ, int luminance) {
		var entries = new ArrayList<LightCollectionEntry>();

		for (int x = startX; x <= endX; x++) {
			for (int y = startY; y <= endY; y++) {
				for (int z = startZ; z <= endZ; z++) {
					entries.add(new LightCollectionEntry(x, y, z, luminance));
				}
			}
		}

		return new LightCollection(entries);
	}
}
