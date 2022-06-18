package endorh.util.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import endorh.util.EndorUtil;
import endorh.util.nbt.JsonToNBTUtil;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.Ingredient.IItemList;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static net.minecraft.util.JSONUtils.getJsonArray;
import static net.minecraft.util.JSONUtils.getJsonObject;

/**
 * Extended shaped recipe, which can define NBT in the output and
 * NBT sources from the ingredients, which get merged into the output.
 * <br><br>
 * The ingredient declaration is identical to that of shaped recipes,
 * except that uppercase letters in the pattern mean NBT sources,
 * while lowercase don't.
 * <br><br>
 * Additionally, the result object may specify a 'tag' key containing
 * NBT that will be added to the output. The NBT should be in the
 * usual JSON format (used by commands, adds a letter after values
 * to declare their type when not obvious, see {@link JsonToNBTUtil})
 * <br><br>
 * NBT in the result tag will override NBT from ingredients.
 * If multiple NBT source ingredients are declared, their NBT will
 * be merged. The topmost-leftmost source has priority.
 * <br>
 * The anvil penalty is handled specially, and the max from all
 * sources is taken, unless the result tag specifies it, in which
 * case, it has priority.
 * <br><br>
 * Subclasses may want to override the
 * {@link NBTInheritingShapedRecipe#getResultTag}
 * to modify the NBT merging logic.
 * <br><br>
 * TODO: Add link to a JSON example when uploaded
 */
public class NBTInheritingShapedRecipe extends ShapedRecipe {
	public static final Serializer SERIALIZER = new Serializer();
	
	public final NonNullList<int[]> nbtSources;
	public final CompoundNBT outputTag;
	protected final int recipeWidth;
	protected final int recipeHeight;
	protected final NonNullList<Ingredient> recipeItems;
	
	public NBTInheritingShapedRecipe(
	  ResourceLocation id, String group, int width, int height,
	  NonNullList<int[]> nbtSourcesIn, NonNullList<Ingredient> items,
	  ItemStack output, CompoundNBT outputTagIn
	) {
		super(id, group, width, height, items, output);
		recipeWidth = width;
		recipeHeight = height;
		recipeItems = items;
		nbtSources = nbtSourcesIn;
		outputTag = outputTagIn;
	}
	
	@NotNull @Override
	public ItemStack getCraftingResult(@NotNull CraftingInventory inv) {
		ItemStack result = super.getCraftingResult(inv).copy();
		result.setTag(getResultTag(inv));
		return result;
	}
	
	public Pair<Integer, Integer> findRecipeOffset(@NotNull CraftingInventory inv) {
		int i = 0, j = 0;
		findNBTSource:
		for (; i <= inv.getWidth() - getRecipeWidth(); i++) {
			for (; j <= inv.getHeight() - this.getRecipeHeight(); j++) {
				if (checkMatch(inv, i, j, true)) break findNBTSource;
				if (checkMatch(inv, i, j, false)) break findNBTSource;
			}
		}
		return Pair.of(i, j);
	}
	
	public CompoundNBT getResultTag(@NotNull CraftingInventory inv) {
		final Pair<Integer, Integer> offset = findRecipeOffset(inv);
		final int i = offset.getFirst(), j = offset.getSecond();
		
		CompoundNBT resultTag = outputTag.copy();
		final String TAG_REPAIR_COST = "RepairCost";
		int repairCost = 0;
		for (int[] nbtSourcePos : nbtSources) {
			ItemStack nbtSource = inv.getStackInSlot(
			  inv.getWidth() * (nbtSourcePos[1] + j) + nbtSourcePos[0] + i).copy();
			// The result tag has priority
			CompoundNBT sourceTag = nbtSource.getTag();
			if (sourceTag != null) {
				repairCost = Math.max(repairCost, sourceTag.getInt(TAG_REPAIR_COST));
				resultTag = sourceTag.merge(resultTag);
			}
		}
		if (outputTag.contains(TAG_REPAIR_COST, 3))
			repairCost = outputTag.getInt(TAG_REPAIR_COST);
		if (repairCost != 0)
			resultTag.putInt(TAG_REPAIR_COST, repairCost);
		return resultTag;
	}
	
	protected boolean checkMatch(
	  CraftingInventory inv, int x, int y, boolean xMirror
	) {
		for(int i = 0; i < inv.getWidth(); i++) {
			for(int j = 0; j < inv.getHeight(); j++) {
				int ix = i - x;
				int iy = j - y;
				
				Ingredient ing =
				  (ix >= 0 && ix < recipeWidth && iy >= 0 && iy < recipeHeight)
				  ? (xMirror
				     ? recipeItems.get(recipeWidth - ix - 1 + iy * recipeWidth)
				     : recipeItems.get(ix + iy * recipeWidth))
				  : Ingredient.EMPTY;
				
				if (!ing.test(inv.getStackInSlot(i + j * inv.getWidth())))
					return false;
			}
		}
		return true;
	}
	
	@NotNull @Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}
	
	public static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>>
	  implements IRecipeSerializer<NBTInheritingShapedRecipe> {
		
		public static final ResourceLocation NAME = new ResourceLocation(
		  EndorUtil.MOD_ID, "nbt_inheriting_shaped_recipe");
		
		public static int MAX_WIDTH = 3;
		public static int MAX_HEIGHT = 3;
		
		Serializer() {
			setRegistryName(NAME);
		}
		
		@NotNull @Override public
		NBTInheritingShapedRecipe read(
		  @NotNull ResourceLocation recipeId, @NotNull JsonObject json
		) {
			String group = JSONUtils.getString(json, "group", "");
			boolean allowUnknown = JSONUtils.getBoolean(json, "allow_unknown_items", false);
			Map<String, Ingredient> map = deserializeKey(getJsonObject(json, "key"), allowUnknown);
			String[] pat = shrink(patternFromJson(getJsonArray(json, "pattern")));
			int w = pat[0].length();
			int h = pat.length;
			NonNullList<int[]> nbtSources = nbtSourcesFromPattern(pat);
			NonNullList<Ingredient> list = deserializeIngredients(pat, map, w, h);
			ItemStack output = ShapedRecipe.deserializeItem(getJsonObject(json, "result"));
			CompoundNBT outputTag = nbtFromJson(json);
			
			return new NBTInheritingShapedRecipe(
			  recipeId, group, w, h, nbtSources, list, output, outputTag);
		}
		
		@Nullable @Override public
		NBTInheritingShapedRecipe read(
		  @NotNull ResourceLocation id, @NotNull PacketBuffer buf
		) {
			int w = buf.readVarInt();
			int h = buf.readVarInt();
			String group = buf.readString(32767);
			NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
			
			for (int i = 0; i < list.size(); i++)
				list.set(i, Ingredient.read(buf));
			
			ItemStack output = buf.readItemStack();
			
			int l = buf.readVarInt();
			NonNullList<int[]> nbtSources = NonNullList.withSize(l, new int[] {0, 0});
			for (int i = 0; i < l; i++)
				nbtSources.set(i, buf.readVarIntArray());
			
			CompoundNBT outputTag = buf.readCompoundTag();
			
			return new NBTInheritingShapedRecipe(id, group, w, h, nbtSources, list, output, outputTag);
		}
		
		@Override public void write(
		  @NotNull PacketBuffer buf, @NotNull NBTInheritingShapedRecipe recipe
		) {
			buf.writeVarInt(recipe.recipeWidth);
			buf.writeVarInt(recipe.recipeHeight);
			buf.writeString(recipe.getGroup());
			
			for (Ingredient ing : recipe.recipeItems)
				ing.write(buf);
			
			buf.writeItemStack(recipe.getRecipeOutput());
			
			buf.writeVarInt(recipe.nbtSources.size());
			for (int[] nbtSource : recipe.nbtSources)
				buf.writeVarIntArray(nbtSource);
			
			buf.writeCompoundTag(recipe.outputTag);
		}
		
		// Pattern parsing logic
		
		public static String[] patternFromJson(JsonArray arr) {
			String[] pattern = new String[arr.size()];
			if (pattern.length > MAX_HEIGHT)
				throw new JsonSyntaxException("Invalid pattern: too many rows, max is " + MAX_HEIGHT);
			if (pattern.length == 0)
				throw new JsonSyntaxException("Invalid pattern: pattern can't be empty");
			for(int i = 0; i < pattern.length; i++) {
				String row = JSONUtils.getString(arr.get(i), "pattern[" + i + "]");
				if (row.length() > MAX_WIDTH)
					throw new JsonSyntaxException(
					  "Invalid pattern: too many columns, max is" + MAX_WIDTH);
				if (i > 0 && pattern[0].length() != row.length())
					throw new JsonSyntaxException(
					  "Invalid pattern: all rows must have the same width");
				pattern[i] = row;
			}
			return pattern;
		}
		
		/**
		 * Returns a key json object as a map
		 */
		public static Map<String, Ingredient> deserializeKey(JsonObject json, boolean allowUnknown) {
			Map<String, Ingredient> map = new HashMap<>();
			for(Entry<String, JsonElement> entry : json.entrySet()) {
				if (entry.getKey().length() != 1)
					throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
				if (" ".equals(entry.getKey()))
					throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
				if (entry.getValue().isJsonArray() && allowUnknown) {
					map.put(entry.getKey(), filterKnownFromList(entry.getValue().getAsJsonArray()));
				} else map.put(entry.getKey(), Ingredient.deserialize(entry.getValue()));
			}
			map.put(" ", Ingredient.EMPTY);
			return map;
		}
		
		/**
		 * Similar to {@link Ingredient#deserialize(JsonElement)}, but
		 * skips unknown items in the array<br>
		 * If at the end there are no remaining items, an exception is nonetheless thrown.
		 * @param array Json array containing items
		 * @return {@link Ingredient}
		 */
		public static Ingredient filterKnownFromList(JsonArray array) {
			if (array.size() == 0)
				throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
			final List<IItemList> list = StreamSupport.stream(
			  array.spliterator(), false).map(
			  el -> {
				  try {
					  return Ingredient.deserializeItemList(getJsonObject(el, "item"));
				  } catch (JsonSyntaxException e) {
					  if (e.getMessage().startsWith("Unknown item"))
						  return null;
					  else throw e;
				  }
			  }
			).filter(Objects::nonNull).collect(Collectors.toList());
			if (list.size() == 0)
				throw new JsonSyntaxException("All items from array were unknown, at least one defined item must be known to load the recipe");
			return Ingredient.fromItemListStream(list.stream());
		}
		
		public static NonNullList<Ingredient> deserializeIngredients(
		  String[] pattern, Map<String, Ingredient> keys, int w, int h
		) {
			NonNullList<Ingredient> list = NonNullList.withSize(w * h, Ingredient.EMPTY);
			Set<String> set = new HashSet<>(keys.keySet());
			set.remove(" ");
			
			for(int i = 0; i < pattern.length; i++) {
				for(int j = 0; j < pattern[i].length(); j++) {
					String s = pattern[i].substring(j, j + 1);
					Ingredient ing = keys.get(s);
					if (ing == null)
						throw new JsonSyntaxException("Pattern references symbol '" + s + "' but it's not defined in the key");
					set.remove(s);
					list.set(j + w * i, ing);
				}
			}
			
			if (!set.isEmpty())
				throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set);
			return list;
		}
		
		/**
		 * Tries to shrink a pattern as much as possible from its String[] form
		 */
		public static String[] shrink(String... pattern) {
			int left = Integer.MAX_VALUE;
			int right = 0;
			int top = 0;
			int bottom = 0;
			
			for(int i = 0; i < pattern.length; i++) {
				String row = pattern[i];
				left = Math.min(left, firstDifferent(row, ' '));
				int rowRight = lastDifferent(row, ' ');
				right = Math.max(right, rowRight);
				if (rowRight < 0) { // Empty row
					if (top == i)
						top++;
					bottom++;
				} else
					bottom = 0;
			}
			
			if (pattern.length == bottom) {
				return new String[0];
			}
			String[] shrunk = new String[pattern.length - bottom - top];
			for(int i = 0; i < shrunk.length; i++)
				shrunk[i] = pattern[i + top].substring(left, right + 1);
			return shrunk;
		}
		public static int firstDifferent(String str, char chr) {
			int i = 0;
			while (i < str.length() && chr == str.charAt(i))
				i++;
			return i;
		}
		public static int lastDifferent(String str, char chr) {
			int i = str.length() - 1;
			while (i >= 0 && chr == str.charAt(i))
				i--;
			return i;
		}
		
		public static NonNullList<int[]> nbtSourcesFromPattern(String[] pattern) {
			NonNullList<int[]> list = NonNullList.create();
			for (int i = 0; i < pattern.length; i++) {
				for (int j = 0; j < pattern[i].length(); j++) {
					if (Character.isUpperCase(pattern[i].charAt(j))) {
						list.add(new int[] {j, i});
					}
				}
			}
			return list;
		}
		
		public static CompoundNBT nbtFromJson(JsonObject root) {
			CompoundNBT outTag = new CompoundNBT();
			JsonElement res = root.get("result");
			if (res != null && res.isJsonObject()) {
				JsonElement tag = res.getAsJsonObject().get("tag");
				if (tag != null && tag.isJsonObject()) {
					outTag = JsonToNBTUtil.getTagFromJson(tag.getAsJsonObject());
				}
			}
			return outTag;
		}
	}
}
