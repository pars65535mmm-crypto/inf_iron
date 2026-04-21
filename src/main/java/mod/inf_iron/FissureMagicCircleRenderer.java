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

public class FissureMagicCircleRenderer extends EntityRenderer<FissureMagicCircleEntity> {

    public FissureMagicCircleRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(FissureMagicCircleEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        int tick = entity.getRadiusTick(); 
        if (tick >= 200) return;

        // 進行度 (0.0 〜 1.0)
        float progress = (tick + partialTicks) / 200.0f;
        
        poseStack.pushPose();

        // 巨大化する魔法陣
        float scale = 1.0f + progress * 50.0f; 
        poseStack.scale(scale, scale, scale);

        // クルクル回る
        float angleY = (tick + partialTicks) * 10.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
        
        // ベースの水平魔法陣
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        Matrix4f matrix1 = poseStack.last().pose();
        drawPolygon(matrix1, vertexConsumer, 36, 1.0f, 255, 50, 50, 255); // 外側の円 (赤)
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(-angleY * 2.0f));
        drawStar(poseStack.last().pose(), vertexConsumer, 1.0f, 255, 0, 0, 255);
        poseStack.popPose();
        drawPolygon(matrix1, vertexConsumer, 12, 0.4f, 255, 100, 100, 255);
        poseStack.popPose();

        // Y軸に回転した球体のような防御陣・追加陣
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0f + angleY * 0.5f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
        Matrix4f matrix2 = poseStack.last().pose();
        drawPolygon(matrix2, vertexConsumer, 24, 0.8f, 150, 0, 255, 200); // 紫色の円
        drawPolygon(matrix2, vertexConsumer, 3, 0.8f, 200, 100, 255, 255); // トライアングル
        poseStack.popPose();

        // 逆斜めの陣
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-45.0f - angleY * 0.5f));
        poseStack.mulPose(Axis.XP.rotationDegrees(45.0f));
        Matrix4f matrix3 = poseStack.last().pose();
        drawPolygon(matrix3, vertexConsumer, 36, 0.9f, 255, 150, 0, 200); // オレンジの円
        drawStar(matrix3, vertexConsumer, 0.6f, 255, 255, 0, 200); // 黄色い星
        poseStack.popPose();

        // もう一つ垂直方向
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(angleY * 1.5f));
        Matrix4f matrix4 = poseStack.last().pose();
        drawPolygon(matrix4, vertexConsumer, 20, 1.1f, 50, 0, 50, 150); // 黒っぽい軌道
        poseStack.popPose();

        poseStack.popPose();
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

            // 線を描画（Quadを細い線として描画）
            float thickness = 0.05f;
            consumer.vertex(matrix, x1, y1 - thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 - thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x2, y2 + thickness, 0).color(r, g, b, a).endVertex();
            consumer.vertex(matrix, x1, y1 + thickness, 0).color(r, g, b, a).endVertex();
        }
    }

    // 六芒星の描画
    private void drawStar(Matrix4f matrix, VertexConsumer consumer, float radius, int r, int g, int b, int a) {
        // トライアングル1
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
        // トライアングル2
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
    public ResourceLocation getTextureLocation(FissureMagicCircleEntity pEntity) {
        return new ResourceLocation("minecraft", "textures/entity/end_crystal/end_crystal_beam.png"); // 未使用（RenderType.lightning）のためダミー
    }
}
