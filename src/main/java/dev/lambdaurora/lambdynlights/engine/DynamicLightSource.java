/*
 * Copyright © 2020 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine;

import dev.lambdaurora.lambdynlights.LambDynLights;
import net.minecraft.world.level.Level;

/**
 * Represents a dynamic light source.
 *
 * @author LambdAurora
 * @version 3.3.0
 * @since 1.0.0
 */
public interface DynamicLightSource {
	/**
	 * {@return the dynamic light source X-coordinate}
	 */
	double getDynamicLightX();

	/**
	 * {@return the dynamic light source Y-coordinate}
	 */
	double getDynamicLightY();

	/**
	 * {@return the dynamic light source Z-coordinate}
	 */
	double getDynamicLightZ();

	/**
	 * {@return the dynamic light source world}
	 */
	Level dynamicLightWorld();

	/**
	 * {@return {@code true} if the dynamic light is enabled, or {@code false} otherwise}
	 */
	default boolean isDynamicLightEnabled() {
		return LambDynLights.get().config.getDynamicLightsMode().isEnabled() && LambDynLights.get().containsLightSource(this);
	}

	void resetDynamicLight();

	/**
	 * {@return the luminance of the light source}
	 * The maximum is 15, values below 1 are ignored.
	 */
	int getLuminance();

	/**
	 * Executed at each tick.
	 */
	void dynamicLightTick();
}
