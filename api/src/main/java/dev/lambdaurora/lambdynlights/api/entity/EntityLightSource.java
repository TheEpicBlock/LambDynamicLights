/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.api.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lambdaurora.lambdynlights.api.entity.luminance.EntityLuminance;
import net.minecraft.advancements.critereon.*;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Optional;

/**
 * Represents an entity light source.
 *
 * @param predicate the predicate to select which entities emit the given luminance
 * @param luminances the luminance sources
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
public record EntityLightSource(EntityPredicate predicate, List<EntityLuminance> luminances) {
	public static final Codec<EntityLightSource> CODEC = RecordCodecBuilder.create(
			instance -> instance.group(
							EntityPredicate.CODEC.fieldOf("match").forGetter(EntityLightSource::predicate),
							EntityLuminance.LIST_CODEC.fieldOf("luminance").forGetter(EntityLightSource::luminances)
					)
					.apply(instance, EntityLightSource::new)
	);

	/**
	 * Gets the luminance of the entity.
	 *
	 * @param entity the entity
	 * @return the luminance value between {@code 0} and {@code 15}
	 */
	public @Range(from = 0, to = 15) int getLuminance(Entity entity) {
		if (this.predicate.test(entity)) {
			return EntityLuminance.getLuminance(entity, this.luminances);
		}

		return 0;
	}

	public record EntityPredicate(
			Optional<EntityTypePredicate> entityType,
			Optional<MobEffectsPredicate> effects,
			Optional<EntityFlagsPredicate> flags,
			Optional<EntityEquipmentPredicate> equipment,
			Optional<EntityPredicate> vehicle,
			Optional<EntityPredicate> passenger,
			Optional<SlotsPredicate> slots
	) {
		public static final Codec<EntityPredicate> CODEC = Codec.recursive(
				"EntityPredicate",
				codec -> RecordCodecBuilder.create(
						instance -> instance.group(
										EntityTypePredicate.CODEC.optionalFieldOf("type").forGetter(EntityPredicate::entityType),
										MobEffectsPredicate.CODEC.optionalFieldOf("effects").forGetter(EntityPredicate::effects),
										EntityFlagsPredicate.CODEC.optionalFieldOf("flags").forGetter(EntityPredicate::flags),
										EntityEquipmentPredicate.CODEC.optionalFieldOf("equipment").forGetter(EntityPredicate::equipment),
										codec.optionalFieldOf("vehicle").forGetter(EntityPredicate::vehicle),
										codec.optionalFieldOf("passenger").forGetter(EntityPredicate::passenger),
										SlotsPredicate.CODEC.optionalFieldOf("slots").forGetter(EntityPredicate::slots)
								)
								.apply(instance, EntityPredicate::new)
				)
		);

		public boolean test(Entity entity) {
			if (entity == null) {
				return false;
			} else if (this.entityType.isPresent() && !this.entityType.get().matches(entity.getType())) {
				return false;
			} else if (this.effects.isPresent() && !this.effects.get().matches(entity)) {
				return false;
			} else if (this.flags.isPresent() && !this.flags.get().matches(entity)) {
				return false;
			} else if (this.equipment.isPresent() && !this.equipment.get().matches(entity)) {
				return false;
			} else if (this.vehicle.isPresent() && !this.vehicle.get().test(entity.getVehicle())) {
				return false;
			} else if (this.passenger.isPresent()
					&& entity.getPassengers().stream().noneMatch(passenger -> this.passenger.get().test(passenger))) {
				return false;
			} else {
				return this.slots.isEmpty() || this.slots.get().matches(entity);
			}
		}
	}
}
