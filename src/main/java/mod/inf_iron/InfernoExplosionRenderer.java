package mod.inf_iron;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class InfernoExplosionRenderer extends EntityRenderer<InfernoExplosionEntity> {

    public InfernoExplosionRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(InfernoExplosionEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, poseStack, pBuffer, pPackedLight);
        
        int tick = pEntity.tickCount;
        int castTicks = 150;
        
        // 詠唱期間のみ巨大な邪悪な魔法陣を描画
        if (tick < castTicks) {
            float progress = (tick + pPartialTicks) / (float)castTicks;
            
            poseStack.pushPose();
            // 上空に展開
            poseStack.translate(0, 20.0, 0);

            // 急激に巨大化する魔法陣
            float scale = 1.0f + progress * 60.0f; 
            poseStack.scale(scale, scale, scale);

            // クルクル回る
            float angleY = (tick + pPartialTicks) * 20.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
            
            VertexConsumer vertexConsumer = pBuffer.getBuffer(RenderType.lightning());
            
            // ベースの水平魔法陣
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            Matrix4f matrix1 = poseStack.last().pose();
            
            // 外側の円 (濃い赤)
            drawPolygon(matrix1, vertexConsumer, 36, 1.0f, 255, 0, 0, 255); 
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(-angleY * 2.5f));
            // 逆回転するオレンジの六芒星
            drawStar(poseStack.last().pose(), vertexConsumer, 1.0f, 255, 100, 0, 255);
            poseStack.popPose();
            // 内側の黒っぽい円
            drawPolygon(matrix1, vertexConsumer, 18, 0.5f, 50, 0, 0, 255);
            poseStack.popPose();

            // ドーム型を予感させる斜めの陣
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(angleY * -1.5f));
            Matrix4f matrix2 = poseStack.last().pose();
            drawPolygon(matrix2, vertexConsumer, 24, 1.2f, 200, 50, 0, 150); 
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(angleY * 2.0f));
            Matrix4f matrix3 = poseStack.last().pose();
            drawPolygon(matrix3, vertexConsumer, 12, 0.8f, 255, 200, 50, 100); 
            poseStack.popPose();

            poseStack.popPose();
        }
    }

    // 正多角形（円）の描画
    private void drawPolygon(Matrix4f matrix, VertexConsumer consumer, int segments, float radius, int r, int g, int b, int a) {
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);

            float x1 = (float) Math.cos(angle1) * radius;
            float y1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float y2 = (float) Math.sin(angle2) * radius;

            float thickness = 0.05f;
            consumer.vertex(matrix, x1, y1 - thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 - thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 + thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x1, y1 + thickness, 0).color(r, g, b, a).endVertex();
        }
    }

    // 六芒星の描画
    private void drawStar(Matrix4f matrix, VertexConsumer consumer, float radius, int r, int g, int b, int a) {
        float t = 0.05f;
        for(int i = 0; i < 3; i++) {
            float a1 = (float) (i * 2 * Math.PI / 3 - Math.PI / 2);
            float a2 = (float) ((i + 1) * 2 * Math.PI / 3 - Math.PI / 2);
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;
            consumer.vertex(matrix, x1, y1 - t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 - t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 + t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x1, y1 + t, 0).color(r, g, b, a).endVertex();
        }
        for(int i = 0; i < 3; i++) {
            float a1 = (float) (i * 2 * Math.PI / 3 + Math.PI / 2);
            float a2 = (float) ((i + 1) * 2 * Math.PI / 3 + Math.PI / 2);
            float x1 = (float) Math.cos(a1) * radius;
            float y1 = (float) Math.sin(a1) * radius;
            float x2 = (float) Math.cos(a2) * radius;
            float y2 = (float) Math.sin(a2) * radius;
            consumer.vertex(matrix, x1, y1 - t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 - t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 + t, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x1, y1 + t, 0).color(r, g, b, a).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(InfernoExplosionEntity pEntity) {
        return new ResourceLocation("minecraft", "textures/block/magma.png"); // ダミー
    }
}
