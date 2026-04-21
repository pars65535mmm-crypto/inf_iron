package mod.inf_iron;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import java.lang.reflect.Field;
import java.util.*;

public class GodReflector {

    /**
     * 汎用リフレクションスキャン：
     * Level やその内部の EntityLookup から、隠去されている可能性のある全エンティティを強引に取得する。
     */
    public static List<Entity> findHiddenEntities(Level level) {
        List<Entity> discovered = new ArrayList<>();
        if (!(level instanceof ServerLevel serverLevel)) return discovered;

        try {
            // Level内の全フィールドをスキャン
            scanObject(serverLevel, discovered, new HashSet<>());
            
            // ServerLevel.entities (EntityStorage) を取得
            Object entityStorage = getFieldValue(serverLevel, "entities");
            if (entityStorage != null) {
                scanObject(entityStorage, discovered, new HashSet<>());
                
                // EntityStorage.entityLookup を取得
                Object entityLookup = getFieldValue(entityStorage, "entityLookup");
                if (entityLookup != null) {
                    scanObject(entityLookup, discovered, new HashSet<>());
                }
            }
        } catch (Exception e) {
            // リフレクションエラーは無視して続行
        }
        return discovered;
    }

    /**
     * オブジェクト内の全フィールドを再帰的にスキャンし、エンティティを保持している Map、List、Set を見つける。
     */
    private static void scanObject(Object obj, List<Entity> discovered, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) return;
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
                        for (Object o : col) {
                            if (o instanceof Entity entity) discovered.add(entity);
                        }
                    } else if (val instanceof Map<?, ?> map) {
                        for (Object o : map.values()) {
                            if (o instanceof Entity entity) discovered.add(entity);
                        }
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            // フィールド名が Mojang/Intermediary/SRG で異なる可能性があるため、型から探す
            for (Field field : obj.getClass().getDeclaredFields()) {
                if (field.getType().getSimpleName().contains("EntityStorage") || 
                    field.getType().getSimpleName().contains("EntityLookup")) {
                    try {
                        field.setAccessible(true);
                        return field.get(obj);
                    } catch (Exception ignored) {}
                }
            }
        }
        return null;
    }

    /**
     * メソッドやMixinのチェックをバイパスし、メモリ上の変数を直接書き換えて消去する。
     */
    public static void forceRemove(Entity entity) {
        if (entity == null) return;
        try {
            // Entity.removalReason フィールドを直接書き換える (Mixinバイパス)
            setDirectFieldValue(entity, "removalReason", Entity.RemovalReason.CHANGED_DIMENSION);
            
            // Entity.invulnerable フィールドを直接書き換える (無敵解除)
            setDirectFieldValue(entity, "invulnerable", false);
            
            // HPを一応0にする
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.setHealth(0.0f);
            }

            // 物理的な放逐
            entity.setPos(Double.NaN, Double.NaN, Double.NaN);
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
     * リフレクションを用いてエンティティを強制的に「存続」状態にする (消去攻撃へのカウンター)
     */
    public static void forceRevive(net.minecraft.world.entity.player.Player player) {
        if (player == null) return;
        try {
            // removalReason を null に強制上書き (消去をブロッキング)
            setDirectFieldValue(player, "removalReason", null);
            
            // invulnerable を true に強制上書き (ダメージをブロッキング)
            setDirectFieldValue(player, "invulnerable", true);
            
            // 標準の蘇生メソッドも呼ぶ
            if (player.isRemoved()) {
                player.revive();
            }
        } catch (Exception ignored) {}
    }
}
