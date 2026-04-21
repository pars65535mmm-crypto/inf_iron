package mod.inf_iron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = InfIron.MODID)
public class HaloTimeStopManager {
    // Stores which level is stopped and who triggered it
    private static final Map<Level, UUID> stoppedLevels = new HashMap<>();
    private static final Map<Level, Integer> stopTicksLeft = new HashMap<>();
    
    // Original entity states
    private static final Map<UUID, Vec3> originalMotions = new HashMap<>();
    private static final Map<UUID, Boolean> originalNoGravity = new HashMap<>();

    public static boolean isTimeStopped(Level level) {
        return stoppedLevels.containsKey(level);
    }

    public static boolean isOwner(Entity entity) {
        if (entity == null) return false;
        Level level = entity.level();
        return stoppedLevels.get(level) != null && stoppedLevels.get(level).equals(entity.getUUID());
    }

    public static void toggleTimeStop(LivingEntity owner) {
        Level level = owner.level();
        if (isTimeStopped(level) && isOwner(owner)) {
            // Resume
            resumeTime(level, owner instanceof Player p ? p : null);
        } else {
            // Stop time for 10 seconds (200 ticks)
            stoppedLevels.put(level, owner.getUUID());
            stopTicksLeft.put(level, 200);
            if (owner instanceof Player p) {
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("§c§l[THE WORLD] §r§fTime has stopped..."), true);
            }
            
            // Dramatic sound
            level.playSound(null, owner.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.0f, 0.1f);
        }
    }

    private static void resumeTime(Level level, Player player) {
        stoppedLevels.remove(level);
        stopTicksLeft.remove(level);
        
        if (player != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a§lTime resumes..."), true);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
        
        // Restore motion and gravity
        if (level instanceof ServerLevel serverLevel) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (originalNoGravity.containsKey(entity.getUUID())) {
                    entity.setNoGravity(originalNoGravity.get(entity.getUUID()));
                }
                if (originalMotions.containsKey(entity.getUUID())) {
                    entity.setDeltaMovement(originalMotions.get(entity.getUUID()));
                }
            }
        }
        originalMotions.clear();
        originalNoGravity.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Level level = event.level;
        if (isTimeStopped(level)) {
            int ticks = stopTicksLeft.getOrDefault(level, 0) - 1;
            if (ticks <= 0) {
                resumeTime(level, null);
                return;
            }
            stopTicksLeft.put(level, ticks);
            
            // Freeze all entities
            if (level instanceof ServerLevel serverLevel) {
                for (Entity entity : serverLevel.getAllEntities()) {
                    if (!isOwner(entity)) {
                        freezeEntity(entity);
                    }
                }
            }
        }
    }

    private static void freezeEntity(Entity entity) {
        UUID uuid = entity.getUUID();
        // Save state if first time
        if (!originalMotions.containsKey(uuid)) {
            originalMotions.put(uuid, entity.getDeltaMovement());
            originalNoGravity.put(uuid, entity.isNoGravity());
        }
        
        // Strip motion and gravity
        entity.setDeltaMovement(0, 0, 0);
        entity.setNoGravity(true);
        entity.hasImpulse = false;
        
        // Disable AI completely for mobs
        if (entity instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2, 255, false, false, false));
        }

        // Projectiles should stop their movement logic
        if (entity instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            projectile.setDeltaMovement(0,0,0);
        }
    }
}
