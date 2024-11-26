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

public interface BeaconBlockEntityLightSource {
	int lambdynlights$getLevels();

	BeaconLightBehavior lambdynlights$getDynamicLightBeam();

	void lambdynlights$setDynamicLightBeam(BeaconLightBehavior beam);
}
