package mod.inf_iron;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class InfinityRingItem extends Item implements ICurioItem {

    public InfinityRingItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText("零の指輪 ‐ Ring of Zero ‐");
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel,
                                @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7Equippable Slot: §eCurios (Ring)"));
        pTooltipComponents.add(Component.literal(""));
        pTooltipComponents.add(Component.literal("§b§l[Passive Ability]"));
        pTooltipComponents.add(Component.literal("§fZero Constraint: §7Removes cooldowns for the held item."));
        pTooltipComponents.add(Component.literal(""));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("All constraints return to zero. The law of absolute continuity."));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "ring".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player) {
            // メインハンドとオフハンドのアイテムのクールダウンを強制的に0にする
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

            if (!mainHand.isEmpty()) {
                player.getCooldowns().removeCooldown(mainHand.getItem());
            }
            if (!offHand.isEmpty()) {
                player.getCooldowns().removeCooldown(offHand.getItem());
            }
        }
    }
}
