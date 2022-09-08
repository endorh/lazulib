package endorh.util.recipe;

import endorh.util.LazuLib;
import endorh.util.common.ObfuscationReflectionUtil;
import endorh.util.common.ObfuscationReflectionUtil.SoftField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;

@EventBusSubscriber(value = Dist.CLIENT, modid = LazuLib.MOD_ID)
public class RecipeManagerHelper {
	protected static final Set<CachedRecipeProvider<?>> PROVIDERS =
	  Collections.newSetFromMap(new WeakHashMap<>());
	protected static final CachedRecipeProvider<Collection<IRecipe<?>>> recipeListProvider =
	  new CachedRecipeProvider<Collection<IRecipe<?>>>() {
		  @Override protected Collection<IRecipe<?>> onReload(RecipeManager manager) {
			  return manager.getRecipes();
		  }
	  };
	
	protected static final SoftField<RecipeManager, Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>>>
	  RecipeManager$recipes = ObfuscationReflectionUtil.getSoftField(
	    RecipeManager.class, "field_199522_d", "recipes");
	
	// Recipe caching
	protected static WeakReference<Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>>>
	  lastRecipes = new WeakReference<>(null);
	protected static WeakReference<RecipeManager> lastRecipeManager = new WeakReference<>(null);
	
	public static @NotNull RecipeManager getRecipeManager() {
		RecipeManager manager = DistExecutor.unsafeRunForDist(
		  () -> () -> {
			  ClientWorld world = Minecraft.getInstance().world;
			  return world != null? world.getRecipeManager() : null;
		  }, () -> () -> {
			  MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
			  return server.getRecipeManager();
		  }
		);
		if (manager == null) throw new IllegalStateException("Could not get recipe manager");
		if (manager != lastRecipeManager.get())
			lastRecipeManager = new WeakReference<>(manager);
		return manager;
	}
	
	protected static boolean checkCache() {
		final Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> map = RecipeManager$recipes
		  .get(getRecipeManager());
		if (map != lastRecipes.get()) {
			lastRecipes = new WeakReference<>(map);
			PROVIDERS.forEach(CachedRecipeProvider::invalidate);
			return false;
		}
		return true;
	}
	
	public static Collection<IRecipe<?>> getRecipes() {
		return recipeListProvider.get();
	}
	
	public static <T> CachedRecipeProvider<List<T>> recipeProviderForType(Class<T> type) {
		return new CachedRecipeProvider<List<T>>() {
			@Override protected List<T> onReload(RecipeManager manager) {
				//noinspection unchecked
				return manager.getRecipes().stream()
				  .filter(type::isInstance).map(r -> (T) r)
				  .collect(Collectors.toList());
			}
		};
	}
	
	@SubscribeEvent protected static void onRecipesUpdated(RecipesUpdatedEvent event) {
		PROVIDERS.forEach(CachedRecipeProvider::invalidate);
	}
	
	protected static void registerCachedRecipeProvider(CachedRecipeProvider<?> provider) {
		PROVIDERS.add(provider);
	}
	
	/**
	 * Used to cache recipes, safely reloading them when out of date<br>
	 * Subclasses should only care about implementing
	 * {@link CachedRecipeProvider#onReload(RecipeManager)}<br>
	 * To get the data use {@link CachedRecipeProvider#get()}
	 */
	public static abstract class CachedRecipeProvider<T> {
		protected T cachedData;
		protected @Internal boolean invalidated;
		
		protected CachedRecipeProvider() {
			registerCachedRecipeProvider(this);
		}
		
		protected @Internal final void invalidate() {
			invalidated = true;
			onInvalidate();
		}
		
		public final boolean isInvalidated() {
			if (!checkCache())
				return true;
			return invalidated;
		}
		
		/**
		 * Called when invalidated, in case a subclass prefers to reload
		 * as soon as possible, though not encouraged.
		 */
		protected void onInvalidate() {}
		protected abstract T onReload(RecipeManager manager);
		
		public final void reload() {
			invalidated = false;
			cachedData = onReload(getRecipeManager());
		}
		
		public final T get() {
			if (isInvalidated()) reload();
			return cachedData;
		}
	}
}
