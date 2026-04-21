package mod.inf_iron;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

import javax.annotation.Nonnull;

public class OmegaWeaponItem extends SwordItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public OmegaWeaponItem(Tier tier, float attackSpeed, Properties properties) {
        super(tier, 0, attackSpeed, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // 攻撃力 255 (ベース1 + 254)
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 254.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", (double)attackSpeed, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@Nonnull EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 殴った相手にデバフの嵐を浴びせる (10秒 = 200 tick)
        int duration = 200;
        int amp = 4; // 強度は適当に高めに
        
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.HUNGER, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, duration, amp));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, amp));

        return super.hurtEnemy(stack, target, attacker);
    }
}
