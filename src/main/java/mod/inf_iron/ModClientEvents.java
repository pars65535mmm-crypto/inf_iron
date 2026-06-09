package mod.inf_iron;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

@Mod.EventBusSubscriber(modid = InfIron.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientEvents {
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ZOLTRAAK.get(), ZoltraakRenderer::new);
        event.registerEntityRenderer(ModEntities.ATHEOS.get(), AtheosRenderer::new);
        event.registerEntityRenderer(ModEntities.ATHEOS_SUMMON.get(), AtheosSummonRenderer::new);
        event.registerEntityRenderer(ModEntities.SPECTRAL_BLADE.get(), SpectralBladeRenderer::new);
        event.registerEntityRenderer(ModEntities.FISSURE_MAGIC_CIRCLE.get(), FissureMagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.TORNADO.get(), TornadoRenderer::new);
        event.registerEntityRenderer(ModEntities.INFERNO_EXPLOSION.get(), InfernoExplosionRenderer::new);
        event.registerEntityRenderer(ModEntities.ULTIMA_MAGIC_CIRCLE.get(), UltimaMagicCircleRenderer::new);
        event.registerEntityRenderer(ModEntities.TRANSCENDENTAL_CORE_ENTITY.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CuriosRendererRegistry.register(ModItems.HALO_CURIOS.get(), HaloCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.FUNNEL_CURIOS.get(), FunnelCurioRenderer::new);
            CuriosRendererRegistry.register(ModItems.ORICHALCUM_HALO_CURIOS.get(), OrichalcumHaloCurioRenderer::new);
            
            // 変形武器のモデル切り替えプロパティ登録
            net.minecraft.client.renderer.item.ItemProperties.register(ModItems.FERRUM_OF_EXECUTION.get(), 
                new ResourceLocation(InfIron.MODID, "mode"), 
                (stack, level, entity, seed) -> FerrumOfExecutionItem.getMode(stack).id);
        });
    }
}
