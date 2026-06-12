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
import net.minecraft.client.multiplayer.ClientLevel;

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
        
        // クラッシュの原因になるエフェクト全付与ループは完全廃止！
        // 代わりにバニラの基本的な行動不能デバフだけを安全に付与
        int tick = 6000;
        int amp = 255;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, tick, amp, false, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, tick, amp, false, false, false));
        
        // 座標データを汚染しない安全なノイズ処理を実行
        corruptFieldsWithNoise(target);
    }
public static void spacetimeAnnihilate(Player attacker, LivingEntity target) {
        if (target == null || target == attacker) return;

        // 【最優先】Mixinのロック対象として完全登録
        ExpToolExecutionContext.markForAnnihilation(target);

        try {
            // 1. まず身ぐるみを剥ぎ、データを0にする
            stripEquipment(target);
            forceHealthZero(target);
            statusEffectOverflow(target);
            bypassHurtKill(attacker, target);

            ExpToolExecutionContext.runAllowRemove(() -> {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
            });
        } finally {
            // 2. 死亡イベントが走った瞬間に、サーバーの全名簿（異次元含む）から物理パージ
            godKillerWipe(target);
            removeFromEntitySections(target);
            forceRemoveBypass(target);

            ExpToolExecutionContext.runAllowRemove(() -> {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
                GodReflector.forceRemove(target);
            });
            
            // 全ディメンション（ハイパースペース等）から強制追放
            if (target.level().getServer() != null) {
                java.util.UUID targetUUID = target.getUUID();
                for (ServerLevel serverLevel : target.level().getServer().getAllLevels()) {
                    Entity dimensionTarget = serverLevel.getEntity(targetUUID);
                    if (dimensionTarget != null) {
                        serverLevel.getChunkSource().removeEntity(dimensionTarget);
                        removeFromEntitySections(dimensionTarget);
                    }
                }
            }

            // 3. 【クライアント幽霊化も同時に爆破】

            if (target.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            // クライアント世界（ClientLevel）に安全にキャストして、ID指定で名簿から直接パージ！
            clientLevel.removeEntity(target.getId(), Entity.RemovalReason.DISCARDED);
        } else {
            // サーバー側などの場合は、バニラ共通Entityインスタンスの機能で安全に消去
            target.remove(Entity.RemovalReason.DISCARDED);
            try {
                target.setRemoved(Entity.RemovalReason.DISCARDED);
                target.level().broadcastEntityEvent(target, (byte) 3); // 死亡エフェクト強制トリガー
            } catch (Exception ignored) {}
        }
        }
    }

    public static void runFullPipeline(Player attacker, Entity target) {
        if (target == null) return;
        wipeFromWorldRegistry(target);
        shutdownTargetTransformer(target);
        
        
        ExpToolExecutionContext.markForAnnihilation(target);

        // 1. 安全な座標のバックアップ（NaN汚染防止）
        double px = target.getX();
        double py = target.getY();
        double pz = target.getZ();
        if (Double.isNaN(px) || Double.isInfinite(px)) px = attacker.getX();
        if (Double.isNaN(py) || Double.isInfinite(py)) py = attacker.getY();
        if (Double.isNaN(pz) || Double.isInfinite(pz)) pz = attacker.getZ();
        target.setPos(px, py, pz);

        // 2. 【最優先】まず身ぐるみを剥ぎ、HPを内部データごと「0」に固定して一切の無敵化を叩き潰す
        if (target instanceof LivingEntity living) {
            stripEquipment(living);
            forceHealthZero(living);
            statusEffectOverflow(living);
            // 3. 逃げる隙を与える前に、バニラの死亡判定（die）を強制的に踏ませてドロップを確定させる
            bypassHurtKill(attacker, living);
        }

        // 4. 死亡イベントが完全に終わった【この段階】で、初めて世界の全名簿から物理削除に入る
        godKillerWipe(target);
        removeFromEntitySections(target);
        forceRemoveBypass(target);

        ExpToolExecutionContext.runAllowRemove(() -> {
            for (int i = 0; i < 3; i++) {
                target.discard();
                target.remove(Entity.RemovalReason.DISCARDED);
                target.kill();
            }
        });

        // 5. 【次元超越パージ】オーバーワールドだけでなく、マイクラサーバーに存在する「全ディメンション」の管理名簿からこのボスを抹殺する
        if (target.level().getServer() != null) {
            java.util.UUID targetUUID = target.getUUID();
            // サーバー内のすべての世界（ポケットディメンションやハイパースペース含む）をループ
            for (ServerLevel serverLevel : target.level().getServer().getAllLevels()) {
                Entity dimensionTarget = serverLevel.getEntity(targetUUID);
                if (dimensionTarget != null) {
                    // 見つけ次第、その世界のチャンクソースから物理追放
                    serverLevel.getChunkSource().removeEntity(dimensionTarget);
                    // 異次元側のセクションからも徹底的に参照を抹消
                    removeFromEntitySections(dimensionTarget);
                }
            }
        }
        

        if (target.level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            // クライアント世界（ClientLevel）に安全にキャストして、ID指定で名簿から直接パージ！
            clientLevel.removeEntity(target.getId(), Entity.RemovalReason.DISCARDED);
        } else {
            // サーバー側などの場合は、バニラ共通Entityインスタンスの機能で安全に消去
            target.remove(Entity.RemovalReason.DISCARDED);
            try {
                target.setRemoved(Entity.RemovalReason.DISCARDED);
                target.level().broadcastEntityEvent(target, (byte) 3); // 死亡エフェクト強制トリガー
            } catch (Exception ignored) {}
        }
    
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
            // 中間のMojangマッピング名、SRG名、難読化名、全てを網羅して強制書き込み
            for (String fieldName : new String[]{"removed", "f_19771_", "field_140511_c", "removalReason", "f_19773_"}) {
                try {
                    Field field = Entity.class.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if (field.getType() == boolean.class) {
                        field.setBoolean(entity, true); // 消滅フラグを強制「真」固定
                    } else if (field.getType() == Entity.RemovalReason.class) {
                        field.set(entity, Entity.RemovalReason.DISCARDED); // 消滅理由を強制上書き
                    }
                } catch (Exception ignored) {}
            }
            

            GodReflector.setDirectFieldValue(entity, "isAlive", false);
            GodReflector.setDirectFieldValue(entity, "deathTime", 20);
        } catch (Exception ignored) {}

        // 座標や移動を0にして完全に固定
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
                // 座標や移動に関連する、絶対にNaNにしてはいけないフィールド名を除外（セーフティ）
                String fName = field.getName().toLowerCase();
                if (fName.contains("position") || fName.contains("movement") || fName.contains("loc") 
                    || fName.contains("pos") || fName.contains("bb") || fName.contains("box") 
                    || fName.contains("field_140517_") || fName.contains("f_19794_") || fName.contains("f_19795_") || fName.contains("f_19796_")) {
                    continue; 
                }

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

    public static void forceHealthZeroPublic(LivingEntity living) {
        forceHealthZero(living);
    }

    public static void bypassHurtKillPublic(Player attacker, LivingEntity living) {
        bypassHurtKill(attacker, living);
    }

    private static void shutdownTargetTransformer(net.minecraft.world.entity.Entity target) {
        if (target == null) return;
        try {
            String targetTargetPackage = target.getClass().getPackageName();
            
            String[] parts = targetTargetPackage.split("\\.");
            String targetBase = parts.length > 2 ? parts[0] + "." + parts[1] + "." + parts[2] : targetTargetPackage;

            // バニラやForge、1自身のMOD（inf_iron）の場合は、絶対に巻き込まないよう即座に保護
            if (targetBase.startsWith("net.minecraft") || 
                targetBase.startsWith("net.minecraftforge") || 
                targetBase.startsWith("mod.inf_iron")) {
                return;
            }

            // 2. ModLauncherのプラグインマップを引きずり出す
            Field field = cpw.mods.modlauncher.Launcher.class.getDeclaredField("launchPlugins");
            field.setAccessible(true);
            Object pluginHandler = field.get(cpw.mods.modlauncher.Launcher.INSTANCE);
            
            Field pluginsField = pluginHandler.getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) pluginsField.get(pluginHandler);
            
            if (map != null) {
                java.util.List<String> keys = new java.util.ArrayList<>(map.keySet());
                for (String key : keys) {
                    Object pluginInstance = map.get(key);
                    if (pluginInstance == null) continue;

                    // プラグインの実体クラスが、殴ったターゲットと同じベースパッケージに属しているか動的チェック！
                    String pluginPackage = pluginInstance.getClass().getPackageName();
                    if (pluginPackage.startsWith(targetBase) || pluginInstance.getClass().getName().contains(targetBase)) {
                        // ターゲットのMODが仕込んだトランスフォーマーだけを「狙撃」して削除！！
                        map.remove(key);
                    }
                }
            }
        } catch (Exception ignored) {
            // 安全第一
        }
    }


    public static void wipeFromWorldRegistry(net.minecraft.world.entity.Entity target) {
        if (target == null) return;
        try {
            net.minecraft.world.level.Level level = target.level();
            
            // サーバー側の世界（ServerLevel）の場合の処理
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                // ServerLevel が持つエンティティ管理システム（EntityManager）をリフレクションで強奪
                Field entityManagerField = net.minecraft.server.level.ServerLevel.class.getDeclaredField("entityManager");
                entityManagerField.setAccessible(true);
                Object entityManager = entityManagerField.get(serverLevel);
                
                if (entityManager != null) {
                    // EntityManager の中にある「index（名簿の実体）」を取得
                    Field indexField = entityManager.getClass().getDeclaredField("index");
                    indexField.setAccessible(true);
                    Object transientEntityLookup = indexField.get(entityManager);
                    
                    if (transientEntityLookup != null) {
                        // 名簿から直接、殴ったターゲットのIDを完全消去（remove）
                        java.lang.reflect.Method removeMethod = transientEntityLookup.getClass().getDeclaredMethod("remove", net.minecraft.world.level.entity.EntityAccess.class);
                        removeMethod.setAccessible(true);
                        removeMethod.invoke(transientEntityLookup, target);
                    }
                }
            }
            
            // クライアント側の世界（ClientLevel）の場合の処理
            if (level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                // クライアント側の名簿システム（EntityStorage）を強奪
                Field entityStorageField = net.minecraft.client.multiplayer.ClientLevel.class.getDeclaredField("entityStorage");
                entityStorageField.setAccessible(true);
                Object entityStorage = entityStorageField.get(clientLevel);
                
                if (entityStorage != null) {
                    Field indexField = entityStorage.getClass().getDeclaredField("idx"); // マッピング名に応じて調整
                    indexField.setAccessible(true);
                    Object idMap = indexField.get(entityStorage);
                    

if (level instanceof net.minecraft.client.multiplayer.ClientLevel) {
                net.minecraft.client.multiplayer.ClientLevel cl = (net.minecraft.client.multiplayer.ClientLevel) level;
                cl.removeEntity(target.getId(), net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
                }
            }
            
            // 最終ダメ押し：バニラ標準のエンティティパージ関数も念のため同時発火
            target.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            
        } catch (Exception ignored) {
            // 他MODとの競合を避けるため、エラーは綺麗にスルー
        }
    }
}