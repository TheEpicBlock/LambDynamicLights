/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.api.behavior;

import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.OptionalInt;

/**
 * Represents a dynamic lighting behavior that is similar to a beacon beam.
 *
 * @param x the X-coordinate of the beam
 * @param y the Y-coordinate of the start of the beam, if absent then the start is the bottom of the world
 * @param z the Z-coordinate of the beam
 * @param luminance the luminance of the beam
 * @param level the world in which the beam is in
 * @author LambdAurora, Akarys
 * @version 4.0.0
 * @since 4.0.0
 */
public record BeaconLightBehavior(int x, OptionalInt y, int z, @Range(from = 0, to = 15) int luminance, Level level) implements DynamicLightBehavior {
	public BeaconLightBehavior(int x, int z, @Range(from = 0, to = 15) int luminance, Level level) {
		this(x, OptionalInt.empty(), z, luminance, level);
	}

	private int getWorldBottom() {
		return this.level.getMinY();
	}

	private int getWorldTop() {
		return this.level.getMaxY();
	}

	@Override
	public @NotNull BoundingBox getBoundingBox() {
		return new BoundingBox(this.x, this.y.orElse(this.getWorldBottom()), this.z, this.x + 1, this.getWorldTop(), this.z + 1);
	}

	@Override
	public @Range(from = 0, to = 15) double lightAtPos(double x, double y, double z, double falloffRatio) {
		double dx = x - this.x;
		double dz = z - this.z;

		double distanceSquared = dx * dx + dz * dz;

		if (this.y.isPresent() && y < this.y.getAsInt()) {
			double dy = y - this.y.getAsInt();
			distanceSquared += dy * dy;
		}

		return luminance - Math.sqrt(distanceSquared) * falloffRatio;
	}

	@Override
	public boolean hasChanged() {
		return false;
	}
}
