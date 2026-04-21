package mod.inf_iron;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VeritasCuriosItem extends Item implements ICurioItem {
    public VeritasCuriosItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "back".equals(slotContext.identifier()) || "curio".equals(slotContext.identifier());
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (slotContext.entity() instanceof Player player && !player.level().isClientSide) {
            Level level = player.level();
            
            // 既存のブレードを数える
            List<SpectralBladeEntity> blades = level.getEntitiesOfClass(SpectralBladeEntity.class, player.getBoundingBox().inflate(64.0),
                e -> e.getOwner() == player);

            if (blades.size() < 12 && player.tickCount % 20 == 0) {
                // 不足分を順次召喚
                int id = blades.size();
                SpectralBladeEntity blade = ModEntities.SPECTRAL_BLADE.get().create(level);
                if (blade != null) {
                    blade.setPos(player.getX(), player.getY() + 2, player.getZ());
                    blade.setOwner(player, id);
                    level.addFreshEntity(blade);
                }
            }
        }
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§7Equippable Slot: §eCurios (Back)"));
        pTooltipComponents.add(Component.literal(""));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("The phantom blades of the void emperor."));
        pTooltipComponents.add(DynamicTextHelper.getGradientText("Twelve shadows that weave the end of reality."));
        pTooltipComponents.add(Component.literal(""));
        pTooltipComponents.add(Component.literal("§bPassive: §fSummons 12 Spectral Blades while equipped."));
        pTooltipComponents.add(Component.literal("§cDamage: §f300 per hit."));
        pTooltipComponents.add(Component.literal("§7(Blades respect collisions and will not clip walls)"));
    }
}
