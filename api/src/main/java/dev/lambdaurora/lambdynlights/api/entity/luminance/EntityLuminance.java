/*
 * Copyright © 2024 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights.api.entity.luminance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the luminance value of an entity.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 4.0.0
 */
public interface EntityLuminance {
	Codec<EntityLuminance> CODEC = Codec.withAlternative(
			Type.CODEC.dispatch(EntityLuminance::type, Type::codec),
			Value.DIRECT_CODEC
	);
	Codec<List<EntityLuminance>> LIST_CODEC = Codec.withAlternative(
			CODEC.listOf(),
			CODEC.xmap(List::of, List::getFirst)
	);

	/**
	 * {@return the type of this entity luminance}
	 */
	Type type();

	/**
	 * Gets the luminance of the given entity.
	 *
	 * @param entity the entity to get the luminance of
	 * @return the luminance of the given entity
	 */
	@Range(from = 0, to = 15)
	int getLuminance(Entity entity);

	/**
	 * Gets the luminance of the given entity out of the given list of entity luminance source.
	 *
	 * @param entity the entity to get the luminance of
	 * @param luminances the entity luminance sources
	 * @return the luminance of the given entity
	 */
	static @Range(from = 0, to = 15) int getLuminance(Entity entity, List<EntityLuminance> luminances) {
		int luminance = 0;

		for (var luminanceSource : luminances) {
			int value = luminanceSource.getLuminance(entity);

			if (value > luminance) {
				luminance = value;
			}
		}

		return luminance;
	}

	/**
	 * Represents a direct entity luminance value.
	 *
	 * @param luminance the luminance
	 */
	record Value(@Range(from = 0, to = 15) int luminance) implements EntityLuminance {
		public static final Codec<Value> DIRECT_CODEC = Brightness.LIGHT_VALUE_CODEC.xmap(Value::new, Value::luminance);
		public static final MapCodec<Value> CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(
						Brightness.LIGHT_VALUE_CODEC.fieldOf("value").forGetter(Value::luminance)
				).apply(instance, Value::new)
		);

		@Override
		public Type type() {
			return Type.VALUE;
		}

		@Override
		public @Range(from = 0, to = 15) int getLuminance(Entity entity) {
			return this.luminance;
		}
	}

	record Type(Identifier id, MapCodec<? extends EntityLuminance> codec) {
		private static final Map<Identifier, Type> TYPES = new Object2ObjectOpenHashMap<>();
		public static final Codec<Type> CODEC = Identifier.CODEC.flatXmap(
				name -> Optional.ofNullable(TYPES.get(name))
						.map(DataResult::success)
						.orElseGet(() -> DataResult.error(() -> "Unknown element name:" + name)),
				type -> DataResult.success(type.id)
		);

		public static final Type VALUE = register("value", Value.CODEC);
		public static final Type ENDERMAN = registerSimple("enderman", EndermanLuminance.INSTANCE);
		public static final Type FALLING_BLOCK = registerSimple("falling_block", FallingBlockLuminance.INSTANCE);
		public static final Type GLOW_SQUID = registerSimple("glow_squid", GlowSquidLuminance.INSTANCE);
		public static final Type MINECART_DISPLAY_BLOCK = registerSimple(
				"minecart_display_block", MinecartDisplayBlockLuminance.INSTANCE
		);

		public static Type register(Identifier id, MapCodec<? extends EntityLuminance> codec) {
			var type = new Type(id, codec);
			TYPES.put(id, type);
			return type;
		}

		private static Type register(String name, MapCodec<? extends EntityLuminance> codec) {
			return register(Identifier.of("lambdynlights", name), codec);
		}

		public static Type registerSimple(Identifier id, EntityLuminance singleton) {
			return register(id, MapCodec.unit(singleton));
		}

		private static Type registerSimple(String name, EntityLuminance singleton) {
			return registerSimple(Identifier.of("lambdynlights", name), singleton);
		}
	}
}
