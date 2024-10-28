/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.engine;

import dev.lambdaurora.lambdynlights.LambDynLights;
import dev.lambdaurora.lambdynlights.api.DynamicLightHandlers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.ChunkSectionPos;
import net.minecraft.core.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the behavior of a dynamic light source.
 *
 * @author LambdAurora
 * @version 3.3.0
 * @since 3.3.0
 */
@ApiStatus.Internal
public interface DynamicLightSourceBehavior extends DynamicLightSource {
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
		var lightSource = (DynamicLightSourceBehavior) entity;

		if (entity.isRemoved()) {
			lightSource.setDynamicLightEnabled(false);
		} else {
			if (DynamicLightHandlers.canLightUp(entity)) {
				lightSource.dynamicLightTick();
			} else {
				lightSource.setLuminance(0);
			}
			LambDynLights.updateTracking(lightSource);
		}
	}

	default boolean lambdynlights$updateDynamicLight(@NotNull LevelRenderer renderer) {
		if (!this.shouldUpdateDynamicLight())
			return false;
		double x = this.getDynamicLightX();
		double y = this.getDynamicLightY();
		double z = this.getDynamicLightZ();
		double deltaX = x - this.getDynamicLightPrevX();
		double deltaY = y - this.getDynamicLightPrevY();
		double deltaZ = z - this.getDynamicLightPrevZ();

		int luminance = this.getLuminance();

		if (Math.abs(deltaX) > 0.1D || Math.abs(deltaY) > 0.1D || Math.abs(deltaZ) > 0.1D || luminance != this.getLastDynamicLuminance()) {
			this.updateDynamicLightPreviousCoordinates();
			this.setLastDynamicLuminance(luminance);

			var newPos = new LongOpenHashSet();

			if (luminance > 0) {
				var chunkPos = new BlockPos.Mutable(
						ChunkSectionPos.blockToSectionCoord(x),
						ChunkSectionPos.blockToSectionCoord(y),
						ChunkSectionPos.blockToSectionCoord(z)
				);

				LambDynLights.scheduleChunkRebuild(renderer, chunkPos);
				LambDynLights.updateTrackedChunks(chunkPos, this.lambdynlights$getTrackedLitChunkPos(), newPos);

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
					LambDynLights.scheduleChunkRebuild(renderer, chunkPos);
					LambDynLights.updateTrackedChunks(chunkPos, this.lambdynlights$getTrackedLitChunkPos(), newPos);
				}
			}

			// Schedules the rebuild of removed chunks.
			this.lambdynlights$scheduleTrackedChunksRebuild(renderer);
			// Update tracked lit chunks.
			this.lambdynlights$setTrackedLitChunkPos(newPos);
			return true;
		}
		return false;
	}

	LongOpenHashSet lambdynlights$getTrackedLitChunkPos();

	void lambdynlights$setTrackedLitChunkPos(LongOpenHashSet trackedLitChunkPos);

	@SuppressWarnings("resource")
	default void lambdynlights$scheduleTrackedChunksRebuild(@NotNull LevelRenderer renderer) {
		if (Minecraft.getInstance().level == this.dynamicLightWorld()) {
			for (long pos : this.lambdynlights$getTrackedLitChunkPos()) {
				LambDynLights.scheduleChunkRebuild(renderer, pos);
			}
		}
	}
}
