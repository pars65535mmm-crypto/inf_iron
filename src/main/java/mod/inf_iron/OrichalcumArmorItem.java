package mod.inf_iron;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import java.util.List;

public class OrichalcumArmorItem extends ArmorItem {

    private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{
        UUID.fromString("1A2B3C4D-5E6F-7A8B-9C0D-1E2F3A4B5C6D"),
        UUID.fromString("2B3C4D5E-6F7A-8B9C-0D1E-2F3A4B5C6D7E"),
        UUID.fromString("3C4D5E6F-7A8B-9C0D-1E2F-3A4B5C6D7E8F"),
        UUID.fromString("4D5E6F7A-8B9C-0D1E-2F3A-4B5C6D7E8F9A")
    };

    public OrichalcumArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getBluishWhiteGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§6【発動スキル】"));
        pTooltipComponents.add(Component.literal("§e不死身 / 天上天下唯我独尊 / 夢幻加速 / 終焉無効 / 森羅万象 / 虚無崩壊"));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("「逃げちゃダメだ、逃げちゃダメだ、逃げちゃダメだ……いや、俺が無敵なんだから敵が逃げるべきでは？」"));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pEquipmentSlot) {
        if (pEquipmentSlot == this.type.getSlot()) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(pEquipmentSlot));
            UUID uuid = ARMOR_MODIFIER_UUID_PER_SLOT[pEquipmentSlot.getIndex()];
            // HP+1024
            builder.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "Armor health boost", 1024.0, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 220, 9, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SATURATION, 220, 255, false, false, true));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 220, 255, false, false, true));

            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        }

        if (!level.isClientSide()) {
        // フル装備（4部位すべてがオリハルコンアーマー）の時だけ発動
        boolean hasFullSet = true;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.isEmpty() || !(armor.getItem() instanceof OrichalcumArmorItem)) {
                hasFullSet = false;
                break;
            }
        }

if (hasFullSet) {
            // 1. システムからのあらゆる消滅命令を拒否（本番SRG: m_142145_）
            try {
                java.lang.reflect.Method unsetRemovedMethod = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                    net.minecraft.world.entity.Entity.class, 
                    "m_142145_"
                );
                unsetRemovedMethod.setAccessible(true);
                unsetRemovedMethod.invoke(player);
            } catch (Exception e) {
                // 予備策：removalReason (f_19774_) を直接 null に上書き
                try {
                    net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                        net.minecraft.world.entity.Entity.class, 
                        player, 
                        null, 
                        "f_19774_"
                    );
                } catch (Exception ignored) {}
            }
            
            // 2. 死亡フラグ(f_20893_)の強制リセット
            try {
                net.minecraftforge.fml.util.ObfuscationReflectionHelper.setPrivateValue(
                    net.minecraft.world.entity.LivingEntity.class, 
                    player, 
                    false, 
                    "f_20893_"
                );
            } catch (Exception ignored) {}

            if (player.getHealth() < player.getMaxHealth() || player.isDeadOrDying()) {
                player.setHealth(player.getMaxHealth());
                player.deathTime = 0;
            }
            
            // 3. 無敵フラグ維持
            player.setInvulnerable(true);

            // 4. 戦闘履歴のクレンジング（本番SRG: m_19270_ をリフレクションで安全に実行）
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                try {
                    java.lang.reflect.Method recheckStatusMethod = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findMethod(
                        net.minecraft.world.damagesource.CombatTracker.class, 
                        "m_19270_"
                    );
                    recheckStatusMethod.setAccessible(true);
                    recheckStatusMethod.invoke(serverPlayer.getCombatTracker());
                } catch (Exception ignored) {}
            }
        }
        
    }
    }
}
