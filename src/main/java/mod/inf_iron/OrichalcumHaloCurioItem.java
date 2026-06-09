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

public class OrichalcumHaloCurioItem extends Item implements ICurioItem {
    public OrichalcumHaloCurioItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getBluishWhiteGradientText(super.getName(pStack).getString());
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "head".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @javax.annotation.Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7Equippable Slot: §eCurios (Head)"));
        pTooltipComponents.add(Component.literal(""));
        
        pTooltipComponents.add(Component.literal("§e§l【神域の権能: 星辰の理】§r"));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("『そして、宇宙は彼を中心に回り始めた。』"));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("全てのダメージを無効化し、近づく愚者を文字通り\"無\"に帰す。"));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("あらゆる概念を上書きする、究極の幻想光輪。"));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof net.minecraft.world.entity.player.Player player) {
            // HPは防具で1024になるが、ここでも回復や無敵付与を行う
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(20.0f);
            
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 20, 255, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 20, 255, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 20, 0, false, false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 20, 0, false, false));
            
            // 圧倒的なDeath Aura
            if (player.tickCount % 5 == 0) {
                player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(20.0)).forEach(entity -> {
                    if (entity != player && !entity.isAlliedTo(player)) {
                        entity.setHealth(0.0f);
                        entity.die(player.damageSources().magic());
                        if(player.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + 1.0, entity.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SONIC_BOOM, entity.getX(), entity.getY() + 1.0, entity.getZ(), 1, 0, 0, 0, 0);
                        }
                    }
                });
            }
            
            // Orichalcumの神々しいオーラ
            if (player.level().isClientSide && player.tickCount % 2 == 0) {
                double x = player.getX() + (player.getRandom().nextDouble() - 0.5) * 2.0;
                double z = player.getZ() + (player.getRandom().nextDouble() - 0.5) * 2.0;
                player.level().addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x, player.getY() + 0.1, z, 0, 0.1, 0);
                player.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, player.getY() + 1.0, z, 0, 0.1, 0);
            }
        }
    }
}
