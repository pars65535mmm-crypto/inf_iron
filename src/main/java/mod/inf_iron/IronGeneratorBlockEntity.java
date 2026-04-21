package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.concurrent.atomic.AtomicBoolean;

public class IronGeneratorBlockEntity extends BlockEntity {
    private int timer = 0;

    public IronGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_GENERATOR.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IronGeneratorBlockEntity blockEntity) {
        if (level.isClientSide) return;

        blockEntity.timer++;
        if (blockEntity.timer >= 20) { // 1秒 = 20 tick
            blockEntity.timer = 0;
            blockEntity.produceIron(level, pos);
        }
    }

    private void produceIron(Level level, BlockPos pos) {
        ItemStack product;
        int blockLevel = 0;
        if (this.getBlockState().getBlock() instanceof IronGeneratorBlock generatorBlock) {
            blockLevel = generatorBlock.getLevel();
        }

        if (blockLevel == 0) {
            product = new ItemStack(Items.IRON_INGOT, 1);
        } else if (blockLevel == 1) {
            product = new ItemStack(Items.IRON_INGOT, 9);
        } else {
            // Lv 2以上は、(Lv-1)段階圧縮鉄を9個出す
            // COMPRESSED_IRONSのIndex 0 が1段階圧縮
            int ironLevel = blockLevel - 2;
            product = new ItemStack(ModItems.COMPRESSED_IRONS.get(ironLevel).get(), 9);
        }
        
        // 上のブロックにインベントリがあれば入れる
        BlockPos upPos = pos.above();
        BlockEntity upEntity = level.getBlockEntity(upPos);
        AtomicBoolean inserted = new AtomicBoolean(false);

        if (upEntity != null) {
            final ItemStack toInsert = product.copy();
            upEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).ifPresent(handler -> {
                for (int i = 0; i < handler.getSlots(); i++) {
                    ItemStack remainder = handler.insertItem(i, toInsert, false);
                    toInsert.setCount(remainder.getCount());
                    if (toInsert.isEmpty()) {
                        inserted.set(true);
                        break;
                    }
                }
            });
        }

        // 入らなかった、または上にインベントリがない場合はドロップ
        if (!inserted.get()) {
            ItemEntity itemEntity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, product);
            itemEntity.setDeltaMovement(0, 0, 0.05); // 少し動いて重なりを防ぐ
            level.addFreshEntity(itemEntity);
        }
    }
}
