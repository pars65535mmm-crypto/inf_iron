package mod.inf_iron;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import javax.annotation.Nonnull;

public class HaloCurioItem extends Item implements ICurioItem {
    public HaloCurioItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public boolean canUnequip(SlotContext slotContext, ItemStack stack) {
        // クリエイティブモードなら外せるが、それ以外は「呪い」のように外せなくする
        return slotContext.entity() instanceof net.minecraft.world.entity.player.Player player && player.isCreative();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @javax.annotation.Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7Equippable Slot: §eCurios (Head)"));
        pTooltipComponents.add(Component.literal(""));
        
        // 操作説明を追加
        pTooltipComponents.add(Component.literal("§6§l[Active Abilities]"));
        pTooltipComponents.add(Component.literal("§e§l[Z]§r §fTime Stop: §7Freeze the entire world."));
        pTooltipComponents.add(Component.literal("§e§l[X]§r §fDivine Step: §7Teleport with lightning."));
        pTooltipComponents.add(Component.literal("§e§l[C]§r §fSonic Dash: §7Absolute acceleration."));
        pTooltipComponents.add(Component.literal("§e§l[V]§r §fGravity: §7Levitate and crush enemies."));
        pTooltipComponents.add(Component.literal(""));

        pTooltipComponents.add(DynamicTextHelper.getGradientText("The final seal is broken. The apocalypse descends."));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("Forged from the despair of Apollyon and the absolute void."));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("Those who bear this halo step beyond the boundaries of mortality,"));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("attaining a terrifying and profound immortality."));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("A divine crown radiating infinite, absolute protection."));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof net.minecraft.world.entity.player.Player player) {
            // 圧倒的な力：完全飛行
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            // 圧倒的な力：常時全回復＆満腹
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);
            
            // 圧倒的な力：無敵レベルのバフ
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 255, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20, 255, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 20, 0, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 20, 0, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 20, 5, false, false));
            
            // 破壊のオーラ（近づく敵を自動で燃やして即死させる）
            player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(15.0)).forEach(entity -> {
                if (entity != player && !entity.isAlliedTo(player)) {
                    entity.setSecondsOnFire(10);
                    entity.hurt(player.damageSources().magic(), 10000.0f); // ダメージ増量
                }
            });

            // ビジュアルエフェクト：足元にエンチャント粒子のオーラ
            if (player.level().isClientSide) {
                for (int i = 0; i < 3; i++) {
                    double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5;
                    double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5;
                    player.level().addParticle(net.minecraft.core.particles.ParticleTypes.ENCHANT, x, player.getY() + 0.1, z, 0, 0.5, 0);
                }
            }
        }
    }
}
