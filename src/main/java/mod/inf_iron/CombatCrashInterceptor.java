package mod.inf_iron;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "inf_iron", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CombatCrashInterceptor {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurtAbsoluteInterceptor(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            
            // モノリスの即死計算が始まる直前に、リフレクション経由で安全に戦闘ログを完全消去！
            try {
                java.lang.reflect.Method recheckStatusMethod = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                    net.minecraft.world.damagesource.CombatTracker.class, 
                    "m_19270_"
                );
                recheckStatusMethod.setAccessible(true);
                recheckStatusMethod.invoke(player.getCombatTracker());
            } catch (Exception e) {
                event.setCanceled(true);
            }
            
            // ダメージを完全無効化
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }
}