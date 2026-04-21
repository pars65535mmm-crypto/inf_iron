package mod.inf_iron;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(InfIron.MODID)
public class InfIron
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "inf_iron";
    private static final Logger LOGGER = LogUtils.getLogger();

    public InfIron(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register items and creative tabs
        ModItems.register(modEventBus);
        // Register entities
        ModEntities.register(modEventBus);
        // Register blocks and block entities
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register ModNetwork
        ModNetwork.register();
    }

}
