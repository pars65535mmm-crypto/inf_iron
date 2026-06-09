package mod.inf_iron;

import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExpTool による強制処刑対象の追跡。Mixin とイベントが参照する。
 */
public final class ExpToolExecutionContext {

    private static final Set<UUID> ANNIHILATION_TARGETS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> FORCED_INTERNAL_SET_HEALTH = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> INTERNAL_CALL = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> ALLOW_REMOVE = ThreadLocal.withInitial(() -> false);

    private ExpToolExecutionContext() {}

    public static boolean isAnnihilationTarget(Entity entity) {
        return entity != null && ANNIHILATION_TARGETS.contains(entity.getUUID());
    }

    public static void markForAnnihilation(Entity entity) {
        if (entity != null) {
            ANNIHILATION_TARGETS.add(entity.getUUID());
        }
    }

    public static void unmark(Entity entity) {
        if (entity != null) {
            ANNIHILATION_TARGETS.remove(entity.getUUID());
        }
    }

    public static Set<UUID> getAnnihilationTargetsView() {
        return Collections.unmodifiableSet(ANNIHILATION_TARGETS);
    }

    public static boolean isInternalSetHealth() {
        return Boolean.TRUE.equals(INTERNAL_CALL.get()) || !FORCED_INTERNAL_SET_HEALTH.isEmpty();
    }

    public static void runWithInternalSetHealth(Runnable action) {
        INTERNAL_CALL.set(true);
        try {
            action.run();
        } finally {
            INTERNAL_CALL.set(false);
        }
    }

    public static boolean isAllowRemove() {
        return Boolean.TRUE.equals(ALLOW_REMOVE.get());
    }

    public static void runAllowRemove(Runnable action) {
        ALLOW_REMOVE.set(true);
        try {
            action.run();
        } finally {
            ALLOW_REMOVE.set(false);
        }
    }

    public static void forceAllowSetHealth(UUID entityId, Runnable action) {
        FORCED_INTERNAL_SET_HEALTH.add(entityId);
        INTERNAL_CALL.set(true);
        try {
            action.run();
        } finally {
            FORCED_INTERNAL_SET_HEALTH.remove(entityId);
            INTERNAL_CALL.set(false);
        }
    }
}
