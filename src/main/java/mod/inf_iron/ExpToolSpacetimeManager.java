package mod.inf_iron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExpTool: 対象の時空間を停止し、移動・AI・コマンド干渉を封じた上で処刑する。
 */
@Mod.EventBusSubscriber(modid = InfIron.MODID)
public final class ExpToolSpacetimeManager {

    private static final Set<UUID> LOCKED = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Vec3> SAVED_MOTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> SAVED_NO_GRAVITY = new ConcurrentHashMap<>();

    private ExpToolSpacetimeManager() {}

    public static boolean isLocked(Entity entity) {
        return entity != null && LOCKED.contains(entity.getUUID());
    }

    public static void lockSpacetime(LivingEntity target) {
        if (target == null) return;
        UUID id = target.getUUID();
        LOCKED.add(id);
        if (!SAVED_MOTION.containsKey(id)) {
            SAVED_MOTION.put(id, target.getDeltaMovement());
            SAVED_NO_GRAVITY.put(id, target.isNoGravity());
        }
        applyFreeze(target);
        ExpToolExecutionContext.markForAnnihilation(target);
    }

    public static void unlock(Entity entity) {
        if (entity == null) return;
        UUID id = entity.getUUID();
        LOCKED.remove(id);
        if (SAVED_NO_GRAVITY.containsKey(id)) {
            entity.setNoGravity(SAVED_NO_GRAVITY.remove(id));
        }
        if (SAVED_MOTION.containsKey(id)) {
            entity.setDeltaMovement(SAVED_MOTION.remove(id));
        }
    }

    private static void applyFreeze(Entity entity) {
        if (entity == null) return;

        entity.setDeltaMovement(Vec3.ZERO);
        entity.setNoGravity(true);
        entity.hasImpulse = false;

        if (entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255));
            living.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 250));
            living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));

            if (living instanceof Mob mob) {
                mob.getNavigation().stop();
                mob.setTarget(null);
            }

            living.invulnerableTime = 0;
            living.deathTime = 20;
            GodReflector.setDirectFieldValue(living, "health", 0.0F);
        }

        GodReflector.setDirectFieldValue(entity, "invulnerable", false);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (UUID id : new ArrayList<>(LOCKED)) {
            Entity entity = serverLevel.getEntity(id);
            if (entity == null || entity.isRemoved()) {
                cleanup(id);
                continue;
            }

            applyFreeze(entity);

            if (entity instanceof LivingEntity living) {
                ExpToolAnnihilator.forceHealthZeroPublic(living);
                ExpToolAnnihilator.bypassHurtKillPublic(null, living);
            }

            ExpToolAnnihilator.godKillerWipe(entity);
            ExpToolAnnihilator.removeFromEntitySections(entity);
        }
    }

    private static void cleanup(UUID id) {
        LOCKED.remove(id);
        SAVED_MOTION.remove(id);
        SAVED_NO_GRAVITY.remove(id);
    }
}