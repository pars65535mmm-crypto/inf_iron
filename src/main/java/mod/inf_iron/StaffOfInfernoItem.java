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

public class StaffOfInfernoItem extends Item {

    public StaffOfInfernoItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText("劫焔の杖 ‐ 終末 ‐");
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel,
                                @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§c世界が熱で溶けゆく..."));
        pTooltipComponents.add(Component.literal("§4全てを熱で破壊する地獄の業火"));
        pTooltipComponents.add(Component.literal("§6§l- 連鎖爆発 -"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        // クールダウン 2400 tick (2分)
        pPlayer.getCooldowns().addCooldown(this, 2400);

        if (!pLevel.isClientSide) {
            // プレイヤーが向いている方向への数歩先
            Vec3 dir = pPlayer.getLookAngle().normalize();
            Vec3 spawnPos = pPlayer.position().add(dir.scale(10.0)).add(0, 1, 0); 

            InfernoExplosionEntity inferno = ModEntities.INFERNO_EXPLOSION.get().create(pLevel);
            if (inferno != null) {
                inferno.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                inferno.setOwner(pPlayer);
                pLevel.addFreshEntity(inferno);
                
                // 詠唱開始音
                pLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                        net.minecraft.sounds.SoundEvents.DRAGON_FIREBALL_EXPLODE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 10.0f, 0.5f);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack pStack) {
        return true; 
    }
}
