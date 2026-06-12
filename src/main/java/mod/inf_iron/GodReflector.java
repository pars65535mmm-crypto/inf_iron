package mod.inf_iron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class GodReflector {

    public static List<Entity> findHiddenEntities(Level level) {
        List<Entity> discovered = new ArrayList<>();
        if (!(level instanceof ServerLevel serverLevel)) return discovered;

        Set<Object> visited = new HashSet<>();
        scanObjectDeep(serverLevel, discovered, visited, 0);

        // EntityStorage を強めに掘る
        Object storage = getFieldByTypeOrName(serverLevel, "entities", "EntityStorage");
        if (storage != null) {
            scanObjectDeep(storage, discovered, visited, 0);
        }

        return discovered;
    }

    /**
     * より深く、再帰的にスキャン（深さ制限付きで無限ループ防止）
     */
    private static void scanObjectDeep(Object obj, List<Entity> discovered, Set<Object> visited, int depth) {
        if (obj == null || depth > 8 || visited.contains(obj)) return;
        visited.add(obj);

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object val = field.get(obj);
                    if (val == null) continue;

                    if (val instanceof Entity entity) {
                        discovered.add(entity);
                    } else if (val instanceof Collection<?> col) {
                        for (Object o : new ArrayList<>(col)) {  // コピーしてConcurrentModification対策
                            if (o instanceof Entity e) discovered.add(e);
                        }
                    } else if (val instanceof Map<?, ?> map) {
                        for (Object o : new ArrayList<>(map.values())) {
                            if (o instanceof Entity e) discovered.add(e);
                        }
                    } else if (depth < 6) {
                        // 深掘り
                        scanObjectDeep(val, discovered, visited, depth + 1);
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    /**
     * 超強力 forceRemove（全部盛り）
     */
    public static void forceRemove(Entity entity) {
        if (entity == null) return;

        try {
            // 1. 基本フラグ全殺し
            setDirectFieldValue(entity, "removalReason", Entity.RemovalReason.DISCARDED);
            setDirectFieldValue(entity, "invulnerable", false);
            setDirectFieldValue(entity, "removed", true);
            setDirectFieldValue(entity, "isAlive", false); // LivingEntity系

            entity.setDeltaMovement(0, 0, 0);
            entity.hasImpulse = false;
            entity.setNoGravity(true);

            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.setHealth(0);
                living.deathTime = 100;
                living.invulnerableTime = 0;
                living.hurt(living.level().damageSources().generic(), Float.MAX_VALUE);
            }

            // 2. Chunk からも強制除去
            if (entity.level() instanceof ServerLevel serverLevel) {
                LevelChunk chunk = serverLevel.getChunkAt(entity.blockPosition());
                removeFromChunk(chunk, entity);
            }

        } catch (Exception ignored) {}
    }

    private static void removeFromChunk(LevelChunk chunk, Entity entity) {
        try {
            Object entityList = getFieldByTypeOrName(chunk, null, "Entity");
            if (entityList instanceof Collection<?> col) {
                col.remove(entity);
            }
            // メソッド呼び出しも試す
            invokeMethod(chunk, "removeEntity", entity);
        } catch (Exception ignored) {}
    }

    public static void setDirectFieldValue(Object obj, String fieldName, Object value) {
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }
    }

    /**
     * 型名 or フィールド名で探す（Mojang mapping 対策）
     */
    private static Object getFieldByTypeOrName(Object obj, String name, String typeHint) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    if (name != null && field.getName().equals(name)) {
                        return field.get(obj);
                    }
                    if (typeHint != null && field.getType().getSimpleName().contains(typeHint)) {
                        return field.get(obj);
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void invokeMethod(Object obj, String methodName, Entity entity) {
        if (obj == null) return;
        for (Method m : obj.getClass().getDeclaredMethods()) {
            if (m.getName().contains(methodName) || m.getName().equalsIgnoreCase(methodName)) {
                try {
                    m.setAccessible(true);
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length >= 1 && Entity.class.isAssignableFrom(params[0])) {
                        m.invoke(obj, entity);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // プレイヤー復活用（必要なら）
    public static void forceRevive(net.minecraft.world.entity.player.Player player) {
        setDirectFieldValue(player, "removalReason", null);
        setDirectFieldValue(player, "invulnerable", true);
        player.revive();
    }

        /**
     * フィールド値取得（LivingEntity対応）
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        if (obj == null) return null;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (Exception ignored) {}
        }
        return null;
    }
}