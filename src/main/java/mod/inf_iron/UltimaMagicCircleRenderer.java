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

public class UltimaMagicCircleRenderer extends EntityRenderer<UltimaMagicCircleEntity> {

    public UltimaMagicCircleRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(UltimaMagicCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        int currentAge = entity.getEntityData().get(UltimaMagicCircleEntity.AGE);
        // ※AGEの取得を安全に。
        // currentAge = entity.tickCount; // entityDataのAGEが同期遅延する場合を考慮しtickCountベースでも良いが、同期用AGEを使用

        poseStack.pushPose();

        // 基本サイズ
        float baseScale = 5.0f;
        // 終盤に巨大化
        if (currentAge > 160) {
            baseScale += (currentAge - 160 + partialTicks) * 2.0f;
        }
        poseStack.scale(baseScale, 1.0f, baseScale);

        // クルクル回る
        float angleY = (currentAge + partialTicks) * 5.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(angleY));

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        Matrix4f matrix = poseStack.last().pose();

        // 複層の三角形
        int triangleCount = 24; // 24個の三角形
        for (int i = 0; i < triangleCount; i++) {
            poseStack.pushPose();
            // 各三角形を少しずつ回転させて重ねる
            float offsetAngle = (float) (i * 360.0 / triangleCount);
            float pulse = (float) Math.sin((currentAge + partialTicks) * 0.1 + i * 0.5) * 0.2f;
            poseStack.mulPose(Axis.ZP.rotationDegrees(offsetAngle + angleY * (i % 2 == 0 ? 1 : -1)));
            
            // 三角形の色を階層ごとに変える（紫〜白）
            int r = 150 + (i * 100 / triangleCount);
            int g = 50 + (i * 50 / triangleCount);
            int b = 255;
            int a = 150 + (int)(pulse * 50);

            drawTriangle(poseStack.last().pose(), vertexConsumer, 1.0f + pulse, r, g, b, a);
            poseStack.popPose();
        }

        // 外周の円
        drawCircle(matrix, vertexConsumer, 48, 1.2f, 255, 255, 255, 100);
        drawCircle(matrix, vertexConsumer, 36, 1.5f, 200, 0, 255, 50);

        poseStack.popPose();
        poseStack.popPose();
    }

    private void drawTriangle(Matrix4f matrix, VertexConsumer consumer, float radius, int r, int g, int b, int a) {
        float t = 0.04f; // 線の太さ
        for (int i = 0; i < 3; i++) {
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
    }

    private void drawCircle(Matrix4f matrix, VertexConsumer consumer, int segments, float radius, int r, int g, int b, int a) {
        float t = 0.02f;
        for (int i = 0; i < segments; i++) {
            float a1 = (float) (i * 2 * Math.PI / segments);
            float a2 = (float) ((i + 1) * 2 * Math.PI / segments);
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
    public ResourceLocation getTextureLocation(UltimaMagicCircleEntity pEntity) {
        return new ResourceLocation("minecraft", "textures/entity/end_crystal/end_crystal_beam.png");
    }
}
