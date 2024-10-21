/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine;

import dev.lambdaurora.lambdynlights.DynamicLightSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

/**
 * Represents the dynamic lighting engine.
 *
 * @author LambdAurora, Akarys
 * @version 3.1.0
 * @since 3.1.0
 */
public final class DynamicLightingEngine {
	private static final double MAX_RADIUS = 7.75;
	private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
	private static final int CELL_SIZE = MathHelper.ceil(MAX_RADIUS);
	public static final int MAX_LIGHT_SOURCES = 1024;
	private static final Vec3i[] CELL_OFFSETS;

	private final SpatialLookupEntry[] spatialLookupEntries = new SpatialLookupEntry[MAX_LIGHT_SOURCES];
	private final int[] startIndices = new int[MAX_LIGHT_SOURCES];

	/**
	 * Returns the dynamic light level at the specified position.
	 *
	 * @param pos the position
	 * @return the dynamic light level at the specified position
	 */
	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		double result = 0;

		var currentCell = new BlockPos.Mutable(
				this.positionToCell(pos.getX()),
				this.positionToCell(pos.getY()),
				this.positionToCell(pos.getZ())
		);
		var cell = currentCell.immutable();

		for (var cellOffset : CELL_OFFSETS) {
			currentCell.setWithOffset(cell, cellOffset);

			int key = this.getHashFromKey(this.hashCell(currentCell.getX(), currentCell.getY(), currentCell.getZ()));
			int startIndex = this.startIndices[key];

			for (int i = startIndex; i < this.spatialLookupEntries.length; i++) {
				SpatialLookupEntry entry = this.spatialLookupEntries[i];
				if (entry == null || entry.cellKey() != key) break;

				result = maxDynamicLightLevel(pos, entry.source(), result);
			}
		}

		return MathHelper.clamp(result, 0, 15);
	}

	/**
	 * Returns the dynamic light level generated by the light source at the specified position.
	 *
	 * @param pos the position
	 * @param lightSource the light source
	 * @param currentLightLevel the current surrounding dynamic light level
	 * @return the dynamic light level at the specified position
	 */
	public static double maxDynamicLightLevel(@NotNull BlockPos pos, @NotNull DynamicLightSource lightSource, double currentLightLevel) {
		int luminance = lightSource.getLuminance();
		if (luminance > 0) {
			// Can't use Entity#squaredDistanceTo because of eye Y coordinate.
			double dx = pos.getX() - lightSource.getDynamicLightX() + 0.5;
			double dy = pos.getY() - lightSource.getDynamicLightY() + 0.5;
			double dz = pos.getZ() - lightSource.getDynamicLightZ() + 0.5;

			double distanceSquared = dx * dx + dy * dy + dz * dz;
			// 7.75 because else we would have to update more chunks and that's not a good idea.
			// 15 (max range for blocks) would be too much and a bit cheaty.
			if (distanceSquared <= MAX_RADIUS_SQUARED) {
				double multiplier = 1.0 - Math.sqrt(distanceSquared) / MAX_RADIUS;
				double lightLevel = multiplier * (double) luminance;
				if (lightLevel > currentLightLevel) {
					return lightLevel;
				}
			}
		}
		return currentLightLevel;
	}

	private int positionToCell(int coord) {
		return coord / CELL_SIZE;
	}

	private int hashCell(int cellX, int cellY, int cellZ) {
		return Math.abs(cellX * 751 + cellY * 86399 + cellZ * 284593);
	}

	private int getHashFromKey(int hash) {
		return hash % MAX_LIGHT_SOURCES;
	}

	public void computeSpatialLookup(Collection<DynamicLightSource> dynamicLightSources) {
		Arrays.fill(this.spatialLookupEntries, null);
		Arrays.fill(this.startIndices, Integer.MAX_VALUE);

		int i = 0;
		for (var source : dynamicLightSources) {
			int x = (int) source.getDynamicLightX();
			int y = (int) source.getDynamicLightY();
			int z = (int) source.getDynamicLightZ();

			int cellKey = this.getHashFromKey(
					this.hashCell(
							this.positionToCell(x),
							this.positionToCell(y),
							this.positionToCell(z)
					)
			);

			this.spatialLookupEntries[i] = new SpatialLookupEntry(cellKey, source);

			i++;
			if (i == MAX_LIGHT_SOURCES) break;
		}

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
