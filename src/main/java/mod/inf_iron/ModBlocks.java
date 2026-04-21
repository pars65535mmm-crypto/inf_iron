package mod.inf_iron;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, InfIron.MODID);

    public static final RegistryObject<Block> EXCALIPOOR_ALTAR = registerBlock("excalipoor_altar",
            () -> new ExcalipoorAltarBlock(BlockBehaviour.Properties.of()));

    public static final RegistryObject<Block> IRON_GENERATOR = registerBlock("iron_generator",
            () -> new IronGeneratorBlock(0, BlockBehaviour.Properties.of()));

    public static final RegistryObject<Block> TRANSCENDENTAL_CORE = registerBlock("transcendental_core",
            () -> new TranscendentalCoreBlock(BlockBehaviour.Properties.of()));

    // 1-9段階の圧縮鉄製造機
    public static final List<RegistryObject<Block>> COMPRESSED_IRON_GENERATORS = new java.util.ArrayList<>();

    static {
        for (int i = 1; i <= 9; i++) {
            final int level = i;
            COMPRESSED_IRON_GENERATORS.add(registerBlock("iron_generator_" + level,
                    () -> new IronGeneratorBlock(level, BlockBehaviour.Properties.of())));
        }
    }


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
