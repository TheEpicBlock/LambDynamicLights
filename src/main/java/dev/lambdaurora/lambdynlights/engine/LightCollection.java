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
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

public record LightCollection(Collection<LightCollectionEntry> entries) {
	public Stream<SpatialLookupCollectionEntry> split() {
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

	/*public static LightCollection line(int startX, int startY, int startZ, int endX, int endY, int endZ, int luminance) {
		HashMap<Vec3i, Double> blocks = new HashMap<>();
	}*/
}
