package mod.inf_iron;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import javax.annotation.Nonnull;

public class FunnelCurioItem extends Item implements ICurioItem {
    public FunnelCurioItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "back".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        return slotContext.entity() instanceof Player player && player.isCreative();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @javax.annotation.Nullable Level pLevel,
            @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7Equippable Slot: §eCurios (Back)"));
        pTooltipComponents.add(Component.literal(""));

        pTooltipComponents.add(DynamicTextHelper.getGradientText("背後に漂う六つの影。それは守護であり、裁きでもある。"));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("収束する次元の歪みが、逃れられぬ『死』を紡ぎ出す。"));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("かつて空を統べた者たちの残滓。今、再び終焉の光を放つ。"));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("全方位攻撃型端末『終焉の翼』——其の輝きは、絶望の声に等しい。"));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            Level level = player.level();
            boolean isClient = level.isClientSide;

            // --- 1. 特殊移動機能：エリトラ式滑空 & ブロック貫通 ---
            // 二段ジャンプ的な飛行をやめ、エリトラ挙動へ
            if (!player.onGround() && !player.isFallFlying() && !player.getAbilities().flying) {
                // 空中でジャンプ等、滑空条件を満たしたときに自動起動 (条件は緩めに)
                if (player.getDeltaMovement().y < -0.1) {
                    player.startFallFlying();
                }
            }

            // 滑走中（エリトラ状態）またはクリエイティブ飛行中の加速処理
            if (player.isFallFlying() || player.getAbilities().flying) {
                // 滑空中の加速（エリトラっぽいがもっと速い）
                Vec3 look = player.getLookAngle();
                Vec3 move = player.getDeltaMovement();
                player.setDeltaMovement(move.add(look.x * 0.1, look.y * 0.05, look.z * 0.1));
            }
            
            // ブロック貫通は常に無効（あるいは必要ならここを調整）
            player.noPhysics = false;

            // クリエイティブ飛行権限は付与しておく（緊急離脱用）が、メインは滑空
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }

            // --- 2. ファンネル制御ロジック (サーバーサイドメイン) ---
            if (!isClient) {
                net.minecraft.nbt.CompoundTag nbt = stack.getOrCreateTag();
                long currentTime = level.getGameTime();

                for (int i = 0; i < 6; i++) {
                    String prefix = "Funnel" + i;
                    int state = nbt.getInt(prefix + "State"); // 0:Orbit, 1:Launch, 2:Return

                    if (state == 0) { // Orbiting
                        // 近くの敵を探す
                        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(20.0));
                        for (LivingEntity target : targets) {
                            if (target != player && !target.isAlliedTo(player) && target.isAlive()) {
                                // 発射！
                                nbt.putInt(prefix + "State", 1);
                                nbt.putUUID(prefix + "Target", target.getUUID());
                                nbt.putLong(prefix + "StartTime", currentTime);
                                break;
                            }
                        }
                    } else if (state == 1) { // Launching
                        java.util.UUID targetUuid = nbt.getUUID(prefix + "Target");
                        Entity target = ((net.minecraft.server.level.ServerLevel) level).getEntity(targetUuid);

                        if (target == null || !target.isAlive() || target.distanceToSqr(player) > 400) {
                            nbt.putInt(prefix + "State", 2); // ターゲット消失なら帰還
                        } else {
                            // 距離判定（着弾）
                            double distSqr = target.getBoundingBox().inflate(0.5).distanceToSqr(player.position()); // 暫定
                            // 実際にはレンダラー側で動かすが、ロジック上では一定時間で着弾とする
                            if (currentTime - nbt.getLong(prefix + "StartTime") > 15) {
                                // 着弾：爆発
                                level.explode(player, target.getX(), target.getY() + 1.0, target.getZ(), 2.0f, false,
                                        Level.ExplosionInteraction.NONE);
                                target.hurt(player.damageSources().magic(), 50.0f);
                                if (target instanceof LivingEntity living) {
                                    living.invulnerableTime = 0;
                                }
                                nbt.putInt(prefix + "State", 2);
                                nbt.putLong(prefix + "StartTime", currentTime);
                            }
                        }
                    } else if (state == 2) { // Returning
                        if (currentTime - nbt.getLong(prefix + "StartTime") > 10) {
                            nbt.putInt(prefix + "State", 0); // 帰還完了
                        }
                    }
                }
            }

            // 視覚エフェクト：吸引などの粒子は継続
            if (isClient) {
                // 吸引粒子
                level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(10.0)).forEach(target -> {
                    if (target != player && !target.isAlliedTo(player)) {
                        level.addParticle(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, target.getX(),
                                target.getY() + 1.0, target.getZ(), 0, 0, 0);
                    }
                });
            }
        }
    }
}
