/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.resource.entity.luminance;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import dev.lambdaurora.lambdynlights.resource.entity.EntityLightSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.jetbrains.annotations.Range;

/**
 * Represents the luminance value of an item entity.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
public final class ItemEntityLuminance implements EntityLuminance {
	public static final ItemEntityLuminance INSTANCE = new ItemEntityLuminance();

	private ItemEntityLuminance() {}

	@Override
	public Type type() {
		return EntityLightSources.ITEM;
	}

	@Override
	public @Range(from = 0, to = 15) int getLuminance(Entity entity) {
		if (entity instanceof ItemEntity itemEntity) {
			return LambDynLights.getLuminanceFromItemStack(itemEntity.getItem(), entity.isSubmergedInWater());
		}

		return 0;
	}
}
