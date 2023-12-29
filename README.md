## LazuLib

Modding utils used in my mods, which I thought could be reused.
Feel free to use in your own mods or fork/copy what you need.

Licensed under the MIT License.

> [!NOTE]
> Since version 1.2.0 this library includes the events and mixins previously provided
> by [FlightCore](https://github.com/endorh/flight-core).

### Utils
Roughly accurate list of utils in this library. Categories correspond to packages in the source code.

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
- `MutableComponentList` List of `MutableComponent`s that can broadcast formatting operations to all its elements. Convenient for text split in lines.
- `TextUtil` Aliases for `TextComponent` and `TranslationComponent` constructors, split text components into formattable lists on newlines (including translated text (on client)), and link creation utils.
- `TooltipUtil` Key hints in tooltips.

#### Events
The following events are injected by this library's mixins.
For reference purposes, the mixins that fire these events can be found in the
[`endorh.lazulib.mixins`](https://github.com/endorh/lazulib/tree/1.20/src/main/java/endorh/lazulib/mixins) package.
- `CancelCapeRenderEvent` Hide a player's cape conditionally.
- `DisableElytraCheckEvent` Conditionally tweak or skip the elytra speed check in servers for players.
- `GenerateEndShipItemFrameEvent` Modify the item frame containing the elytra in end ships.
  The handler for this event in [Aerobatic Elytra](https://github.com/endorh/aerobatic-elytra) exposes
  this event to datapacks through a loot table.
- `PlayerTravelEvent` Modify the movement physics of the player.
- `RemotePlayerTravelEvent` Modify the local movement physics extrapolation for remote players.
- `PlayerTurnEvent` Modify the logic controlling the player's rotation.
- `RegisterTextureAtlasEvent` Inject custom texture atlas into the `ModelManager`'s `AtlasSet`.
- `SetupRotationsRenderPlayerEvent` Modify the rotations applied to the player's model.