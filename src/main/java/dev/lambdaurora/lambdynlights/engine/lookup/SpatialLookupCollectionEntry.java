/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine.lookup;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an entry made of a collection of light sources in a spatial lookup.
 *
 * @param cellKey the cell key of this entry
 * @param positions the positions of each light sources in the current cell
 * @param luminance the light values in the current cell
 * @author LambdAurora, Akarys
 * @version 4.0.0
 * @since 4.0.0
 */
public record SpatialLookupCollectionEntry(int cellKey, long[] positions, byte[] luminance) implements SpatialLookupEntry {
	@Override
	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		double maxLightLevel = 0.;

		for (int i = 0; i < this.luminance.length; i++) {
			int posIndex = i * 3;
			double x = this.positions[posIndex] + 0.5;
			double y = this.positions[posIndex + 1] + 0.5;
			double z = this.positions[posIndex + 2] + 0.5;
			byte luminance = this.luminance[i];

			double lightLevel = SpatialLookupEntry.lightAtPos(x, y, z, pos, luminance);

			if (lightLevel > maxLightLevel) {
				maxLightLevel = lightLevel;
			}
		}

		return maxLightLevel;
	}
}
