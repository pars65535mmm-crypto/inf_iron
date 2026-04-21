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

public class StaffOfUltimaItem extends Item {

    public StaffOfUltimaItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText("究極の杖 ‐ アルテマ ‐");
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel,
                                @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.literal("§8万物を消滅させ、森羅万象を虚無へと帰す。"));
        pTooltipComponents.add(Component.literal("§8虚空を生み出す、禁忌の究極魔法。"));
        pTooltipComponents.add(Component.literal("§4§l- ULTIMA -"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        // クールダウン 1200 tick (60秒) 究極魔法なので一分間のインターバル
        pPlayer.getCooldowns().addCooldown(this, 1200);

        if (!pLevel.isClientSide) {
            // プレイヤーの目の前の地面に魔法陣を展開
            Vec3 look = pPlayer.getLookAngle();
            Vec3 spawnPos = pPlayer.position().add(look.x * 5, 0.1, look.z * 5);
            
            UltimaMagicCircleEntity magicCircle = ModEntities.ULTIMA_MAGIC_CIRCLE.get().create(pLevel);
            if (magicCircle != null) {
                magicCircle.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                magicCircle.setOwner(pPlayer);
                pLevel.addFreshEntity(magicCircle);
                
                // 究極魔法特有の低音の響き
                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                        net.minecraft.sounds.SoundSource.PLAYERS, 10.0f, 0.1f);
                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
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
