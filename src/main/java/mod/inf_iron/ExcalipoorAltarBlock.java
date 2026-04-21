package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ExcalipoorAltarBlock extends Block {
    public static final BooleanProperty HAS_SWORD = BooleanProperty.create("has_sword");

    public ExcalipoorAltarBlock(BlockBehaviour.Properties properties) {
        super(properties.strength(5.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(HAS_SWORD, true));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(HAS_SWORD)) {
            if (!level.isClientSide) {
                // エクスカリパーをプレイヤーに与える
                ItemStack sword = new ItemStack(ModItems.EXCALIPOOR.get());
                if (!player.getInventory().add(sword)) {
                    player.drop(sword, false);
                }
                // ブロックの状態を「剣なし」に変更
                level.setBlock(pos, state.setValue(HAS_SWORD, false), 3);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HAS_SWORD);
    }
}
