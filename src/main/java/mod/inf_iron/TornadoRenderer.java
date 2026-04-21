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

public class TornadoRenderer extends EntityRenderer<TornadoEntity> {

    public TornadoRenderer(EntityRendererProvider.Context pContext) {
        super(pContext);
    }

    @Override
    public void render(TornadoEntity pEntity, float pEntityYaw, float pPartialTicks, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {
        super.render(pEntity, pEntityYaw, pPartialTicks, poseStack, pBuffer, pPackedLight);
        
        int tick = pEntity.tickCount;
        int castTicks = 150;
        
        // 詠唱期間のみ魔法陣を描画
        if (tick < castTicks) {
            float progress = (tick + pPartialTicks) / (float)castTicks;
            
            poseStack.pushPose();
            // 上空に展開
            poseStack.translate(0, 15.0, 0);

            // 巨大化する魔法陣
            float scale = 1.0f + progress * 40.0f; // 巨大
            poseStack.scale(scale, scale, scale);

            // クルクル回る
            float angleY = (tick + pPartialTicks) * 15.0f;
            poseStack.mulPose(Axis.YP.rotationDegrees(angleY));
            
            // ベースの水平魔法陣
            VertexConsumer vertexConsumer = pBuffer.getBuffer(RenderType.lightning());
            poseStack.pushPose();
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            Matrix4f matrix1 = poseStack.last().pose();
            
            // 外側の円 (水色 / シアン)
            drawPolygon(matrix1, vertexConsumer, 36, 1.0f, 0, 255, 255, 255); 
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(-angleY * 2.0f));
            // 青い星
            drawStar(poseStack.last().pose(), vertexConsumer, 1.0f, 0, 100, 255, 255);
            poseStack.popPose();
            // 内側の円
            drawPolygon(matrix1, vertexConsumer, 18, 0.6f, 0, 200, 150, 255);
            poseStack.popPose();

            // 風を模した縦方向の回転陣
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(75.0f));
            poseStack.mulPose(Axis.YP.rotationDegrees(angleY * 3.0f));
            Matrix4f matrix2 = poseStack.last().pose();
            // エメラルド色
            drawPolygon(matrix2, vertexConsumer, 24, 1.2f, 50, 255, 100, 150); 
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
    public ResourceLocation getTextureLocation(TornadoEntity pEntity) {
        return new ResourceLocation("minecraft", "textures/block/cobweb.png"); // ダミー
    }
}
