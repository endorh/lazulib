package endorh.util.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public abstract class PlayerTickableSound extends TickableSound {
	private static final Logger LOGGER = LogManager.getLogger();
	public final PlayerEntity player;
	
	/**
	 * Attenuation for this sound effect<br>
	 * By default applies an exponential attenuation with reach of 4 chunks
	 */
	protected IAttenuation attenuationType = IAttenuation.exponential(64F);
	
	/**
	 * The sound engine doesn't apply attenuation to tickable sounds
	 * on each tick, so we do our own.
	 */
	public interface IAttenuation {
		float attenuate(float volume, float distance);
		IAttenuation NONE = (volume, distance) -> volume;
		static IAttenuation linear(float radius) {
			return (volume, distance) -> distance > radius? 0F : volume * (1F - distance / radius);
		}
		static IAttenuation exponential(float radius) {
			return (volume, distance) ->
			  distance > radius? 0F : volume * AudioUtil.fadeOutExp(distance / radius);
		}
	}
	
	public PlayerTickableSound(
	  PlayerEntity player, SoundEvent soundIn, SoundCategory categoryIn
	) { this(player, soundIn, categoryIn, null); }
	
	public PlayerTickableSound(
	  PlayerEntity player, SoundEvent soundIn, SoundCategory categoryIn,
	  @Nullable IAttenuation attenuation
	) {
		super(soundIn, categoryIn);
		this.player = player;
		looping = true;
		delay = 0;
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		
		// The built-in attenuation is useless, since it doesn't apply per tick
		super.attenuation = ISound.AttenuationType.NONE;
		if (attenuation != null)
			attenuationType = attenuation;
	}
	
	@Override public boolean canPlaySound() { return !player.isSilent(); }
	@Override public boolean canStartSilent() { return true; }
	
	/**
	 * Updates the player position<br>
	 * Subclasses should call super
	 */
	@Override public void tick() {
		x = player.getX();
		y = player.getY();
		z = player.getZ();
	}
	
	/**
	 * Get the volume attenuated by the current distance
	 */
	@Override public final float getVolume() {
		final float vol = getVolumeBeforeAttenuation();
		if (attenuationType == IAttenuation.NONE) // Specially handled
			return vol;
		final ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null)
			return vol;
		final Vector3d pos = player.position();
		final double x_d = pos.x - x;
		final double y_d = pos.y - y;
		final double z_d = pos.z - z;
		final float distance = MathHelper.sqrt(x_d * x_d + y_d * y_d + z_d * z_d);
		return attenuationType.attenuate(vol, distance);
	}
	
	/**
	 * Can be overridden by subclasses
	 */
	public float getVolumeBeforeAttenuation() { return volume; }
	
	public static class PlayerTickableSubSound extends PlayerTickableSound {
		protected PlayerEntity player;
		
		public PlayerTickableSubSound(
		  PlayerEntity player, SoundEvent sound, SoundCategory category
		) { this(player, sound, category, null); }
		
		public PlayerTickableSubSound(
		  PlayerEntity player, SoundEvent sound, SoundCategory category, IAttenuation attenuation
		) {
			super(player, sound, category, attenuation);
			this.player = player;
			volume = 0F;
			pitch = 1F;
		}
		
		public void play() {
			Minecraft.getInstance().getSoundManager().play(this);
		}
		
		public void setVolume(float volume) {
			this.volume = volume;
		}
		
		public void setPitch(float pitch) {
			this.pitch = pitch;
		}
		
		public void finish() {
			stop();
		}
	}
}
