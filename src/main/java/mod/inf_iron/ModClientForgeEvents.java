package mod.inf_iron;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.EquipmentSlot;

@Mod.EventBusSubscriber(modid = InfIron.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModClientForgeEvents {
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player != null && player.isShiftKeyDown()) {
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof OrichalcumPaxelItem) {
                event.setCanceled(true); // Prevent hotbar switching
                double delta = event.getScrollDelta();
                if (delta != 0) {
                    ModNetwork.CHANNEL.sendToServer(new PaxelRadiusPacket(delta > 0 ? 1 : -1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && ClientKeybinds.KEY_EXP_TOOL_MENU.consumeClick()) {
            ItemStack stack = mc.player.getMainHandItem();
            if (stack.getItem() instanceof OrichalcumExpToolItem) {
                mc.setScreen(new ExpToolRadialScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (event.getEntity() instanceof Player player) {
            if (isWearingFullOrichalcumArmor(player)) {
                boolean hasHalo = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.is(ModItems.HALO_CURIOS.get())).isPresent();
                if (!hasHalo) {
                    com.mojang.blaze3d.vertex.PoseStack poseStack = event.getPoseStack();
                    poseStack.pushPose();
                    
                    // 防具のレンダーとして適切な位置に調整 (頭の上と背後)
                    // キャラクターの回転に合わせる
                    float bodyYaw = net.minecraft.util.Mth.lerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
                    poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(bodyYaw));
                    poseStack.translate(0.0, 1.3, 0.0);

                    OrichalcumHaloCurioRenderer.renderHalo(poseStack, event.getMultiBufferSource(), player, player.tickCount + event.getPartialTick());

                    poseStack.popPose();
                }
            }
        }
    }

    private static boolean isWearingFullOrichalcumArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.ORICHALCUM_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.ORICHALCUM_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.ORICHALCUM_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.ORICHALCUM_BOOTS.get();
    }
}
