package mod.inf_iron;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import javax.annotation.Nonnull;
import java.util.List;

public class OrichalcumBladeItem extends SwordItem {

    private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> defaultModifiers;

    public OrichalcumBladeItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> builder = com.google.common.collect.ImmutableMultimap.builder();
        builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 9999999999.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", 1000.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        return slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && entity instanceof LivingEntity target) {
            Level level = player.level();
            
            // 派手な斬撃エフェクト
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
            }

            // 確殺処理（因果・絶ち）
            executeTrueMurder(level, player, target);
        }
        return true; 
    }

    private void executeTrueMurder(Level level, Player player, LivingEntity target) {
        if (target == player) return;
        
        // 無敵防具をシステム的に引剥がす（因果の破壊）
        if (target instanceof Player targetPlayer) {
            targetPlayer.getInventory().clearContent();
        } else {
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                target.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        
        // 無敵時間の強制解除
        target.invulnerableTime = 0;
        
        // 純粋な殺害（アニメーションとドロップを伴う）
        target.setHealth(0.0F);
        target.die(level.damageSources().playerAttack(player));
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                performSpiralConvergence(level, player);
            } else {
                performReincarnationDiffusion(level, player);
            }
        }
        
        player.getCooldowns().addCooldown(this, 20); 
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void performReincarnationDiffusion(Level level, Player player) {
        player.displayClientMessage(Component.literal("§d§l[輪廻・拡散] §r§f千の刃が悉くを断つ...！"), true);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 3.0F, 2.0F);
        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ILLUSIONER_CAST_SPELL, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.5F);

        // 半径64以内の敵全てに斬撃
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(64.0);
        java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area);
        
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (LivingEntity target : enemies) {
                if (target == player) continue;
                
                // 刃が飛んでいくような演出
                for (int j = 0; j < 5; j++) {
                    double ox = (level.random.nextDouble() - 0.5) * 4;
                    double oy = (level.random.nextDouble() - 0.5) * 4;
                    double oz = (level.random.nextDouble() - 0.5) * 4;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME, target.getX() + ox, target.getY() + 1 + oy, target.getZ() + oz, 0, -ox/5, -oy/5, -oz/5, 0.5);
                }

                // 強力な斬撃パーティクル
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.5, 0.5, 0.5, 0.0);
                serverLevel.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
                
                executeTrueMurder(level, player, target);
            }
        }
    }

    private void performSpiralConvergence(Level level, Player player) {
        player.displayClientMessage(Component.literal("§5§l[螺旋・収束・虚無・崩壊] §r§f深淵に平伏せよ...！"), true);
        
        net.minecraft.world.phys.HitResult hitResult = player.pick(128.0D, 0.0F, false);
        net.minecraft.world.phys.Vec3 center = hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hitResult.getLocation() : player.getEyePosition().add(player.getLookAngle().scale(30.0D));

        level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 0.5F);
        level.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 0.1F);

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            // ブラックホール的エフェクトの強化
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 1500, 4.0, 4.0, 4.0, 0.1);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, center.x, center.y, center.z, 10, 1.0, 1.0, 1.0, 0.1);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 200, 2.0, 2.0, 2.0, 0.0);
            
            // 半径100以内の全エンティティを特異点に強制転移＆圧殺
            net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(100.0);
            java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area);
            
            for (LivingEntity target : enemies) {
                if (target == player) continue;
                
                // 強制吸い寄せ（テレポ）
                target.teleportTo(center.x, center.y, center.z);
                
                // 無限の斬撃
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.5, 0.5, 0.5, 0.0);
                
                executeTrueMurder(level, player, target);
            }
            // 崩壊音
            serverLevel.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.WITHER_DEATH, net.minecraft.sounds.SoundSource.PLAYERS, 10.0f, 0.1f);
            serverLevel.playSound(null, center.x, center.y, center.z, net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN, net.minecraft.sounds.SoundSource.PLAYERS, 10.0f, 0.5f);
        }
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getBluishWhiteGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§c⚔ 攻撃力: -0i §7(概念特攻)"));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("「神殺しって言うけど…右から斬る？左から斬る？それとも…概念から斬り落とす？」"));
        pTooltipComponents.add(Component.literal("§8(※対象のHPを絶対零度で固定し、概念的に『殺害』します)"));
    }
}
