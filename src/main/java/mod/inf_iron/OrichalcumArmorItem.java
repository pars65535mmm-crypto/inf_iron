package mod.inf_iron;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class OrichalcumArmorItem extends ArmorItem {

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
}
