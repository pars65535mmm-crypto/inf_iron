package mod.inf_iron;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

public class GodKillerItem extends SwordItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public GodKillerItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
        
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", (double)2000.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", 1000.0D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@Nonnull EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            performTargetWipe(player, entity);
            performAoeWipe(player, player.level());
        }
        return true; // 攻撃をキャンセルして独自処理に
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(DynamicTextHelper.getGradientText("All targets shall be erased."));
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        if (!level.isClientSide) {
            performAoeWipe(player, level);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public static void performAoeWipe(Player player, Level level) {
        double radius = 10.0D;
        AABB area = player.getBoundingBox().inflate(radius);
        
        // 1. 標準的な取得 (バニラレベル)
        List<Entity> targets = new ArrayList<>(level.getEntities((Entity)null, area, entity -> entity != player));
        
        // 2. 隠蔽されている実体をリフレクションで強制取得 
        Set<Entity> allEntities = new HashSet<>(targets);
        allEntities.addAll(GodReflector.findHiddenEntities(level));
        
        for (Entity target : allEntities) {
            if (target == null || target == player) continue;
            
            // 範囲外のエンティティは無視 (hiddenEntities は全実体を取得するため)
            if (target.distanceToSqr(player) > radius * radius) continue;

            wipeEntity(target);
        }
    }

    public static void performTargetWipe(Player player, Entity target) {
        if (target != null && target != player) {
            wipeEntity(target);
        }
    }

    private static void wipeEntity(Entity target) {
        try {
            // NBT抹消 (データの初期化)
            target.load(new CompoundTag());
            
            // メソッドをバイパスした強制削除 (Mixinチェック回避)
            GodReflector.forceRemove(target);
            
            // 念のための標準的な削除命令
            target.discard();
        } catch (Exception e) {
            target.discard();
        }
    }
}
