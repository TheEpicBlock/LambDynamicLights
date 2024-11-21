/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine.source;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.engine.DynamicLightingEngine;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.ChunkSectionPos;
import net.minecraft.core.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;

/**
 * Represents the behavior of a dynamic light source.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
@ApiStatus.Internal
public interface EntityDynamicLightSourceBehavior extends EntityDynamicLightSource {
	/**
	 * {@return the dynamic light source previous X-coordinate}
	 */
	double getDynamicLightPrevX();

	/**
	 * {@return the dynamic light source previous Y-coordinate}
	 */
	double getDynamicLightPrevY();

	/**
	 * {@return the dynamic light source previous Z-coordinate}
	 */
	double getDynamicLightPrevZ();

	/**
	 * Updates the previous coordinates of this dynamic light source.
	 */
	void updateDynamicLightPreviousCoordinates();

	void setLuminance(int luminance);

	int getLastDynamicLuminance();

	void setLastDynamicLuminance(int luminance);

	/**
	 * Sets whether the dynamic light is enabled or not.
	 * <p>
	 * Note: please do not call this function in your mod, or you will break things.
	 *
	 * @param enabled {@code true} if the dynamic light is enabled, or {@code false} otherwise
	 */
	@ApiStatus.Internal
	default void setDynamicLightEnabled(boolean enabled) {
		this.resetDynamicLight();
		if (enabled)
			LambDynLights.get().addLightSource(this);
		else
			LambDynLights.get().removeLightSource(this);
	}

	/**
	 * {@return {@code true} if this dynamic light source should update, or {@code false} otherwise}
	 */
	boolean shouldUpdateDynamicLight();

	/**
	 * Ticks the given entity for dynamic lighting.
	 *
	 * @param entity the entity to tick
	 */
	static void tickEntity(Entity entity) {
		if (!LambDynLights.get().shouldTick()) return;

		var lightSource = (EntityDynamicLightSourceBehavior) entity;

		if (entity.isRemoved()) {
			lightSource.setDynamicLightEnabled(false);
		} else {
			if (DynamicLightingEngine.canLightUp(entity)) {
				lightSource.dynamicLightTick();
			} else {
				lightSource.setLuminance(0);
			}
			LambDynLights.updateTracking(lightSource);
		}
	}

	default LongSet getDynamicLightChunksToRebuild(boolean forced) {
		if (forced) {
			return this.lambdynlights$getTrackedLitChunkPos();
		}

		double x = this.getDynamicLightX();
		double y = this.getDynamicLightY();
		double z = this.getDynamicLightZ();
		double deltaX = x - this.getDynamicLightPrevX();
		double deltaY = y - this.getDynamicLightPrevY();
		double deltaZ = z - this.getDynamicLightPrevZ();

		int luminance = this.getLuminance();

		if (Math.abs(deltaX) <= 0.1 && Math.abs(deltaY) <= 0.1 && Math.abs(deltaZ) <= 0.1 && luminance == this.getLastDynamicLuminance()) {
			return LongSet.of();
		}

		var newPos = new LongOpenHashSet();

		if (luminance > 0) {
			this.gatherClosestChunks(newPos, x, y, z);
		}

		var result = new LongOpenHashSet(newPos);
		result.addAll(this.lambdynlights$getTrackedLitChunkPos());

		this.updateDynamicLightPreviousCoordinates();
		this.setLastDynamicLuminance(luminance);
		this.lambdynlights$setTrackedLitChunkPos(newPos);

		return result;
	}

	private void gatherClosestChunks(LongSet chunks, double x, double y, double z) {
		var chunkPos = new BlockPos.Mutable(
				ChunkSectionPos.blockToSectionCoord(x),
				ChunkSectionPos.blockToSectionCoord(y),
				ChunkSectionPos.blockToSectionCoord(z)
		);

		chunks.add(ChunkSectionPos.asLong(chunkPos.getX(), chunkPos.getY(), chunkPos.getZ()));

		var directionX = (MathHelper.floor(x) & 15) >= 8 ? Direction.EAST : Direction.WEST;
		var directionY = (MathHelper.floor(y) & 15) >= 8 ? Direction.UP : Direction.DOWN;
		var directionZ = (MathHelper.floor(z) & 15) >= 8 ? Direction.SOUTH : Direction.NORTH;

		for (int i = 0; i < 7; i++) {
			if (i % 4 == 0) {
				chunkPos.move(directionX); // X
			} else if (i % 4 == 1) {
				chunkPos.move(directionZ); // XZ
			} else if (i % 4 == 2) {
				chunkPos.move(directionX.getOpposite()); // Z
			} else {
				chunkPos.move(directionZ.getOpposite()); // origin
				chunkPos.move(directionY); // Y
			}
			chunks.add(ChunkSectionPos.asLong(chunkPos.getX(), chunkPos.getY(), chunkPos.getZ()));
		}
	}

	LongSet lambdynlights$getTrackedLitChunkPos();

	void lambdynlights$setTrackedLitChunkPos(LongSet trackedLitChunkPos);
}
