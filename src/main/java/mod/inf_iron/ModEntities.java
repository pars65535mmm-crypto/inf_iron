package mod.inf_iron;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            InfIron.MODID);

    public static final RegistryObject<EntityType<ZoltraakEntity>> ZOLTRAAK = ENTITIES.register("zoltraak",
            () -> EntityType.Builder.<ZoltraakEntity>of(ZoltraakEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .setTrackingRange(64)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("zoltraak"));

    public static final RegistryObject<EntityType<AtheosEntity>> ATHEOS = ENTITIES.register("atheos",
            () -> EntityType.Builder.of(AtheosEntity::new, MobCategory.MONSTER)
                    .sized(4.0F, 4.0F)
                    .clientTrackingRange(128)
                    .build("atheos"));

    public static final RegistryObject<EntityType<AtheosSummonEntity>> ATHEOS_SUMMON = ENTITIES.register("atheos_summon",
            () -> EntityType.Builder.<AtheosSummonEntity>of(AtheosSummonEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .build("atheos_summon"));

    public static final RegistryObject<EntityType<SpectralBladeEntity>> SPECTRAL_BLADE = ENTITIES.register("spectral_blade",
            () -> EntityType.Builder.<SpectralBladeEntity>of(SpectralBladeEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(64)
                    .build("spectral_blade"));

    public static final RegistryObject<EntityType<FissureMagicCircleEntity>> FISSURE_MAGIC_CIRCLE = ENTITIES.register("fissure_magic_circle",
            () -> EntityType.Builder.<FissureMagicCircleEntity>of(FissureMagicCircleEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(128)
                    .build("fissure_magic_circle"));

    public static final RegistryObject<EntityType<TornadoEntity>> TORNADO = ENTITIES.register("tornado",
            () -> EntityType.Builder.<TornadoEntity>of(TornadoEntity::new, MobCategory.MISC)
                    .sized(5.0F, 15.0F)
                    .clientTrackingRange(128)
                    .setUpdateInterval(1)
                    .build("tornado"));

    public static final RegistryObject<EntityType<InfernoExplosionEntity>> INFERNO_EXPLOSION = ENTITIES.register("inferno_explosion",
            () -> EntityType.Builder.<InfernoExplosionEntity>of(InfernoExplosionEntity::new, MobCategory.MISC)
                    .sized(5.0F, 5.0F)
                    .clientTrackingRange(128)
                    .setUpdateInterval(1)
                    .build("inferno_explosion"));

    public static final RegistryObject<EntityType<UltimaMagicCircleEntity>> ULTIMA_MAGIC_CIRCLE = ENTITIES.register("ultima_magic_circle",
            () -> EntityType.Builder.<UltimaMagicCircleEntity>of(UltimaMagicCircleEntity::new, MobCategory.MISC)
                    .sized(10.0F, 1.0F)
                    .clientTrackingRange(256)
                    .build("ultima_magic_circle"));

    public static final RegistryObject<EntityType<TranscendentalCoreEntity>> TRANSCENDENTAL_CORE_ENTITY = ENTITIES.register("transcendental_core_entity",
            () -> EntityType.Builder.<TranscendentalCoreEntity>of(TranscendentalCoreEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(256)
                    .setUpdateInterval(1)
                    .build("transcendental_core_entity"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
