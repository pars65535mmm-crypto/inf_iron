package mod.inf_iron;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.stream.Stream;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, InfIron.MODID);

    public static final RegistryObject<BlockEntityType<IronGeneratorBlockEntity>> IRON_GENERATOR =
            BLOCK_ENTITIES.register("iron_generator", () ->
                    BlockEntityType.Builder.of(IronGeneratorBlockEntity::new,
                            Stream.concat(Stream.of(ModBlocks.IRON_GENERATOR.get()),
                                          ModBlocks.COMPRESSED_IRON_GENERATORS.stream().map(RegistryObject::get))
                                  .toArray(Block[]::new)).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
