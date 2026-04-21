package mod.inf_iron;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ExcalipoorItem extends SwordItem {
    public ExcalipoorItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@Nonnull EquipmentSlot slot) {
        // 全く属性を持たない（攻撃力1、追加速度なし）ようにすることで、バニラの属性表示を無効化
        return ImmutableMultimap.of();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag isAdvanced) {
        // 見せかけの攻撃力を表示する
        tooltipComponents.add(Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal(" 100 ").withStyle(ChatFormatting.DARK_GREEN)
                .append(Component.translatable("attribute.name.generic.attack_damage").withStyle(ChatFormatting.DARK_GREEN)));
        
        // プレイヤーへのヒント
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("見た目は強そうだが... 攻撃力は飾りだ。").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
