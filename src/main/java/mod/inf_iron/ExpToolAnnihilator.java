package mod.inf_iron;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ExpTool 専用の「どんな mob でも消し飛ばす」実行エンジン。
 */
public final class ExpToolAnnihilator {

    private ExpToolAnnihilator() {}

    public static void annihilateSingle(Player attacker, Entity target) {
        if (target == null || target == attacker || attacker.level().isClientSide) {
            return;
        }
        runFullPipeline(attacker, target);
    }

    public static void annihilateAoE(Player attacker, Level level, double radius) {
        if (level.isClientSide) return;
        AABB area = attacker.getBoundingBox().inflate(radius);
        List<Entity> targets = new ArrayList<>(level.getEntities((Entity) null, area, e -> e != attacker));
        Set<Entity> all = new HashSet<>(targets);
        all.addAll(GodReflector.findHiddenEntities(level));
        for (Entity e : all) {
            if (e == null || e == attacker) continue;
            if (e.distanceToSqr(attacker) > radius * radius) continue;
            runFullPipeline(attacker, e);
        }
    }

    public static void statusEffectOverflow(LivingEntity target) {
        if (target == null || target.level().isClientSide) return;
        ExpToolExecutionContext.markForAnnihilation(target);
        int tick = 6000;
        int amp = 255;
        for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
            if (effect == MobEffects.HARM) continue;
            try {
                target.addEffect(new MobEffectInstance(effect, tick, amp, false, false, false));
            } catch (Exception ignored) {}
        }
        for (int i = 0; i < 64; i++) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, tick, amp, false, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tick, amp, false, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, tick, amp, false, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, tick, amp, false, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, tick, amp, false, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.POISON, tick, amp, false, false, false));
        }
        corruptFieldsWithNoise(target);
    }

    public static void spacetimeAnnihilate(Player attacker, LivingEntity target) {
        if (target == null || target == attacker) return;

        // 1. 【スレ主の修正案】時空をロックする前に、まず登録リスト・メモリ・世界から完全にパージする
        godKillerWipe(target);
        removeFromEntitySections(target);
        forceRemoveBypass(target);

        // 2. 座標を世界の果て（NaN/虚無）へ送り飛ばして、あらゆるシステムTickから完全に隔離
        target.setPos(Double.NaN, Double.NaN, Double.NaN);

        // 3. その上で、残った魂や残像（パケット）を時空凍結で完全停止させる
        ExpToolSpacetimeManager.lockSpacetime(target);
        try {
            forceHealthZero(target);
            statusEffectOverflow(target);
            stripEquipment(target);
            bypassHurtKill(attacker, target);

            ExpToolExecutionContext.runAllowRemove(() -> {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
            });
        } finally {
            ExpToolSpacetimeManager.unlock(target);
            ExpToolExecutionContext.runAllowRemove(() -> {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
                GodReflector.forceRemove(target);
            });
            ExpToolExecutionContext.unmark(target);
        }
    }

    private static void runFullPipeline(Player attacker, Entity target) {
        ExpToolExecutionContext.markForAnnihilation(target);

        // 1. 相手が生存偽装をする前に、まず世界の登録セクション・Lookupから強制削除（先手必勝）
        godKillerWipe(target);
        removeFromEntitySections(target);

        if (target instanceof LivingEntity living) {
            stripEquipment(living);
            forceHealthZero(living);
            statusEffectOverflow(living);
            bypassHurtKill(attacker, living);
        }

        // 2. 最後に残存データを完全に虚無（NaN）へバイパスして物理破壊
        forceRemoveBypass(target);

        ExpToolExecutionContext.runAllowRemove(() -> {
            for (int i = 0; i < 3; i++) {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
            }
        });

        // ターゲットの座標を完全に消滅させ、システムに「存在しない」と確定させる
        target.setPos(Double.NaN, Double.NaN, Double.NaN);

        ExpToolExecutionContext.unmark(target);
    }

    public static void godKillerWipe(Entity target) {
        try {
            target.load(new CompoundTag());
            GodReflector.forceRemove(target);
            target.discard();
        } catch (Exception e) {
            target.discard();
        }
    }

    private static void stripEquipment(LivingEntity target) {
        if (target instanceof Player tp) {
            tp.getInventory().clearContent();
        } else {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                target.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        target.invulnerableTime = 0;
        GodReflector.setDirectFieldValue(target, "invulnerable", false);
    }

    private static void forceHealthZero(LivingEntity living) {
        if (living == null) return;

        ExpToolExecutionContext.forceAllowSetHealth(living.getUUID(), () -> {
            GodReflector.setDirectFieldValue(living, "health", 0.0F);
            GodReflector.setDirectFieldValue(living, "maxHealth", 0.0F);

            try {
                Object data = GodReflector.getFieldValue(living, "entityData");
                if (data == null) data = GodReflector.getFieldValue(living, "data");
                if (data != null) {
                    GodReflector.setDirectFieldValue(data, "items", new ArrayList<>());
                }
            } catch (Exception ignored) {}

            living.setHealth(0.0F);
            living.invulnerableTime = 0;
            living.deathTime = 20;
        });

        for (int i = 0; i < 3; i++) {
            living.hurt(living.level().damageSources().generic(), Float.MAX_VALUE);
        }
    }

    private static void bypassHurtKill(Player attacker, LivingEntity living) {
        if (living == null) return;
        living.invulnerableTime = 0;
        living.hurt(living.level().damageSources().generic(), Float.MAX_VALUE);
        if (attacker != null) {
            living.hurt(living.level().damageSources().playerAttack(attacker), Float.MAX_VALUE);
        }
        if (!living.isDeadOrDying()) {
            living.die(living.level().damageSources().generic());
        }
    }

    private static void forceRemoveBypass(Entity entity) {
        if (entity == null) return;

        GodReflector.forceRemove(entity);

        try {
            GodReflector.setDirectFieldValue(entity, "removed", true);
            GodReflector.setDirectFieldValue(entity, "removalReason", Entity.RemovalReason.DISCARDED);
            GodReflector.setDirectFieldValue(entity, "isAlive", false);
        } catch (Exception ignored) {}

        entity.setPos(Double.NaN, Double.NaN, Double.NaN);
        entity.setDeltaMovement(0, 0, 0);
        entity.hasImpulse = false;
        entity.setNoGravity(true);
    }

    public static void removeFromEntitySections(Entity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        try {
            Object storage = GodReflectorFindHidden.getFieldValue(serverLevel, "entities");
            if (storage == null) return;

            purgeFromSectionStorage(storage, entity);
            Object lookup = GodReflectorFindHidden.getFieldValue(storage, "entityLookup");
            if (lookup != null) {
                purgeEntityFromLookup(lookup, entity);
            }
            orphanEntitySection(storage, entity);
        } catch (Exception ignored) {}
    }

    private static void purgeFromSectionStorage(Object storage, Entity entity) {
        for (Field field : storage.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object val = field.get(storage);
                if (val == null) continue;
                String name = val.getClass().getSimpleName();
                if (name.contains("Section") || name.contains("Storage") || val instanceof Iterable<?>) {
                    removeEntityFromContainer(val, entity);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void purgeEntityFromLookup(Object lookup, Entity entity) {
        for (String methodName : new String[]{"remove", "delete", "untrack"}) {
            try {
                for (Method m : lookup.getClass().getDeclaredMethods()) {
                    if (!m.getName().equals(methodName)) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1 && Entity.class.isAssignableFrom(params[0])) {
                        m.setAccessible(true);
                        m.invoke(lookup, entity);
                    }
                }
            } catch (Exception ignored) {}
        }
        removeEntityFromContainer(lookup, entity);
    }

    private static void orphanEntitySection(Object storage, Entity entity) {
        try {
            for (Field field : storage.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object val = field.get(storage);
                if (val instanceof java.util.Map<?, ?> map) {
                    for (Object section : map.values()) {
                        if (section == null) continue;
                        removeEntityFromContainer(section, entity);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static void removeEntityFromContainer(Object container, Entity entity) {
        if (container instanceof java.util.Collection<?> col) {
            col.remove(entity);
            col.removeIf(o -> o instanceof Entity e && e.getId() == entity.getId());
        } else if (container instanceof java.util.Map<?, ?> map) {
            map.values().remove(entity);
            map.entrySet().removeIf(e -> e.getValue() instanceof Entity en && en.getId() == entity.getId());
        }
    }

    private static void corruptFieldsWithNoise(Object obj) {
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    if (field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        if (field.getType() == int.class) {
                            field.setInt(obj, field.getInt(obj) ^ 0x7FFFFFFF);
                        } else if (field.getType() == float.class) {
                            field.setFloat(obj, Float.NaN);
                        } else if (field.getType() == double.class) {
                            field.setDouble(obj, Double.NaN);
                        } else if (field.getType() == boolean.class) {
                            field.setBoolean(obj, !field.getBoolean(obj));
                        }
                    }
                } catch (Exception ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
    }

    private static final class GodReflectorFindHidden {
        static Object getFieldValue(Object obj, String fieldName) {
            try {
                Field field = obj.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception e) {
                for (Field field : obj.getClass().getDeclaredFields()) {
                    String simple = field.getType().getSimpleName();
                    if (simple.contains("EntityStorage") || simple.contains("EntityLookup")
                            || simple.contains("Section")) {
                        try {
                            field.setAccessible(true);
                            return field.get(obj);
                        } catch (Exception ignored) {}
                    }
                }
            }
            return null;
        }
    }

    // SpacetimeManagerから呼ぶための公開ラッパー
    public static void forceHealthZeroPublic(LivingEntity living) {
        forceHealthZero(living);
    }

    public static void bypassHurtKillPublic(Player attacker, LivingEntity living) {
        bypassHurtKill(attacker, living);
    }
}