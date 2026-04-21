package mod.inf_iron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class SpectralBladeRenderer extends EntityRenderer<SpectralBladeEntity> {
    public SpectralBladeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SpectralBladeEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        
        float time = entity.tickCount + partialTicks;

        // レンダリング用の滑らかな回転 (Lerp補間)
        float interpolatedYaw = Mth.lerp(partialTicks, entity.yRotO, entity.getYRot());
        float interpolatedPitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        // 浮遊感と回転
        poseStack.translate(0, 0.5, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-interpolatedYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(interpolatedPitch));
        
        // 攻撃中かつ物理移動があるなら回転（斬撃エフェクトっぽく） - 速度5倍化
        if (entity.getDeltaMovement().lengthSqr() > 0.01) {
             poseStack.mulPose(Axis.ZP.rotationDegrees(time * 100));
        }

        poseStack.scale(1.5f, 1.5f, 1.5f);

        // 神殺しの剣の外見を使用
        ItemStack sword = new ItemStack(ModItems.GOD_KILLER.get());
        
        Minecraft.getInstance().getItemRenderer().renderStatic(
            sword,
            ItemDisplayContext.FIXED,
            15728880,
            OverlayTexture.NO_OVERLAY,
            poseStack,
            buffer,
            entity.level(),
            entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpectralBladeEntity entity) {
        return null;
    }
}
