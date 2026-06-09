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

public class OrichalcumHaloCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        LivingEntity entity = slotContext.entity();
        EntityModel<T> model = renderLayerParent.getModel();
        
        poseStack.pushPose();

        ICurioRenderer.translateIfSneaking(poseStack, entity);
        ICurioRenderer.rotateIfSneaking(poseStack, entity);
        
        if (model instanceof HumanoidModel<?> humanoidModel) {
            ICurioRenderer.followHeadRotations(entity, humanoidModel.head);
            poseStack.translate(0.0, -0.65, 0.0);
        } else {
            poseStack.translate(0.0, -0.5, 0.0);
        }

        renderHalo(poseStack, renderTypeBuffer, entity, ageInTicks + partialTicks);
        
        poseStack.popPose();
    }

    public static void renderHalo(PoseStack poseStack, MultiBufferSource renderTypeBuffer, LivingEntity entity, float time) {
        int fullLight = 15728880;

        float pulse = 1.0f + 0.1f * (float)Math.sin(time * 0.15f);
        poseStack.scale(pulse, pulse, pulse);

        // -------------------------
        // 1. 太陽・星型の後光 (背後に配置)
        // -------------------------
        poseStack.pushPose();
        // 体の後ろ、頭より上に配置
        poseStack.translate(0, -0.5, 0.8);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 2.0f)); // ゆっくり回転

        // ギザギザの光輪を作る
        int rays = 16;
        for (int i = 0; i < rays; i++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(i * (360.0f / rays)));
            poseStack.translate(0, 0.5, 0); // 中心から離す
            
            // 長さを交互に変える
            float lengthScale = (i % 2 == 0) ? 1.5f : 0.8f;
            poseStack.scale(0.3f, lengthScale, 0.3f);
            
            Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.GLOWSTONE), 
                ItemDisplayContext.FIXED, 
                fullLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 
                poseStack, 
                renderTypeBuffer, 
                entity.level(), 
                0
            );
            
            // 先端をさらに尖らせる
            poseStack.translate(0, 0.8, 0);
            poseStack.scale(0.5f, 0.5f, 0.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.END_ROD), 
                ItemDisplayContext.FIXED, 
                fullLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 
                poseStack, 
                renderTypeBuffer, 
                entity.level(), 
                0
            );
            
            poseStack.popPose();
        }
        
        // 中心にコア
        poseStack.pushPose();
        poseStack.scale(0.8f, 0.8f, 0.8f);
        Minecraft.getInstance().getItemRenderer().renderStatic(
            new ItemStack(ModItems.OMNIVERSAL_CORE.get()), 
            ItemDisplayContext.FIXED, fullLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
        poseStack.popPose();
        
        poseStack.popPose();

        // -------------------------
        // 2. 周囲を公転する惑星群
        // -------------------------
        net.minecraft.world.level.block.Block[] planets = {
            net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK, // 地球っぽい
            net.minecraft.world.level.block.Blocks.RED_SANDSTONE, // 火星っぽい
            net.minecraft.world.level.block.Blocks.GOLD_BLOCK,    // 金星っぽい
            net.minecraft.world.level.block.Blocks.SLIME_BLOCK,   // 緑の星
            net.minecraft.world.level.block.Blocks.LAPIS_BLOCK,   // 青い星
            net.minecraft.world.level.block.Blocks.MAGMA_BLOCK,   // 灼熱の星
            net.minecraft.world.level.block.Blocks.END_STONE,     // 荒涼とした星
            net.minecraft.world.level.block.Blocks.PURPUR_BLOCK   // 紫の星
        };

        for (int i = 0; i < planets.length; i++) {
            poseStack.pushPose();
            
            float distance = 1.8f + (i * 0.4f); // 軌道の半径
            float speed = 3.0f - (i * 0.2f);    // 公転速度
            float angle = time * speed + (i * 45.0f); // 現在の角度
            
            // 軌道の傾きを付ける
            poseStack.mulPose(Axis.XP.rotationDegrees(15.0f * (float)Math.sin(i)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(15.0f * (float)Math.cos(i)));
            
            // 公転
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(0, 0, distance);
            
            // 自転
            poseStack.mulPose(Axis.XP.rotationDegrees(time * 5f));
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 7f));
            
            poseStack.scale(0.35f, 0.35f, 0.35f); // 惑星のサイズ
            
            Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(planets[i]),
                ItemDisplayContext.FIXED,
                fullLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                renderTypeBuffer,
                entity.level(),
                0
            );
            
            // 土星の輪（一部の惑星のみ）
            if (i == 2 || i == 6) {
                poseStack.pushPose();
                poseStack.scale(2.5f, 0.1f, 2.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                    new ItemStack(Items.LIGHT_WEIGHTED_PRESSURE_PLATE),
                    ItemDisplayContext.FIXED, fullLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, renderTypeBuffer, entity.level(), 0);
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }


    private void renderRing(PoseStack poseStack, MultiBufferSource renderTypeBuffer, LivingEntity entity, float time, float radius, int direction, float rotationSpeed, float tiltAngle, int light, net.minecraft.world.item.Item item) {
        poseStack.pushPose();
        
        poseStack.mulPose(Axis.XP.rotationDegrees(tiltAngle * (float)Math.sin(time * 0.05f)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(tiltAngle * (float)Math.cos(time * 0.05f)));
        
        poseStack.mulPose(Axis.YP.rotationDegrees(time * rotationSpeed * direction));

        int pieces = 12;
        for (int i = 0; i < pieces; i++) {
            poseStack.pushPose();
            float angle = (float) i * (360.0f / pieces);
            poseStack.mulPose(Axis.YP.rotationDegrees(angle));
            poseStack.translate(0.0, 0.0, radius);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            
            poseStack.scale(0.4f, 0.8f, 0.4f);

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
