package endorh.util.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent.Context;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import net.minecraftforge.fmllegacy.network.PacketDistributor.PacketTarget;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Class of packets containing information from a player, which when
 * sent to the server, automatically relay themselves to clients based
 * on a packet target provider specified at registration (usually other
 * players tracking the player).
 * <br>
 * <br>
 * The packets can additionally be checked on the server and potentially invalidated,
 * cancelling relay to the clients and, optionally, instead returning a
 * corrected packet to the sender.<br>
 * The packet target provider is a {@code Function<Supplier<PlayerEntity>, PacketTarget>}
 * which is run on the server (so the player is actually always a {@code ServerPlayerEntity}),
 * specified at registration.<br>
 * The most commonly used are
 * {@code p -> }{@link PacketDistributor#TRACKING_ENTITY}{@code .with(p::get)} and
 * {@code ignored -> }{@link PacketDistributor#ALL}{@code .noArg()}, which
 * can be used easily if registering the packets through a {@link PacketRegisterer}
 * <br>
 * <br>
 * Subclasses should override either {@link DistributedPlayerPacket#onCommon} or
 * both {@link DistributedPlayerPacket#onServer} and
 * {@link DistributedPlayerPacket#onClient}
 * Optionally, they may override {@link DistributedPlayerPacket#onServerCancellable}
 * instead of {@code onServer}, which allows them to cancel a packet's propagation
 * to clients and, optionally, send a corrected packet back.
 * <br>
 * When the packet reaches the server, its {@code onServerCancellable}
 * method will be called. By default this does not cancel and calls
 * {@code onServer}.<br>
 * If not cancelled, after this call, the packet is relayed to clients and
 * {@code onClient} is called on their client threads.<br>
 * By default, both {@code onClient} and {@code onServer} call
 * {@code onCommon}, which by default does nothing.
 *
 * @see ValidatedDistributedPlayerPacket
 * @see ServerPlayerPacket
 * @see ClientPlayerPacket
 * @see ServerWorldPacket
 */
public abstract class DistributedPlayerPacket {
	protected UUID playerID;
	protected ServerPlayer sender = null;
	protected boolean onServer = false;
	protected boolean bounced = false;
	
	public DistributedPlayerPacket() {}
	
	/**
	 * Contains the {@link SimpleChannel} that was used to register each packet class
	 */
	public static final HashMap<Class<DistributedPlayerPacket>, SimpleChannel> channelMap = new HashMap<>();
	private static final UUID nil = new UUID(0L, 0L);
	
	/**
	 * Obtain a {@link PacketRegisterer} with given parameters.
	 * @param channel {@link SimpleChannel} to use to register packets.
	 * @param idSupplier An int supplier to provide with ids for registered packets.
	 */
	public static PacketRegisterer with(SimpleChannel channel, Supplier<Integer> idSupplier) {
		return new PacketRegisterer(channel, idSupplier);
	}
	
	/**
	 * Simplifies the packet registration process.<br>
	 * Create an instance using {@link DistributedPlayerPacket#with}<br>
	 * Uses a single {@link SimpleChannel} and an int supplier
	 * (usually something like {@code () -> MySimpleChannel.ID_GEN++})
	 * to register several packets in order, by applying the methods
	 * {@link PacketRegisterer#registerLocal}, {@link PacketRegisterer#registerGlobal}
	 * or {@link PacketRegisterer#register}
	 * A typical usage example is provided:
	 * <pre>{@code
	 *    DistributedPlayerPacket.with(
	 *      MySimpleChannel.INSTANCE,
	 *      () -> MySimpleChannel.ID_GEN++
	 *    ).registerLocal(DAccelerationPacket::new)
	 *     .registerGlobal(DKillStrikeRecord::new);
	 * }</pre>
	 */
	public static class PacketRegisterer {
		public final SimpleChannel channel;
		public final Supplier<Integer> idSupplier;
		
		private PacketRegisterer(SimpleChannel channel, Supplier<Integer> idSupplier) {
			this.channel = channel;
			this.idSupplier = idSupplier;
		}
		
		/**
		 * Shorthand for {@link PacketRegisterer#register(Supplier, Function)}
		 * using the distributor {@link PacketDistributor#TRACKING_ENTITY}<br>
		 * Useful for distributed packets that carry information of the player
		 * relevant for players around it, such as positioning/rendering.<br>
		 * @see PacketRegisterer#register
		 * @see PacketRegisterer#registerGlobal
		 */
		public <T extends DistributedPlayerPacket>
		PacketRegisterer registerLocal(Supplier<T> sup) {
			return register(sup, playerSupplier -> PacketDistributor.TRACKING_ENTITY.with(playerSupplier::get));
		}
		
		/**
		 * Shorthand for {@link DistributedPlayerPacket.PacketRegisterer#register(Supplier, Function)}
		 * using the distributor {@link PacketDistributor#ALL}<br>
		 * Useful for distributed packets that carry information of the player
		 * relevant for all players, such as scoreboard info.<br>
		 * @see PacketRegisterer#register
		 * @see PacketRegisterer#registerLocal
		 */
		public <T extends DistributedPlayerPacket>
		PacketRegisterer registerGlobal(Supplier<T> sup) {
			return register(sup, ignored -> PacketDistributor.ALL.noArg());
		}
		
		/**
		 * Shorthand for
		 * {@link DistributedPlayerPacket#register(Supplier, Function, SimpleChannel, int)}
		 * using the {@link PacketRegisterer}'s channel and id supplier.
		 * @see PacketRegisterer#registerLocal
		 * @see PacketRegisterer#registerGlobal
		 */
		public <T extends DistributedPlayerPacket>
		PacketRegisterer register(Supplier<T> sup, Function<Supplier<Player>, PacketTarget> distributor) {
			DistributedPlayerPacket.register(sup, distributor, channel, idSupplier.get());
			return this;
		}
	}
	
	/**
	 * Register a packet.
	 * @param sup A supplier of your packet instances (empty), used for
	 *            deserialization, typically {@code YourPacketClass::new}
	 * @param distributor A packet distributor, see {@code registerTracking}
	 *                    and {@code registerAll}
	 * @param channel {@link SimpleChannel} to register the packet in.
	 * @param id The packet id to be used in registration.
	 * @param <T> The class of your packet, implicit by sup
	 *
	 * @see PacketRegisterer#registerLocal
	 * @see PacketRegisterer#registerGlobal
	 * @see PacketRegisterer#register(Supplier, Function)
	 */
	public static <T extends DistributedPlayerPacket>
	void register(Supplier<T> sup, Function<Supplier<Player>, PacketTarget> distributor,
	              final SimpleChannel channel, int id) {
		//noinspection unchecked
		Class<T> cls = (Class<T>) sup.get().getClass();
		//noinspection unchecked
		channelMap.put((Class<DistributedPlayerPacket>)cls, channel);
		channel.registerMessage(
		  id, cls,
		  // Serializer
		  (packet, buffer) -> {
		  	buffer.writeUUID(
		  	  packet.playerID != null
		     ? packet.playerID
		  	  : nil);
		  	packet.serialize(buffer);
		  },
		  // Deserializer
		  (buffer) -> {
			  T packet = sup.get();
			  packet.playerID = buffer.readUUID();
			  if (nil.equals(packet.playerID))
			  	packet.playerID = null;
			  packet.deserialize(buffer);
			  return packet;
		  },
		  // Handler
		  (packet, ctxSupplier) -> {
			  final Context ctx = ctxSupplier.get();
			  ctx.enqueueWork(() -> {
				  // Server handler
				  if (packet.playerID == null) {
					  packet.onServer = true;
					  ServerPlayer sender = ctx.getSender();
					  assert sender != null;
					  packet.sender = sender;
					  packet.playerID = sender.getUUID();
					  if (packet.onServerCancellable(sender, ctx)) {
						  channel.send(distributor.apply(() -> sender), packet);
					  }
					  // Client handler
				  } else {
					  final Minecraft mc = Minecraft.getInstance();
					  final ClientLevel world = mc.level;
					  assert world != null;
					  Player sender = world.getPlayerByUUID(packet.playerID);
					  if (sender == mc.player) {
						  packet.bounced = true;
						  packet.onBounce(sender, ctx);
					  } else {
						  packet.onClient(sender, ctx);
					  }
				  }
			  });
			  ctx.setPacketHandled(true);
		  }
		);
	}
	
	/**
	 * Called on the server thread when the packet reaches the server<br>
	 * By default, calls {@link DistributedPlayerPacket#onServer}
	 * and returns true
	 *
	 * @param sender The sender of the packet
	 * @param ctx The packet context.
	 * @return True if the packet should be relayed to clients tracking the sender
	 */
	protected boolean onServerCancellable(Player sender, Context ctx) {
		onServer(sender, ctx);
		return true;
	}
	
	/**
	 * Called on the server thread when the packet reaches the server<br>
	 * It may alter the packet before it's sent back to clients<br>
	 * If you'd like to be able to cancel the automatic packet propagation
	 * to clients, override {@link DistributedPlayerPacket#onServerCancellable} instead
	 *  @param sender The sender of the packet
	 * @param ctx The packet context
	 */
	protected void onServer(Player sender, Context ctx) {
		onCommon(sender, ctx);
	}
	
	/**
	 * Called on the client thread when the packet is relayed from the server
	 *  @param sender The {@link Player} of the original sender of the message
	 *               in this client
	 * @param ctx    The context of the packet
	 */
	protected void onClient(Player sender, Context ctx) {
		onCommon(sender, ctx);
	}
	
	/**
	 * Called by the default implementations of {@link DistributedPlayerPacket#onServer}
	 * and {@link DistributedPlayerPacket#onClient}
	 * <br>
	 * Useful to override to avoid repeating code
	 *  @param sender The entity corresponding to the sender of the packet
	 * @param ctx The packet context
	 */
	protected void onCommon(Player sender, Context ctx) {}
	
	/**
	 * Called on the client when the packet is rebounded from the server,
	 * usually as a result of an invalid value in the packet.<br>
	 * By default calls {@link DistributedPlayerPacket#onClient}
	 * @param self The own client player entity
	 * @param ctx The packet context
	 */
	protected void onBounce(Player self, Context ctx) {
		onClient(self ,ctx);
	}
	
	/**
	 * Save all packet's fields in a {@link FriendlyByteBuf}
	 * @param buf The FriendlyByteBuf
	 */
	protected abstract void serialize(FriendlyByteBuf buf);
	
	/**
	 * Update this packet's fields from a {@link FriendlyByteBuf}
	 * @param buf The FriendlyByteBuf
	 */
	protected abstract void deserialize(FriendlyByteBuf buf);
	
	/**
	 * Sends this packet to the server, where it might be relayed to clients
	 */
	public void send() {
		getChannel().sendToServer(this);
	}
	
	/**
	 * Get the channel in which this packet was registered
	 * @throws IllegalStateException - If the packet has not been registered
	 */
	public SimpleChannel getChannel() throws IllegalStateException {
		SimpleChannel channel = channelMap.get(this.getClass());
		if (channel == null)
			throw new IllegalStateException("Attempted to use a non-registered packet`s channel");
		return channel;
	}
	
	/**
	 * Sends back the packet to the original sender<br>
	 * Mainly used to respond to invalid packets with correcting information
	 */
	protected void sendBack() {
		getChannel().sendTo(
		  this, sender.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
	}
}
