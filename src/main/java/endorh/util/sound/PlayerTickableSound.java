package endorh.util.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public abstract class PlayerTickableSound extends AbstractTickableSoundInstance {
	private static final Logger LOGGER = LogManager.getLogger();
	public final Player player;
	
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
	  Player player, SoundEvent soundIn, SoundSource categoryIn
	) { this(player, soundIn, categoryIn, null); }
	
	public PlayerTickableSound(
	  Player player, SoundEvent soundIn, SoundSource categoryIn,
	  @Nullable IAttenuation attenuation
	) {
		super(soundIn, categoryIn, player.getRandom());
		this.player = player;
		looping = true;
		delay = 0;
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		
		// The built-in attenuation is useless, since it doesn't apply per tick
		super.attenuation = SoundInstance.Attenuation.NONE;
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
		final LocalPlayer player = Minecraft.getInstance().player;
		if (player == null)
			return vol;
		final Vec3 pos = player.position();
		final double x_d = pos.x - x;
		final double y_d = pos.y - y;
		final double z_d = pos.z - z;
		final float distance = Mth.sqrt((float) (x_d * x_d + y_d * y_d + z_d * z_d));
		return attenuationType.attenuate(vol, distance);
	}
	
	/**
	 * Can be overridden by subclasses
	 */
	public float getVolumeBeforeAttenuation() { return volume; }
	
	public static class PlayerTickableSubSound extends PlayerTickableSound {
		protected Player player;
		
		public PlayerTickableSubSound(
		  Player player, SoundEvent sound, SoundSource category
		) { this(player, sound, category, null); }
		
		public PlayerTickableSubSound(
		  Player player, SoundEvent sound, SoundSource category, IAttenuation attenuation
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
