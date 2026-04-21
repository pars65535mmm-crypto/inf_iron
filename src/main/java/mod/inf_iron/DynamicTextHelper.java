package mod.inf_iron;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.Util;

public class DynamicTextHelper {
    public static Component getGradientText(String text) {
        long time = Util.getMillis();
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            // Wave based on time and character position for black & white gradient
            double wave = Math.sin((time / 300.0) + (i * 0.4));
            // wave is -1.0 to 1.0, map to 100 - 255 to keep it visible (grey to white)
            int colorVal = (int) ((wave + 1.0) / 2.0 * 155.0) + 100; 
            int rgb = (colorVal << 16) | (colorVal << 8) | colorVal;
            
            component.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return component;
    }

    public static Component getBluishWhiteGradientText(String text) {
        long time = Util.getMillis();
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double wave = Math.sin((time / 300.0) + (i * 0.4));
            int base = (int) ((wave + 1.0) / 2.0 * 105.0) + 150; 
            int r = base - 30; 
            int g = base - 10; 
            int b = 255;
            if (r < 0) r = 0;
            if (g < 0) g = 0;
            int rgb = (r << 16) | (g << 8) | b;
            
            component.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return component;
    }

    public static Component getRainbowText(String text) {
        long time = Util.getMillis();
        MutableComponent component = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float hue = ((time % 2000L) / 2000.0F) + (i * 0.05F);
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.8F, 1.0F);
            component.append(Component.literal(String.valueOf(text.charAt(i)))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(rgb))));
        }
        return component;
    }
}
