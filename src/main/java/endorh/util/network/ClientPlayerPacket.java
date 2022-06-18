package endorh.util.network;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Class of packets referring to a player, sent from clients to
 * the server<br><br>
 * Subclasses should override {@link ClientPlayerPacket#onServer}, which
 * is called when the packet reaches the server, with the
 * {@code ServerPlayerEntity} corresponding to the player the packet belongs
 * to as an argument.<br><br>
 * Packet registration should be performed by using a {@link PacketRegisterer}
 * provided by {@link ClientPlayerPacket#with}
 * @see DistributedPlayerPacket
 * @see ServerPlayerPacket
 * @see ServerWorldPacket
 */
public abstract class ClientPlayerPacket {
	/**
	 * Internal player UUID used to refer to the player across sides
	 */
	protected UUID playerID;
	protected ClientPlayerEntity player;
	
	/**
	 * Internal constructor for deserialization
	 */
	protected ClientPlayerPacket() {}
	
	/**
	 * Base constructor
	 * @param player The player to which the event belongs
	 */
	public ClientPlayerPacket(PlayerEntity player) {
		this.player = (ClientPlayerEntity) player;
		playerID = player.getUniqueID();
	}
	
	/**
	 * Contains the {@link SimpleChannel} that was used to register each packet
	 */
	public static final HashMap<Class<ClientPlayerPacket>, SimpleChannel> channelMap =
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
	 * Create an instance using {@link ClientPlayerPacket#with}<br>
	 * Uses a single {@link SimpleChannel} and an int supplier
	 * (usually something like {@code () -> MySimpleChannel.ID_GEN++})
	 * to register several packets in order, by applying the method
	 * {@link PacketRegisterer#register}
	 * A typical usage example is provided:
	 * <pre>{@code
	 *    ClientPlayerPacket.with(
	 *      MySimpleChannel.INSTANCE,
	 *      () -> MySimpleChannel.ID_GEN++
	 *    ).register(CCustomRecipeUsePacket::new)
	 *     .register(CSummonChickenSpellPacket::new);
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
		 * {@link ClientPlayerPacket#register(Supplier, SimpleChannel, int)}
		 * using the {@link PacketRegisterer}'s channel and id supplier.
		 */
		public <T extends ClientPlayerPacket>
		PacketRegisterer register(Supplier<T> sup) {
			ClientPlayerPacket.register(sup, channel, idSupplier.get());
			return this;
		}
	}
	
	/**
	 * Register a packet
	 * @param sup An instance supplier
	 * @param channel The {@link SimpleChannel} to use
	 * @param id The id to use
	 * @param <T> Packet class, implicit by sup
	 */
	public static <T extends ClientPlayerPacket>
	void register(Supplier<T> sup, SimpleChannel channel, int id) {
		//noinspection unchecked
		Class<T> cls = (Class<T>) sup.get().getClass();
		//noinspection unchecked
		channelMap.put((Class<ClientPlayerPacket>)cls, channel);
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
			   ServerPlayerEntity sender = ctx.getSender();
			   packet.onServer(sender, ctx);
		   });
		  	ctx.setPacketHandled(true);
		  },
		  Optional.of(NetworkDirection.PLAY_TO_SERVER)
		);
	}
	
	/**
	 * Called on the client thread when the packet reaches the client
	 * @param player The PlayerEntity to which this packet belongs
	 * @param ctx The context of the packet
	 */
	public abstract void onServer(PlayerEntity player, Context ctx);
	
	/**
	 * Save all packet's fields in a {@link PacketBuffer}
	 * @param buf Serialization buffer
	 */
	public abstract void serialize(PacketBuffer buf);
	
	/**
	 * Update this packet's fields from a {@link PacketBuffer}
	 * @param buf Deserialization buffer
	 */
	public abstract void deserialize(PacketBuffer buf);
	
	/**
	 * Get the channel in which this packet was registered.
	 * @throws IllegalStateException if the packet has not been registered
	 * @see ClientPlayerPacket#send()
	 */
	public SimpleChannel getChannel() {
		SimpleChannel channel = channelMap.get(this.getClass());
		if (channel == null)
			throw new IllegalStateException("Attempted to send non-registered packet");
		return channel;
	}
	
	/**
	 * Sends the packet to the server
	 */
	public void send() {
		getChannel().sendToServer(this);
	}
}
