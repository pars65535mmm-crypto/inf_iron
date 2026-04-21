package mod.inf_iron;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import javax.annotation.Nonnull;

public class HomingBowItem extends BowItem {
    public HomingBowItem(Properties pProperties) {
        super(pProperties);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @javax.annotation.Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(DynamicTextHelper.getGradientText("The arrow knows where it is at all times."));
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack pStack, @Nonnull Level pLevel, @Nonnull LivingEntity pEntityLiving, int pTimeLeft) {
        if (pEntityLiving instanceof Player player) {
            // "必中・ホーミング" (Guaranteed Hit, Homing)
            if (!pLevel.isClientSide) {
                double radius = 100.0D;
                AABB area = player.getBoundingBox().inflate(radius);
                
                List<Entity> targets = new java.util.ArrayList<>(pLevel.getEntities((Entity)null, area, entity -> entity != player && entity instanceof LivingEntity));
                // Add hidden entities using GodReflector
                targets.addAll(GodReflector.findHiddenEntities(pLevel));
                
                Entity closest = null;
                double closestDistSq = Double.MAX_VALUE;
                for (Entity e : targets) {
                    if (e == player) continue;
                    double distSq = e.distanceToSqr(player);
                    if (distSq < closestDistSq) {
                        closestDistSq = distSq;
                        closest = e;
                    }
                }
                
                if (closest != null) {
                    // Wipe the target instantly, mimicking the GodKiller
                    GodKillerItem.performTargetWipe(player, closest);
                }
            }
        }
    }
}
