/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.accessor.DynamicLightHandlerHolder;
import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupEntry;
import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupLightSourceEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Represents the dynamic lighting engine.
 *
 * @author LambdAurora, Akarys
 * @version 4.0.0
 * @since 3.1.0
 */
public final class DynamicLightingEngine {
	public static final double MAX_RADIUS = 7.75;
	public static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
	private static final int CELL_SIZE = MathHelper.ceil(MAX_RADIUS);
	public static final int MAX_LIGHT_SOURCES = 1024;
	private static final Vec3i[] CELL_OFFSETS;

	private final SpatialLookupEntry[] spatialLookupEntries = new SpatialLookupEntry[MAX_LIGHT_SOURCES];
	private final int[] startIndices = new int[MAX_LIGHT_SOURCES];

	/**
	 * Returns whether the given entity can light up or not.
	 *
	 * @param entity the entity
	 * @param <T> the type of the entity
	 * @return {@code true} if the entity can light up, or {@code false} otherwise
	 */
	public static <T extends Entity> boolean canLightUp(T entity) {
		if (entity == Minecraft.getInstance().player) {
			if (!LambDynLights.get().config.getSelfLightSource().get())
				return false;
		} else if (!LambDynLights.get().config.getEntitiesLightSource().get()) {
			return false;
		}

		var setting = DynamicLightHandlerHolder.cast(entity.getType()).lambdynlights$getSetting();
		return !(setting == null || !setting.get());
	}

	/**
	 * Returns the dynamic light level at the specified position.
	 *
	 * @param pos the position
	 * @return the dynamic light level at the specified position
	 */
	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		double result = 0;

		var currentCell = new BlockPos.Mutable(
				positionToCell(pos.getX()),
				positionToCell(pos.getY()),
				positionToCell(pos.getZ())
		);
		var cell = currentCell.immutable();

		for (var cellOffset : CELL_OFFSETS) {
			currentCell.setWithOffset(cell, cellOffset);

			int key = hashCell(currentCell.getX(), currentCell.getY(), currentCell.getZ());
			int startIndex = this.startIndices[key];

			for (int i = startIndex; i < this.spatialLookupEntries.length; i++) {
				SpatialLookupEntry entry = this.spatialLookupEntries[i];
				if (entry == null || entry.cellKey() != key) break;

				double light = entry.getDynamicLightLevel(pos);
				if (light > result) {
					result = light;
				}
			}
		}

		return MathHelper.clamp(result, 0, 15);
	}

	static int hashAt(int x, int y, int z) {
		return hashCell(
				positionToCell(x),
				positionToCell(y),
				positionToCell(z)
		);
	}

	private static int positionToCell(int coord) {
		return coord / CELL_SIZE;
	}

	private static int hashCell(int cellX, int cellY, int cellZ) {
		return Math.abs(cellX * 751 + cellY * 86399 + cellZ * 284593) % MAX_LIGHT_SOURCES;
	}

	public void computeSpatialLookup(Collection<? extends DynamicLightSource> dynamicLightSources, Collection<LightCollection> lightCollections) {
		Arrays.fill(this.spatialLookupEntries, null);
		Arrays.fill(this.startIndices, Integer.MAX_VALUE);

		int i = 0;

		for (var source : dynamicLightSources) {
			if (i == MAX_LIGHT_SOURCES) break;

			int x = (int) source.getDynamicLightX();
			int y = (int) source.getDynamicLightY();
			int z = (int) source.getDynamicLightZ();

			int cellKey = hashAt(x, y, z);
			this.spatialLookupEntries[i] = new SpatialLookupLightSourceEntry(cellKey, source);

			i++;
		}

		var lightCollectionChunks = lightCollections.stream().flatMap(LightCollection::split).toArray(SpatialLookupEntry[]::new);
		int maxChunks = Math.min(i + lightCollectionChunks.length, MAX_LIGHT_SOURCES) - i;
		System.arraycopy(lightCollectionChunks, 0, this.spatialLookupEntries, i, maxChunks);

		Arrays.sort(this.spatialLookupEntries, Comparator.comparingInt(entry -> entry == null ? Integer.MAX_VALUE : entry.cellKey()));

		for (i = 0; i < MAX_LIGHT_SOURCES; i++) {
			if (this.spatialLookupEntries[i] == null) break;

			int key = this.spatialLookupEntries[i].cellKey();
			int previousKey = i == 0 ? Integer.MAX_VALUE : this.spatialLookupEntries[i - 1].cellKey();

			if (key != previousKey) {
				this.startIndices[key] = i;
			}
		}
	}

	static {
		CELL_OFFSETS = new Vec3i[27];
		int i = 0;

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					CELL_OFFSETS[i] = new Vec3i(x, y, z);
					i++;
				}
			}
		}
	}
}
