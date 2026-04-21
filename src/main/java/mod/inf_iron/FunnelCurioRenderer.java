package mod.inf_iron;

import net.minecraft.world.entity.Entity;
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

public class FunnelCurioRenderer implements ICurioRenderer {
    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext,
            PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource renderTypeBuffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw,
            float headPitch) {
        LivingEntity entity = slotContext.entity();
        EntityModel<T> model = renderLayerParent.getModel();

        poseStack.pushPose();

        ICurioRenderer.translateIfSneaking(poseStack, entity);
        ICurioRenderer.rotateIfSneaking(poseStack, entity);

        // 体の回転に追従
        if (model instanceof HumanoidModel<?> humanoidModel) {
            ICurioRenderer.followBodyRotations(entity, (HumanoidModel<LivingEntity>) humanoidModel);
        }

        int fullLight = 15728880;
        float time = ageInTicks + partialTicks;
        net.minecraft.nbt.CompoundTag nbt = stack.getTag();

        // 6台のファンネルを浮遊・射出させる
        for (int i = 0; i < 6; i++) {
            poseStack.pushPose();

            // --- 座標計算 ---
            // 基礎となる旋回位置（Orbit）
            float offset = i * (float) (Math.PI * 2.0 / 6.0);
            double orbitX = Math.sin(time * 0.1 + offset) * 1.2;
            double orbitY = Math.cos(time * 0.07 + offset * 0.5) * 0.4 - 0.2;
            double orbitZ = 0.6 + Math.cos(time * 0.1 + offset) * 0.3;

            double finalX = orbitX;
            double finalY = orbitY;
            double finalZ = orbitZ;

            // NBTから状態を取得して補間
            if (nbt != null) {
                String prefix = "Funnel" + i;
                int state = nbt.getInt(prefix + "State");
                long startTime = nbt.getLong(prefix + "StartTime");
                float progress = (entity.level().getGameTime() - startTime + partialTicks);

                if (state == 1) { // Launching
                    java.util.UUID targetUuid = nbt.getUUID(prefix + "Target");
                    net.minecraft.world.entity.Entity target = null;
                    // クライアント側でエンティティを検索
                    for (Entity e : entity.level().getEntities(entity, entity.getBoundingBox().inflate(40.0))) {
                        if (e.getUUID().equals(targetUuid)) {
                            target = e;
                            break;
                        }
                    }

                    if (target != null) {
                        float lerp = Math.min(progress / 15.0f, 1.0f);
                        // プレイヤーの背後からターゲットへ
                        double tx = (target.getX() - entity.getX()) * lerp;
                        double ty = ((target.getY() + 1.0) - entity.getY()) * lerp;
                        double tz = (target.getZ() - entity.getZ()) * lerp;

                        finalX = orbitX + tx;
                        finalY = orbitY + ty;
                        finalZ = orbitZ + tz;
                    }
                } else if (state == 2) { // Returning
                    float lerp = 1.0f - Math.min(progress / 10.0f, 1.0f);
                    // 帰還中は適当に軌道を戻す（ターゲット座標が不明なため、前回の位置を推測するのは難しいが、
                    // ここでは単に orbit 位置への接近として表現）
                    finalX = orbitX * (1.0 + lerp * 2.0);
                    finalY = orbitY * (1.0 + lerp * 2.0);
                    finalZ = orbitZ * (1.0 + lerp * 2.0);
                }
            }

            poseStack.translate(finalX, finalY, finalZ);

            // ファンネル自体の回転
            poseStack.mulPose(Axis.YP.rotationDegrees(time * 15.0f + i * 45f));
            poseStack.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(time * 0.1 + offset) * 20f));

            poseStack.scale(0.35f, 0.35f, 0.35f);

            // 本体（ネザースター：禍々しいエネルギー体）
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    new ItemStack(Items.NETHER_STAR),
                    ItemDisplayContext.FIXED,
                    fullLight,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    poseStack,
                    renderTypeBuffer,
                    entity.level(),
                    0);

            // パーツ（エンドロッド：収束器）を上下に配置
            for (int j = -1; j <= 1; j += 2) {
                poseStack.pushPose();
                poseStack.translate(0, 0.4 * j, 0);
                poseStack.scale(0.4f, 1.5f, 0.4f);
                if (j == 1)
                    poseStack.mulPose(Axis.XP.rotationDegrees(180f));
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        new ItemStack(Items.END_ROD),
                        ItemDisplayContext.FIXED,
                        fullLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                        poseStack,
                        renderTypeBuffer,
                        entity.level(),
                        0);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
