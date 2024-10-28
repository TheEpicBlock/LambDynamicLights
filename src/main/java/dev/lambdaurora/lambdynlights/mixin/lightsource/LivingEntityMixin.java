/*
 * Copyright © 2020 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.mixin.lightsource;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin {
	@Override
	public void dynamicLightTick() {
		if (this.isOnFire() || this.isCurrentlyGlowing()) {
			this.lambdynlights$luminance = 15;
		} else {
			this.lambdynlights$luminance = LambDynLights.getLivingEntityLuminanceFromItems((LivingEntity) (Object) this);
		}

		int luminance = DynamicLightHandlers.getLuminanceFrom((Entity) (Object) this);
		if (luminance > this.lambdynlights$luminance)
			this.lambdynlights$luminance = luminance;
	}
}
