package endorh.util.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Class of packets referring to a player, sent from the server
 * to clients.<br><br>
 * Subclasses should override {@link ServerPlayerPacket#onClient}
 * which is called when the packet reaches a client, with the
 * {@code ClientPlayerEntity} corresponding to the player the packet
 * belongs to as an argument.<br><br>
 * Packet registration should be performed by using a {@link PacketRegisterer}
 * provided by {@link ServerPlayerPacket#with}
 * @see DistributedPlayerPacket
 * @see ClientPlayerPacket
 * @see ServerWorldPacket
 */
public abstract class ServerPlayerPacket {
	protected UUID playerID = null;
	protected ServerPlayerEntity player = null;
	
	/**
	 * Internal constructor for deserialization
	 */
	protected ServerPlayerPacket() {}
	
	/**
	 * Base constructor
	 * @param player The player to which the event belongs
	 */
	public ServerPlayerPacket(PlayerEntity player) {
		this.player = (ServerPlayerEntity) player;
		playerID = player.getUniqueID();
	}
	
	/**
	 * Contains the {@link SimpleChannel} that was used to register each packet class
	 */
	public static final HashMap<Class<ServerPlayerPacket>, SimpleChannel> channelMap =
	  new HashMap<>();
	
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
	 * Create an instance using {@link ServerPlayerPacket#with}<br>
	 * Uses a single {@link SimpleChannel} and an int supplier
	 * (usually something like {@code () -> MySimpleChannel.ID_GEN++})
	 * to register several packets in order, by applying the method
	 * {@link PacketRegisterer#register}
	 * A typical usage example is provided:
	 * <pre>{@code
	 *    ServerPlayerPacket.with(
	 *      MySimpleChannel.INSTANCE,
	 *      () -> MySimpleChannel.ID_GEN++
	 *    ).register(SLoginModDataPacket::new)
	 *     .register(SCustomChatPacket::new);
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
		 * Shorthand for
		 * {@link ServerPlayerPacket#register(Supplier, SimpleChannel, int)}
		 * using the {@link PacketRegisterer}'s channel and id supplier.
		 */
		public <T extends ServerPlayerPacket>
		PacketRegisterer register(Supplier<T> sup) {
			ServerPlayerPacket.register(sup, channel, idSupplier.get());
			return this;
		}
	}
	
	/**
	 * Register a packet
	 * @param sup An instance supplier
	 * @param channel The SimpleChannel to use
	 * @param id The id to use
	 * @param <T> Packet class, implicit by sup
	 */
	public static <T extends ServerPlayerPacket>
	void register(Supplier<T> sup, SimpleChannel channel, int id) {
		//noinspection unchecked
		Class<T> cls = (Class<T>) sup.get().getClass();
		//noinspection unchecked
		channelMap.put((Class<ServerPlayerPacket>)cls, channel);
		channel.registerMessage(
		  id, cls,
		  (packet, buffer) -> {
		  	buffer.writeUniqueId(packet.playerID);
		  	packet.serialize(buffer);
		  },
		  (buffer) -> {
		  	T packet = sup.get();
		  	packet.playerID = buffer.readUniqueId();
		  	packet.deserialize(buffer);
		  	return packet;
		  },
		  (packet, ctxSupplier) -> {
			  final Context ctx = ctxSupplier.get();
			  ctx.enqueueWork(() -> {
				  final ClientWorld world = Minecraft.getInstance().world;
				  assert world != null;
				  packet.onClient(world.getPlayerByUuid(packet.playerID), ctx);
			  });
			  ctx.setPacketHandled(true);
		  },
		  Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}
	
	/**
	 * Called on the client thread when the packet reaches the client
	 * @param player The PlayerEntity to which this packet belongs
	 * @param ctx The context of the packet
	 */
	protected abstract void onClient(PlayerEntity player, Context ctx);
	
	/**
	 * Save all packet's fields in a {@link PacketBuffer}
	 * @param buf Serialization buffer
	 */
	protected abstract void serialize(PacketBuffer buf);
	
	/**
	 * Update this packet's fields from a {@link PacketBuffer}
	 * @param buf Deserialization buffer
	 */
	protected abstract void deserialize(PacketBuffer buf);
	
	/**
	 * Get the channel in which this packet was registered.
	 * @throws IllegalStateException if the packet has not been registered
	 * @see ServerPlayerPacket#sendTo(ServerPlayerEntity)
	 * @see ServerPlayerPacket#sendTracking()
	 * @see ServerPlayerPacket#sendTo(PacketTarget)
	 */
	public SimpleChannel getChannel() {
		SimpleChannel channel = channelMap.get(this.getClass());
		if (channel == null)
			throw new IllegalStateException("Attempted to send non-registered packet");
		return channel;
	}
	
	/**
	 * Sends the packet to a specific player.
	 * @param player Packet target.
    * @see ServerPlayerPacket#sendTracking()
	 * @see ServerPlayerPacket#sendTrackingAndSelf()
	 * @see ServerPlayerPacket#sendTo(PacketTarget)
	 */
	public void sendTo(ServerPlayerEntity player) {
		getChannel().sendTo(
		  this, player.connection.getNetworkManager(),
		  NetworkDirection.PLAY_TO_CLIENT);
	}
	
	/**
	 * Sends the packet to all players tracking the packet's player, excluding itself
	 * @see ServerPlayerPacket#sendTrackingAndSelf()
	 * @see ServerPlayerPacket#sendTo(ServerPlayerEntity)
	 * @see ServerPlayerPacket#sendTo(PacketTarget)
	 */
	public void sendTracking() {
		getChannel().send(
		  PacketDistributor.TRACKING_ENTITY.with(() -> player), this);
	}
	
	/**
	 * Sends the packet to all players tracking the packet's player, including itself
	 * @see ServerPlayerPacket#sendTracking()
	 * @see ServerPlayerPacket#sendTo(ServerPlayerEntity)
	 * @see ServerPlayerPacket#sendTo(PacketTarget)
	 */
	public void sendTrackingAndSelf() {
		getChannel().send(
		  PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), this);
	}
	
	/**
	 * Sends the packet to the provided {@link PacketTarget}
	 * @param target Target clients
	 * @see ServerPlayerPacket#sendTo(ServerPlayerEntity)
	 * @see ServerPlayerPacket#sendTracking()
	 * @see ServerPlayerPacket#sendTrackingAndSelf()
	 */
	public void sendTo(PacketTarget target) {
		getChannel().send(target, this);
	}
}
