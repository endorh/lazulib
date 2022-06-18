package endorh.util.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.PacketDistributor.PacketTarget;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Class of packets referring to a world, sent from the server
 * to clients.<br><br>
 * Subclasses should override {@link ServerWorldPacket#onClient}
 * which is called when the packet reaches a client, with the
 * {@code ClientWorld} corresponding to the world the packet
 * belongs to as an argument, if the client is currently in that world.<br><br>
 * Packet registration should be performed by using a {@link PacketRegisterer}
 * provided by {@link ServerWorldPacket#with}
 * @see DistributedPlayerPacket
 * @see ServerPlayerPacket
 * @see ClientPlayerPacket
 */
public abstract class ServerWorldPacket {
	protected ServerWorld world = null;
	protected ResourceLocation worldLocation = null;
	
	/**
	 * Internal constructor for deserialization
	 */
	protected ServerWorldPacket() {}
	
	/**
	 * Base constructor
	 * @param world World to which the packet belongs
	 */
	public ServerWorldPacket(World world) {
		this.world = (ServerWorld) world;
		worldLocation = world.dimension().location();
	}
	
	/**
	 * Contains the {@link SimpleChannel} that was used to register each packet
	 */
	public static final HashMap<Class<ServerWorldPacket>, SimpleChannel> channelMap =
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
	 * Create an instance using {@link ServerWorldPacket#with}<br>
	 * Uses a single {@link SimpleChannel} and an int supplier
	 * (usually something like {@code () -> MySimpleChannel.ID_GEN++})
	 * to register several packets in order, by applying the method
	 * {@link PacketRegisterer#register}
	 * A typical usage example is provided:
	 * <pre>{@code
	 *    ServerWorldPacket.with(
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
		 * {@link ServerWorldPacket#register(Supplier, SimpleChannel, int)}
		 * using the {@link PacketRegisterer}'s channel and id supplier.
		 */
		public <T extends ServerWorldPacket>
		PacketRegisterer register(Supplier<T> sup) {
			ServerWorldPacket.register(sup, channel, idSupplier.get());
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
	public static <T extends ServerWorldPacket>
	void register(Supplier<T> sup, SimpleChannel channel, int id) {
		//noinspection unchecked
		Class<T> cls = (Class<T>) sup.get().getClass();
		//noinspection unchecked
		channelMap.put((Class<ServerWorldPacket>)cls, channel);
		channel.registerMessage(
		  id, cls,
		  (packet, buffer) -> {
		  	buffer.writeResourceLocation(packet.worldLocation);
		  	packet.serialize(buffer);
		  },
		  (buffer) -> {
		  	T packet = sup.get();
		  	packet.worldLocation = buffer.readResourceLocation();
		  	packet.deserialize(buffer);
		  	return packet;
		  },
		  (packet, ctxSupplier) -> {
			  final Context ctx = ctxSupplier.get();
			  ctx.enqueueWork(() -> {
				  if (packet.world != null)
					  return;
				  final Minecraft mc = Minecraft.getInstance();
				  final ClientWorld world = mc.level;
				  assert world != null;
				  ResourceLocation worldLocation = world.dimension().location();
				  if (!worldLocation.equals(packet.worldLocation))
					  return;
				  packet.onClient(doCast(world), ctx);
			  });
			  ctx.setPacketHandled(true);
		  },
		  Optional.of(NetworkDirection.PLAY_TO_CLIENT)
		);
	}
	
	// Class loading is funny
	@OnlyIn(Dist.CLIENT) private static World doCast(ClientWorld world) { return world; }
	
	/**
	 * Called on the client thread when the packet reaches the client
	 * if the world in which the client is equals the packet's world
	 * @param world The world to which this packet belongs
	 * @param ctx The context of the packet
	 */
	public abstract void onClient(World world, Context ctx);
	
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
	 * @throws IllegalStateException if the pcaket has not been registered.
	 * @see ServerWorldPacket#sendTracking()
	 * @see ServerWorldPacket#sendTarget
	 */
	public SimpleChannel getChannel() {
		SimpleChannel channel = channelMap.get(this.getClass());
		if (channel == null)
			throw new IllegalStateException("Attempted to send non-registered packet");
		return channel;
	}
	
	/**
	 * Sends the packet to all players in the world the packet refers to
	 * @see ServerWorldPacket#sendTarget
	 */
	public void sendTracking() {
		getChannel().send(
		  PacketDistributor.DIMENSION.with(() -> world.dimension()), this
		);
	}
	
	/**
	 * Sends the packet to the given {@link PacketTarget}
	 * @see ServerWorldPacket#sendTracking()
	 */
	public void sendTarget(PacketTarget target) {
		getChannel().send(target, this);
	}
}
