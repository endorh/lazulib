## Endor Util

Modding utils used in my mods, that could be useful in other mods.
Feel free to use in your own mods or fork what you need.

Licensed under the MIT License.

Since these are tools I make for my own use, I can't promise they will remain
backwards compatibility across Minecraft versions.

### Utils
Roughly accurate list of implemented utils. Categories correspond to packages in the source code.

#### Commands
- `QualifiedNameArgumentType` Argument type accepting `qualified:resource_names`

#### Common
- `ColorUtil` Measure color distances, find closest dye/formatting color, mix and interpolate colors, and other miscellaneous color utils.
- `ForgeUtil`
  - `futureNotNull` Placeholder for Object Holder initializers
  - `getSerializedCaps` Extract capabilities NBT from ItemStack
- `LogUtil` Log once utils.
- `ObfuscationReflectionUtil` Failure tolerant reflection utils.

#### Math
- `Interpolator` Very basic interpolation functions for animations.
- `MathParser` Very inefficient math parser with support for custom variables and functions.
- `MathHighlighter` Add syntax highlighting to the expressions parsed by MathParser.
- `Vec3d` and `Vec3f` Custom vector implementations with many rotation utils.

#### NBT
- `JsonToNBTUtil` Self documented name.
- `NBTPath` Navigate NBT using dot-separated paths. Can create intermediate nodes, and works similarly to IO paths.
- `NBTPredicate` Serializable NBT predicates, such as `{Fireworks.Flight >= 3}`. Supports lists and complex predicates, though it's not perfect.
- `NBTCompareHelper` Utils to compare NBT on specific paths.

#### Network
- `PacketBufferUtil` read/write nullables, lists and maps.
- `ClientPlayerPacket` and `ServerPlayerPacket` Base class for packets carrying a player reference.
- `DistributedPlayerPacket` and `ValidatedDistributedPlayerPacket` Special client player packet, that is automatically relayed to other players (usually those tracking the player). The server can prevent this behaviour ofc.
- `ServerWorldPacket` Base class for packets carrying a world reference to the clients.

#### Recipe
- `NBTInheritingShapedRecipe` Base recipe class that can specify (using upper case letters) items in the grid which are used as NBT sources for the crafting result.
- `RecipeManagerHelper` Utils to get registered recipes.

#### Sound
- `AudioUtil` Fading and cross-fading utils.
- `PlayerTickableSound` Base class for `TickableSound`s associated to a player.

#### Text
- `FormattableTextComponentList` List of `FormattableTextComponent`s that can broadcast formatting operations to all its elements. Convenient for text split in lines.
- `TextUtil` Aliases for `StringTextComponent` and `TranslationTextComponent` constructors, split text components into formattable lists on newlines, even translated text, and link creation utils.
- `TooltipUtil` Key hints in tooltips.