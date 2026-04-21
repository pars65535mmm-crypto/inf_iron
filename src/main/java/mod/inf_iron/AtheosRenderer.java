package mod.inf_iron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class AtheosRenderer extends EntityRenderer<AtheosEntity> {
    public AtheosRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AtheosEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        float time = entity.tickCount + partialTicks;
        int actionState = entity.getActionState();

        // 全体の回転と浮遊感
        poseStack.translate(0, 2.0 + Math.sin(time * 0.05) * 0.5, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 0.5f));
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 0.03) * 10f));

        // コア（ネザースター）の描画
        poseStack.pushPose();
        float coreScale = 2.0f + (float) Math.sin(time * 0.2) * 0.5f;
        poseStack.scale(coreScale, coreScale, coreScale);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 20.0f));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.NETHER_STAR),
                ItemDisplayContext.FIXED,
                15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                0);
        poseStack.popPose();

        // 外殻（鉄と金の混合）の描画
        float expand;
        boolean isCharging = entity.tickCount % 20 < 10 && actionState != 0;
        
        if (actionState == 10) {
            // 神ノ怒：最大展開して激しく振動
            expand = 6.0f + (float) Math.sin(time * 3.0) * 0.5f;
        } else if (actionState == 6 || actionState == 5) {
            expand = 4.0f + (float) Math.sin(time * 1.5) * 1.0f;
        } else {
            expand = (actionState > 0) ? 3.0f + (float) Math.sin(time * 1.0) * 0.5f : 1.5f;
        }

        float blockRotation;
        if (actionState == 10) blockRotation = time * 20.0f;
        else if (actionState > 0) blockRotation = time * 8.0f;
        else blockRotation = time;
        
        float scaleMultiplier = (actionState == 10) ? 1.5f : (isCharging ? 1.2f : 1.0f);

        drawBlock(poseStack, buffer, expand * scaleMultiplier, 0, 0, blockRotation, Blocks.IRON_BLOCK);
        drawBlock(poseStack, buffer, -expand * scaleMultiplier, 0, 0, blockRotation, Blocks.IRON_BLOCK);
        drawBlock(poseStack, buffer, 0, expand * scaleMultiplier, 0, blockRotation, Blocks.GOLD_BLOCK);
        drawBlock(poseStack, buffer, 0, -expand * scaleMultiplier, 0, blockRotation, Blocks.GOLD_BLOCK);
        drawBlock(poseStack, buffer, 0, 0, expand * scaleMultiplier, blockRotation, Blocks.IRON_BLOCK);
        drawBlock(poseStack, buffer, 0, 0, -expand * scaleMultiplier, blockRotation, Blocks.IRON_BLOCK);

        // 中間のエッジ部分
        if (actionState != 1 && (actionState < 5 || actionState > 10)) { 
            float edgeDist = expand * 0.7f;
            drawBlock(poseStack, buffer, edgeDist, edgeDist, 0, time * 0.7f, Blocks.GOLD_BLOCK);
            drawBlock(poseStack, buffer, -edgeDist, edgeDist, 0, time * 0.7f, Blocks.IRON_BLOCK);
            drawBlock(poseStack, buffer, edgeDist, -edgeDist, 0, time * 0.7f, Blocks.IRON_BLOCK);
            drawBlock(poseStack, buffer, -edgeDist, -edgeDist, 0, time * 0.7f, Blocks.GOLD_BLOCK);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void drawBlock(PoseStack poseStack, MultiBufferSource buffer, float x, float y, float z,
            float rotation, net.minecraft.world.level.block.Block block) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 2));
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation * 3));
        poseStack.scale(1.2f, 1.2f, 1.2f);

        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                block.defaultBlockState(),
                poseStack,
                buffer,
                15728880,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(AtheosEntity entity) {
        return null;
    }
}
