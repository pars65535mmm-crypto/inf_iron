package mod.inf_iron;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class HeavyIronDrillItem extends PickaxeItem {

    public HeavyIronDrillItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            // プレイヤーの視線方向のブロックを取得
            HitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

            if (hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHitResult = (BlockHitResult) hitResult;
                BlockPos hitPos = blockHitResult.getBlockPos();
                Direction hitFace = blockHitResult.getDirection();

                // 破壊不可ブロック（岩盤など）は破壊しない安全設計
                mine3x3x3Area(serverLevel, player, hitPos, hitFace);
                
                // クールダウン 0.5秒
                player.getCooldowns().addCooldown(this, 10);
                
                return InteractionResultHolder.success(player.getItemInHand(hand));
            }
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    private void mine3x3x3Area(ServerLevel level, Player player, BlockPos center, Direction hitFace) {
        // 掘り進める方向（当たった面の反対方向）
        Direction mineDirection = hitFace.getOpposite();

        // 当たった面に対して平行な2つの軸を決定
        Direction down = Direction.DOWN;
        Direction right;

        if (mineDirection.getAxis() == Direction.Axis.Y) {
            // 上下に掘る場合
            down = Direction.NORTH;
            right = Direction.EAST;
        } else {
            // 水平方向に掘る場合
            right = mineDirection.getCounterClockWise();
        }

        // 3x3x3 の範囲を計算
        // 奥行き 0, 1, 2 (視線方向へ3ブロック)
        for (int depth = 0; depth < 3; depth++) {
            // 縦横 -1, 0, 1 (3x3の平面)
            for (int w = -1; w <= 1; w++) {
                for (int h = -1; h <= 1; h++) {
                    // 対象座標の算出
                    BlockPos targetPos = center.relative(mineDirection, depth)
                            .relative(right, w)
                            .relative(down, h);

                    BlockState state = level.getBlockState(targetPos);

                    // 空気、または岩盤などの破壊不可ブロック（destroySpeedが-1以下）は無視
                    if (!state.isAir() && state.getDestroySpeed(level, targetPos) >= 0) {
                        // アイテムをドロップしつつブロックを破壊
                        level.destroyBlock(targetPos, true, player);
                    }
                }
            }
        }
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("ロマンの塊、6重圧縮鉄を贅沢に使用！").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("右クリック:").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal(" 向いている方向の 3x3x3 エリアを硬度無視で一括粉砕化！ (岩盤以外)").withStyle(ChatFormatting.YELLOW));
    }
}
