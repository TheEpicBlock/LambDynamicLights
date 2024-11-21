/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine.source;

import dev.lambdaurora.lambdynlights.engine.lookup.SpatialLookupEntry;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.stream.Stream;

/**
 * Represents a dynamic light source.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
public interface DynamicLightSource {
	/**
	 * Splits this dynamic light source into spatial lookup entries.
	 *
	 * @return a stream of spatial lookup entries this source is made of
	 */
	Stream<SpatialLookupEntry> splitIntoDynamicLightEntries();

	/**
	 * Computes the set of chunk sections to rebuild to display in-world the new light values.
	 *
	 * @param forced {@code true} if relevant chunk sections must be returned to rebuild the chunk sections (for example if the light source is removed),
	 * or {@code false} otherwise
	 * @return the set of chunk sections to rebuild, or an empty set if none is to be rebuilt
	 */
	LongSet getDynamicLightChunksToRebuild(boolean forced);
}
