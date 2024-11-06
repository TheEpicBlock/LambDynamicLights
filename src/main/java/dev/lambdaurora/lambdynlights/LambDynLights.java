/*
 * Copyright © 2020 LambdAurora <email@lambdaurora.dev>
 *
 * This file is part of LambDynamicLights.
 *
 * Licensed under the Lambda License. For more information,
 * see the LICENSE file.
 */

package dev.lambdaurora.lambdynlights;

import dev.lambdaurora.lambdynlights.accessor.WorldRendererAccessor;
import dev.lambdaurora.lambdynlights.api.DynamicLightsInitializer;
import dev.lambdaurora.lambdynlights.engine.DynamicLightSource;
import dev.lambdaurora.lambdynlights.engine.DynamicLightSourceBehavior;
import dev.lambdaurora.lambdynlights.engine.DynamicLightingEngine;
import dev.lambdaurora.lambdynlights.resource.entity.EntityLightSources;
import dev.lambdaurora.lambdynlights.resource.item.ItemLightSources;
import dev.yumi.commons.event.EventManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.io.ResourceType;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockAndTintGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

/**
 * Represents the LambDynamicLights mod.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public class LambDynLights implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger("LambDynamicLights");
	public static final EventManager<Identifier> EVENT_MANAGER = new EventManager<>(LambDynLightsConstants.id("default"), Identifier::parse);
	private static LambDynLights INSTANCE;
	public final DynamicLightsConfig config = new DynamicLightsConfig(this);
	public final ItemLightSources itemLightSources = new ItemLightSources();
	public final EntityLightSources entityLightSources = new EntityLightSources(this.itemLightSources);
	public final DynamicLightingEngine engine = new DynamicLightingEngine();
	private final Set<DynamicLightSourceBehavior> dynamicLightSources = new HashSet<>();
	private final List<DynamicLightSourceBehavior> toClear = new ArrayList<>();
	private final ReentrantReadWriteLock lightSourcesLock = new ReentrantReadWriteLock();
	private long lastUpdate = System.currentTimeMillis();
	private int lastUpdateCount = 0;

	@Override
	public void onInitializeClient() {
		INSTANCE = this;
		log(LOGGER, "Initializing LambDynamicLights...");

		this.config.load();

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this.itemLightSources);
		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(this.entityLightSources);

		CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
			this.itemLightSources.apply(registries);
			this.entityLightSources.apply(registries);
		});

		ClientTickEvents.END_WORLD_TICK.register(level -> {
			this.lightSourcesLock.writeLock().lock();
			this.engine.computeSpatialLookup(this.dynamicLightSources);
			this.toClear.forEach(source -> source.lambdynlights$scheduleTrackedChunksRebuild(Minecraft.getInstance().levelRenderer));
			this.toClear.clear();
			this.lightSourcesLock.writeLock().unlock();
		});

		WorldRenderEvents.START.register(context -> {
			Profiler.get().swap("dynamic_lighting");
			this.updateAll(context.worldRenderer());
		});

		this.initializeApi();
	}

	/**
	 * Initializes the API.
	 */
	private void initializeApi() {
		this.invokeInitializers(DynamicLightsInitializer.ENTRYPOINT_KEY);
		// Legacy
		this.invokeInitializers("dynamiclights");

		// Sinytra
		// Under NeoForge there is no simple entrypoint system, so we end up just re-implementing Fabric-style entrypoints.
		// @TODO: This might need tweaking depending on how it gets implemented on Sinytra's side.
		FabricLoader.getInstance().getAllMods().stream()
				.filter(mod -> mod.getMetadata().containsCustomValue(DynamicLightsInitializer.ENTRYPOINT_KEY))
				.forEach(this::invokeInitializer);
	}

	/**
	 * Invokes {@linkplain DynamicLightsInitializer dynamic lights initializers} using Fabric's entrypoint system.
	 *
	 * @param entrypointKey the key of the entrypoints to invoke
	 */
	private void invokeInitializers(String entrypointKey) {
		FabricLoader.getInstance().getEntrypointContainers(entrypointKey, DynamicLightsInitializer.class)
				.stream()
				.map(EntrypointContainer::getEntrypoint)
				.forEach(this::invokeInitializer);
	}

	private void invokeInitializer(ModContainer mod) {
		String id = mod.getMetadata().getId();
		var entrypointValue = mod.getMetadata().getCustomValue(DynamicLightsInitializer.ENTRYPOINT_KEY);

		if (entrypointValue.getType() != CustomValue.CvType.STRING) {
			error(LOGGER, "Ignoring {} entrypoint from mod {}: not a string", DynamicLightsInitializer.ENTRYPOINT_KEY, id);
			return;
		}

		try {
			var initializer = LanguageAdapter.getDefault().create(mod, entrypointValue.getAsString(), DynamicLightsInitializer.class);
			this.invokeInitializer(initializer);
		} catch (LanguageAdapterException e) {
			error(LOGGER, "Failed to initializer {} entrypoint from mod {}: exception thrown", DynamicLightsInitializer.ENTRYPOINT_KEY, id, e);
			throw new RuntimeException(e);
		}
	}

	private void invokeInitializer(DynamicLightsInitializer initializer) {
		initializer.onInitializeDynamicLights(this.itemLightSources, this.entityLightSources);
	}

	/**
	 * Updates all light sources.
	 *
	 * @param renderer the renderer
	 */
	public void updateAll(@NotNull LevelRenderer renderer) {
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;

		long now = System.currentTimeMillis();
		if (now >= this.lastUpdate + 50) {
			this.lastUpdate = now;
			this.lastUpdateCount = 0;

			for (var lightSource : this.dynamicLightSources) {
				if (lightSource.lambdynlights$updateDynamicLight(renderer)) this.lastUpdateCount++;
			}
		}
	}

	/**
	 * Returns the last number of dynamic light source updates.
	 *
	 * @return the last number of dynamic light source updates
	 */
	public int getLastUpdateCount() {
		return this.lastUpdateCount;
	}

	/**
	 * Returns the lightmap with combined light levels.
	 *
	 * @param level the level in which the light is computed
	 * @param pos the position
	 * @param lightmap the vanilla lightmap coordinates
	 * @return the modified lightmap coordinates
	 */
	public int getLightmapWithDynamicLight(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, int lightmap) {
		if (!(level instanceof ClientLevel)) this.lightSourcesLock.readLock().lock();
		double light = this.getDynamicLightLevel(pos);
		if (!(level instanceof ClientLevel)) this.lightSourcesLock.readLock().unlock();
		return this.getLightmapWithDynamicLight(light, lightmap);
	}

	/**
	 * Returns the lightmap with combined light levels.
	 *
	 * @param dynamicLightLevel the dynamic light level
	 * @param lightmap the vanilla lightmap coordinates
	 * @return the modified lightmap coordinates
	 */
	public int getLightmapWithDynamicLight(double dynamicLightLevel, int lightmap) {
		if (dynamicLightLevel > 0) {
			// lightmap is (skyLevel << 20 | blockLevel << 4)

			// Get vanilla block light level.
			int blockLevel = LightTexture.block(lightmap);
			if (dynamicLightLevel > blockLevel) {
				// Equivalent to a << 4 bitshift with a little quirk: this one ensure more precision (more decimals are saved).
				int luminance = (int) (dynamicLightLevel * 16.0);
				lightmap &= 0xfff00000;
				lightmap |= luminance & 0x000fffff;
			}
		}

		return lightmap;
	}

	/**
	 * Returns the dynamic light level at the specified position.
	 *
	 * @param pos the position
	 * @return the dynamic light level at the specified position
	 */
	public double getDynamicLightLevel(@NotNull BlockPos pos) {
		return this.engine.getDynamicLightLevel(pos);
	}

	/**
	 * Adds the light source to the tracked light sources.
	 *
	 * @param lightSource the light source to add
	 */
	public void addLightSource(@NotNull DynamicLightSourceBehavior lightSource) {
		if (!lightSource.dynamicLightWorld().isClientSide())
			return;
		if (!this.config.getDynamicLightsMode().isEnabled())
			return;
		if (this.containsLightSource(lightSource))
			return;
		this.dynamicLightSources.add(lightSource);
	}

	/**
	 * Returns whether the light source is tracked or not.
	 *
	 * @param lightSource the light source to check
	 * @return {@code true} if the light source is tracked, else {@code false}
	 */
	public boolean containsLightSource(@NotNull DynamicLightSource lightSource) {
		if (!lightSource.dynamicLightWorld().isClientSide())
			return false;

		return this.dynamicLightSources.contains(lightSource);
	}

	/**
	 * Returns the number of dynamic light sources that currently emit lights.
	 *
	 * @return the number of dynamic light sources emitting light
	 */
	public int getLightSourcesCount() {
		return this.dynamicLightSources.size();
	}

	/**
	 * Removes the light source from the tracked light sources.
	 *
	 * @param lightSource the light source to remove
	 */
	public void removeLightSource(@NotNull DynamicLightSourceBehavior lightSource) {
		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSource it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (it.equals(lightSource)) {
				dynamicLightSources.remove();
				this.toClear.add(lightSource);
				break;
			}
		}
	}

	/**
	 * Clears light sources.
	 */
	public void clearLightSources() {
		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSourceBehavior it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			dynamicLightSources.remove();
			if (it.getLuminance() > 0)
				it.resetDynamicLight();
			this.toClear.add(it);
		}
	}

	/**
	 * Removes light sources if the filter matches.
	 *
	 * @param filter the removal filter
	 */
	public void removeLightSources(@NotNull Predicate<DynamicLightSourceBehavior> filter) {
		var dynamicLightSources = this.dynamicLightSources.iterator();
		DynamicLightSourceBehavior it;
		while (dynamicLightSources.hasNext()) {
			it = dynamicLightSources.next();
			if (filter.test(it)) {
				dynamicLightSources.remove();
				if (it.getLuminance() > 0)
					it.resetDynamicLight();
				this.toClear.add(it);
				break;
			}
		}
	}

	/**
	 * Removes entities light source from tracked light sources.
	 */
	public void removeEntitiesLightSource() {
		this.removeLightSources(lightSource -> (lightSource instanceof Entity && !(lightSource instanceof Player)));
	}

	/**
	 * Removes TNT light sources from tracked light sources.
	 */
	public void removeTntLightSources() {
		this.removeLightSources(entity -> entity instanceof PrimedTnt);
	}

	/**
	 * Logs an informational message.
	 *
	 * @param logger the logger to use
	 * @param msg the message to log
	 */
	public static void log(Logger logger, String msg) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			msg = "[LambDynLights] " + msg;
		}

		logger.info(msg);
	}

	/**
	 * Logs a warning message.
	 *
	 * @param logger the logger to use
	 * @param msg the message to log
	 */
	public static void warn(Logger logger, String msg) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			msg = "[LambDynLights] " + msg;
		}

		logger.warn(msg);
	}

	/**
	 * Logs a warning message.
	 *
	 * @param logger the logger to use
	 * @param msg the message to log
	 */
	public static void warn(Logger logger, String msg, Object... args) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			msg = "[LambDynLights] " + msg;
		}

		logger.warn(msg, args);
	}

	/**
	 * Logs an error message.
	 *
	 * @param logger the logger to use
	 * @param msg the message to log
	 */
	public static void error(Logger logger, String msg, Object... args) {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
			msg = "[LambDynLights] " + msg;
		}

		logger.error(msg, args);
	}

	/**
	 * Schedules a chunk rebuild at the specified chunk position.
	 *
	 * @param renderer the renderer
	 * @param chunkPos the chunk position
	 */
	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, @NotNull BlockPos chunkPos) {
		scheduleChunkRebuild(renderer, chunkPos.getX(), chunkPos.getY(), chunkPos.getZ());
	}

	/**
	 * Schedules a chunk rebuild at the specified chunk position.
	 *
	 * @param renderer the renderer
	 * @param chunkPos the packed chunk position
	 */
	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, long chunkPos) {
		scheduleChunkRebuild(renderer, BlockPos.unpackLongX(chunkPos), BlockPos.unpackLongY(chunkPos), BlockPos.unpackLongZ(chunkPos));
	}

	public static void scheduleChunkRebuild(@NotNull LevelRenderer renderer, int x, int y, int z) {
		if (Minecraft.getInstance().level != null)
			((WorldRendererAccessor) renderer).lambdynlights$scheduleChunkRebuild(x, y, z, false);
	}

	/**
	 * Updates the tracked chunk sets.
	 *
	 * @param chunkPos the packed chunk position
	 * @param old the set of old chunk coordinates to remove this chunk from it
	 * @param newPos the set of new chunk coordinates to add this chunk to it
	 */
	public static void updateTrackedChunks(@NotNull BlockPos chunkPos, @Nullable LongOpenHashSet old, @Nullable LongOpenHashSet newPos) {
		if (old != null || newPos != null) {
			long pos = chunkPos.asLong();
			if (old != null)
				old.remove(pos);
			if (newPos != null)
				newPos.add(pos);
		}
	}

	/**
	 * Updates the dynamic lights tracking.
	 *
	 * @param lightSource the light source
	 */
	public static void updateTracking(@NotNull DynamicLightSourceBehavior lightSource) {
		boolean enabled = lightSource.isDynamicLightEnabled();
		int luminance = lightSource.getLuminance();

		if (!enabled && luminance > 0) {
			lightSource.setDynamicLightEnabled(true);
		} else if (enabled && luminance < 1) {
			lightSource.setDynamicLightEnabled(false);
		}
	}

	private static boolean isEyeSubmergedInFluid(LivingEntity entity) {
		if (!LambDynLights.get().config.getWaterSensitiveCheck().get()) {
			return false;
		}

		var eyePos = BlockPos.ofFloored(entity.getX(), entity.getEyeY(), entity.getZ());
		return !entity.level().getFluidState(eyePos).isEmpty();
	}

	public static int getLivingEntityLuminanceFromItems(LivingEntity entity) {
		boolean submergedInFluid = isEyeSubmergedInFluid(entity);
		int luminance = 0;

		for (var equipped : entity.getAllSlots()) {
			if (!equipped.isEmpty())
				luminance = Math.max(luminance, INSTANCE.itemLightSources.getLuminance(equipped, submergedInFluid));
		}

		return luminance;
	}

	/**
	 * Returns the luminance from an entity.
	 *
	 * @param entity the entity
	 * @param <T> the type of the entity
	 * @return the luminance
	 */
	public static <T extends Entity> int getLuminanceFrom(T entity) {
		if (!INSTANCE.config.getEntitiesLightSource().get())
			return 0;
		if (entity == Minecraft.getInstance().player && !INSTANCE.config.getSelfLightSource().get())
			return 0;

		if (!DynamicLightingEngine.canLightUp(entity))
			return 0;

		return INSTANCE.entityLightSources.getLuminance(entity);
	}

	/**
	 * Returns the LambDynamicLights mod instance.
	 *
	 * @return the mod instance
	 */
	public static LambDynLights get() {
		return INSTANCE;
	}
}
