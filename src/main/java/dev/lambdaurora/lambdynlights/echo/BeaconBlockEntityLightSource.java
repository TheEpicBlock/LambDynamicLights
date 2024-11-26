/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.echo;

import dev.lambdaurora.lambdynlights.api.behavior.BeaconLightBehavior;

/**
 * Represents a beacon beam light source.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
public interface BeaconBlockEntityLightSource {
	int lambdynlights$getLevels();

	BeaconLightBehavior lambdynlights$getDynamicLightBeam();

	void lambdynlights$setDynamicLightBeam(BeaconLightBehavior beam);
}
