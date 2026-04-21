package mod.inf_iron;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.tags.BlockTags;
import java.util.List;

public class OrichalcumPaxelItem extends DiggerItem {

    public OrichalcumPaxelItem(Tier tier, float attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(attackDamageModifier, attackSpeedModifier, tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
    }

    public static int getMiningRadius(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("MiningRadius")) {
            return stack.getTag().getInt("MiningRadius");
        }
        return 0; // 0 means 1x1 (radius 0)
    }

    public static void setMiningRadius(ItemStack stack, int radius) {
        stack.getOrCreateTag().putInt("MiningRadius", radius);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getBluishWhiteGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        int r = getMiningRadius(pStack);
        int size = (r * 2) + 1;
        pTooltipComponents.add(Component.literal("§bCurrent Mode: " + size + "x" + size));
        pTooltipComponents.add(DynamicTextHelper.getBluishWhiteGradientText("「岩盤、横から掘るか？下から掘るか？…いや、最初から無かったことにするか？」"));
        pTooltipComponents.add(Component.literal("§8(スニーク＋スクロールで1x1〜17x17のモードチェンジ)"));
    }
}
