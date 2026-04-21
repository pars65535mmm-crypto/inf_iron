package mod.inf_iron;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class StaffOfTornadoItem extends Item {

    public StaffOfTornadoItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText("旋龗の杖 ‐ 龍巻 ‐");
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel,
                                @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§b海神の怒りが渦を巻く..."));
        pTooltipComponents.add(Component.literal("§3全てを呑み込む風の化身"));
        pTooltipComponents.add(Component.literal("§f§l- 暴風 -"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        // クールダウン 1200 tick (1分)
        pPlayer.getCooldowns().addCooldown(this, 1200);

        if (!pLevel.isClientSide) {
            Vec3 spawnPos = pPlayer.position(); 
            TornadoEntity tornado = ModEntities.TORNADO.get().create(pLevel);
            if (tornado != null) {
                tornado.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                tornado.setOwner(pPlayer);
                pLevel.addFreshEntity(tornado);
                
                // 詠唱開始音
                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                        net.minecraft.sounds.SoundSource.PLAYERS, 5.0f, 0.1f);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack pStack) {
        return true; 
    }
}
