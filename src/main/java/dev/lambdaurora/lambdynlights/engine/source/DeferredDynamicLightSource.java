/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine.source;

import dev.lambdaurora.lambdynlights.api.behavior.DynamicLightBehavior;
import dev.lambdaurora.lambdynlights.engine.DynamicLightingEngine;
import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupDeferredEntry;
import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupEntry;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.stream.Stream;

/**
 * Represents a dynamic light source which is deferred to a {@link DynamicLightBehavior}.
 *
 * @author LambdAurora, Akarys
 * @version 4.0.0
 * @since 4.0.0
 */
public record DeferredDynamicLightSource(DynamicLightBehavior behavior) implements DynamicLightSource {
	@Override
	public Stream<SpatialLookupEntry> splitIntoDynamicLightEntries() {
		DynamicLightBehavior.BoundingBox boundingBox = this.behavior.getBoundingBox();

		var chunks = new ArrayList<SpatialLookupEntry>();

		int cellEndX = DynamicLightingEngine.positionToCell(boundingBox.endX());
		int cellEndY = DynamicLightingEngine.positionToCell(boundingBox.endY());
		int cellEndZ = DynamicLightingEngine.positionToCell(boundingBox.endZ());

		for (int x = DynamicLightingEngine.positionToCell(boundingBox.startX()); x <= cellEndX; x++) {
			for (int y = DynamicLightingEngine.positionToCell(boundingBox.startY()); y <= cellEndY; y++) {
				for (int z = DynamicLightingEngine.positionToCell(boundingBox.startZ()); z <= cellEndZ; z++) {
					chunks.add(new SpatialLookupDeferredEntry(DynamicLightingEngine.hashCell(x, y, z), behavior));
				}
			}
		}

		return chunks.stream();
	}

	@Override
	public LongSet getDynamicLightChunksToRebuild(boolean forced) {
		if (!forced && !this.behavior.hasChanged()) {
			return LongSet.of();
		}

		DynamicLightBehavior.BoundingBox boundingBox = this.behavior.getBoundingBox();

		var chunks = new LongOpenHashSet();

		int chunkStartX = getStartChunk(boundingBox.startX());
		int chunkStartY = getStartChunk(boundingBox.startY());
		int chunkStartZ = getStartChunk(boundingBox.startZ());
		int chunkEndX = getEndChunk(boundingBox.endX());
		int chunkEndY = getEndChunk(boundingBox.endY());
		int chunkEndZ = getEndChunk(boundingBox.endZ());

		for (int x = chunkStartX; x <= chunkEndX; x++) {
			for (int y = chunkStartY; y <= chunkEndY; y++) {
				for (int z = chunkStartZ; z <= chunkEndZ; z++) {
					chunks.add(ChunkSectionPos.asLong(x, y, z));
				}
			}
		}

		return chunks;
	}

	private static int getStartChunk(int blockPos) {
		int chunkStart = ChunkSectionPos.blockToSectionCoord(blockPos);

		if ((MathHelper.floor(blockPos) & 15) < 8) {
			chunkStart--;
		}

		return chunkStart;
	}

	private static int getEndChunk(int blockPos) {
		int chunkStart = ChunkSectionPos.blockToSectionCoord(blockPos);

		if ((MathHelper.floor(blockPos) & 15) >= 8) {
			chunkStart++;
		}

		return chunkStart;
	}
}
