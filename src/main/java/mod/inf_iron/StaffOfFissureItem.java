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

public class StaffOfFissureItem extends Item {

    public StaffOfFissureItem(Properties properties) {
        super(properties);
    }

    @Override
    @Nonnull
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText("裂核の杖 ‐ 爆裂 ‐");
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @Nullable Level pLevel,
                                @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        // "誰かが昔残した..いや残された..力と記憶が残っている... - 爆裂 -"
        pTooltipComponents.add(Component.literal("§8誰かが昔残した..いや残された.."));
        pTooltipComponents.add(Component.literal("§8力と記憶が残っている..."));
        pTooltipComponents.add(Component.literal("§c§l- 爆裂 -"));
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    @Nonnull
    public InteractionResultHolder<ItemStack> use(@Nonnull Level pLevel, @Nonnull Player pPlayer, @Nonnull InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);

        // クールダウン 400 tick (20秒) 大技なので長めに設定
        pPlayer.getCooldowns().addCooldown(this, 400);

        if (!pLevel.isClientSide) {
            // FissureMagicCircleEntity のスポーン
            Vec3 spawnPos = pPlayer.position().add(0, 10, 0); // プレイヤーの10ブロック上空
            FissureMagicCircleEntity magicCircle = ModEntities.FISSURE_MAGIC_CIRCLE.get().create(pLevel);
            if (magicCircle != null) {
                magicCircle.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                magicCircle.setOwner(pPlayer);
                pLevel.addFreshEntity(magicCircle);
                
                // 詠唱開始音
                pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                        net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 5.0f, 0.1f);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    @Override
    public boolean isFoil(@Nonnull ItemStack pStack) {
        return true; // 常にエンチャントの輝き
    }
}
