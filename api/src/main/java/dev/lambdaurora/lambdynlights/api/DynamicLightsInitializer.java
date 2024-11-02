/*
 * Copyright © 2020 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.api;

import dev.lambdaurora.lambdynlights.api.entity.EntityLightSourceManager;
import dev.lambdaurora.lambdynlights.api.item.ItemLightSourceManager;

/**
 * Represents the entrypoint for LambDynamicLights API.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 1.3.2
 */
public interface DynamicLightsInitializer {
	/**
	 * Called when LambDynamicLights is initialized to register custom dynamic light handlers and item light sources.
	 *
	 * @param itemLightSourceManager the manager for item light sources
	 * @param entityLightSourceManager the manager for entity light sources
	 */
	void onInitializeDynamicLights(ItemLightSourceManager itemLightSourceManager, EntityLightSourceManager entityLightSourceManager);
}
