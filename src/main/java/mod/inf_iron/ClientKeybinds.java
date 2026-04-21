package mod.inf_iron;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = InfIron.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientKeybinds {
    public static final KeyMapping KEY_TIME_STOP = new KeyMapping("key.the_inf_iron.time_stop", GLFW.GLFW_KEY_Z, "category.the_inf_iron.halo");
    public static final KeyMapping KEY_TELEPORT = new KeyMapping("key.the_inf_iron.teleport", GLFW.GLFW_KEY_X, "category.the_inf_iron.halo");
    public static final KeyMapping KEY_DASH = new KeyMapping("key.the_inf_iron.dash", GLFW.GLFW_KEY_C, "category.the_inf_iron.halo");
    public static final KeyMapping KEY_GRAVITY = new KeyMapping("key.the_inf_iron.gravity", GLFW.GLFW_KEY_V, "category.the_inf_iron.halo");

    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(KEY_TIME_STOP);
        event.register(KEY_TELEPORT);
        event.register(KEY_DASH);
        event.register(KEY_GRAVITY);
    }
}
