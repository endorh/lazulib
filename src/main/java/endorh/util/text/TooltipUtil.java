package endorh.util.text;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static endorh.util.text.TextUtil.ttc;

@OnlyIn(Dist.CLIENT)
public class TooltipUtil {
	public static MutableComponent ctrlToExpand() {
		return ctrlToExpand(ChatFormatting.DARK_GRAY, ChatFormatting.GRAY);
	}
	
	public static MutableComponent ctrlToExpand(
	  ChatFormatting color, ChatFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.control").withStyle(
		  Screen.hasControlDown()? accent : color
		)).withStyle(color);
	}
	
	public static MutableComponent altToExpand() {
		return altToExpand(ChatFormatting.DARK_GRAY, ChatFormatting.GRAY);
	}
	
	public static MutableComponent altToExpand(
	  ChatFormatting color, ChatFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.alt").withStyle(
		  Screen.hasAltDown()? accent : color
		)).withStyle(color);
	}
	
	public static MutableComponent shiftToExpand() {
		return shiftToExpand(ChatFormatting.DARK_GRAY, ChatFormatting.GRAY);
	}
	
	public static MutableComponent shiftToExpand(
	  ChatFormatting color, ChatFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.shift").withStyle(
		  Screen.hasShiftDown()? accent : color
		)).withStyle(color);
	}
}
