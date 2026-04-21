package mod.inf_iron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class HaloCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = slotContext.entity();
        EntityModel<T> model = renderLayerParent.getModel();
        
        poseStack.pushPose();

        ICurioRenderer.translateIfSneaking(poseStack, entity);
        ICurioRenderer.rotateIfSneaking(poseStack, entity);
        
        if (model instanceof HumanoidModel<?> humanoidModel) {
            ICurioRenderer.followHeadRotations(entity, humanoidModel.head);
            // さっきの画像で高すぎたので、下げる（頭のすぐ上に）
            poseStack.translate(0.0, -0.65, 0.0);
        } else {
            poseStack.translate(0.0, -0.5, 0.0);
        }

        // 光源を最大にして光らせる
        int fullLight = 15728880;
        float time = ageInTicks + partialTicks;

        // 鼓動するようなスケール変更
        float pulse = 1.0f + 0.05f * (float)Math.sin(time * 0.1f);
        poseStack.scale(pulse, pulse, pulse);

        // 1つ目のリング（外側、エンドロッド12個）時計回り
        renderRing(poseStack, renderTypeBuffer, entity, time, 0.75f, 1, 2.0f, 15.0f, fullLight, Items.END_ROD);
        // 2つ目のリング（内側、エンドロッド12個）反時計回り
        renderRing(poseStack, renderTypeBuffer, entity, time, 0.55f, -1, 4.0f, -10.0f, fullLight, Items.END_ROD);
        // 3つ目のリング（最外周、アメジストの欠片）ゆっくり
        renderRing(poseStack, renderTypeBuffer, entity, time, 1.0f, 1, 1.0f, 5.0f, fullLight, Items.AMETHYST_SHARD);

        // 中央のコア（ネザースター）
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 5.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees((float)Math.sin(time * 0.05f) * 20.0f));
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.NETHER_STAR), ItemDisplayContext.FIXED, fullLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
        poseStack.popPose();

        // 浮遊する紋章（エンダーアイ）
        for(int i=0; i<4; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 3.0f + i * 90f));
            poseStack.translate(0.4, -0.1, 0);
            poseStack.scale(0.3f, 0.3f, 0.3f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.ENDER_EYE), ItemDisplayContext.FIXED, fullLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
            poseStack.popPose();
        }

        poseStack.popPose();
    }

    private void renderRing(PoseStack poseStack, MultiBufferSource renderTypeBuffer, LivingEntity entity, float time, float radius, int direction, float rotationSpeed, float tiltAngle, int light, net.minecraft.world.item.Item item) {
        poseStack.pushPose();
        
        // 立体感を強めるための傾き
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltAngle * (float)Math.sin(time * 0.05f)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltAngle * (float)Math.cos(time * 0.05f)));
        
        // リング全体の回転
        poseStack.mulPose(Axis.YP.rotationDegrees(time * rotationSpeed * direction));

        int pieces = 12; // 12角形
        for (int i = 0; i < pieces; i++) {
            poseStack.pushPose();
            float angle = (float) i * (360.0f / pieces);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(0.0, 0.0, radius);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f)); // 横向きに寝かせる
            
            // リング状につなげる
            poseStack.scale(0.3f, 0.7f, 0.3f);

            Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(item),
                ItemDisplayContext.FIXED,
                light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                renderTypeBuffer,
                entity.level(),
                0
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
