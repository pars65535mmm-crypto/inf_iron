package mod.inf_iron;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OrichalcumItem extends Item {
    public OrichalcumItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        String tooltip = "The divine ring of eternity forged from the ultimate collapse of eighteenfold infinite iron. It shines endlessly like a halo.";
        pTooltipComponents.add(DynamicTextHelper.getGradientText(tooltip));
    }
}
