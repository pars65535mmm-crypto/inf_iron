package mod.inf_iron;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = InfIron.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // consumeClick() は1回押下されたときにtrueを返し、内部フラグを下げる
        while (ClientKeybinds.KEY_TIME_STOP.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new AbilityPacket(0));
        }
        while (ClientKeybinds.KEY_TELEPORT.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new AbilityPacket(1));
        }
        while (ClientKeybinds.KEY_DASH.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new AbilityPacket(2));
        }
        while (ClientKeybinds.KEY_GRAVITY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new AbilityPacket(3));
        }
    }
}
