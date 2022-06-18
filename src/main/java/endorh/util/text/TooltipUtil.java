package endorh.util.text;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static endorh.util.text.TextUtil.ttc;

@OnlyIn(Dist.CLIENT)
public class TooltipUtil {
	public static IFormattableTextComponent ctrlToExpand() {
		return ctrlToExpand(TextFormatting.DARK_GRAY, TextFormatting.GRAY);
	}
	
	public static IFormattableTextComponent ctrlToExpand(
	  TextFormatting color, TextFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.control").mergeStyle(
		  Screen.hasControlDown()? accent : color
		)).mergeStyle(color);
	}
	
	public static IFormattableTextComponent altToExpand() {
		return altToExpand(TextFormatting.DARK_GRAY, TextFormatting.GRAY);
	}
	
	public static IFormattableTextComponent altToExpand(
	  TextFormatting color, TextFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.alt").mergeStyle(
		  Screen.hasAltDown()? accent : color
		)).mergeStyle(color);
	}
	
	public static IFormattableTextComponent shiftToExpand() {
		return shiftToExpand(TextFormatting.DARK_GRAY, TextFormatting.GRAY);
	}
	
	public static IFormattableTextComponent shiftToExpand(
	  TextFormatting color, TextFormatting accent
	) {
		return ttc("key.display.wrap", ttc("key.keyboard.shift").mergeStyle(
		  Screen.hasShiftDown()? accent : color
		)).mergeStyle(color);
	}
}
