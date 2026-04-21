package mod.inf_iron;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public class AtheosEntity extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<Integer> ACTION_STATE = SynchedEntityData.defineId(AtheosEntity.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TRUE_HEALTH = SynchedEntityData.defineId(AtheosEntity.class,
            EntityDataSerializers.FLOAT);

    private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(Component.literal("Atheos"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

    public static final float MAX_TRUE_HEALTH = 53000000.0f; // 5300万に上方修正
    private int sequenceTicks = 0;
    private int attackTicks = 0;
    private int chargeTicks = 0;
    private int transformationTicks = 0;
    private boolean phase2Triggered = false;
    private Vec3 lastDir = Vec3.ZERO;
    private Vec3 lastTargetPos = Vec3.ZERO;

    public AtheosEntity(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.xpReward = 100000;
        if (!level.isClientSide) {
            this.setTrueHealth(MAX_TRUE_HEALTH);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000000.0D) // バニラHPを100万に設定して誤検知を防止
                .add(Attributes.FLYING_SPEED, 1.2D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.ATTACK_DAMAGE, 1000.0D)
                .add(Attributes.ARMOR, 200.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ACTION_STATE, 0);
        this.entityData.define(TRUE_HEALTH, MAX_TRUE_HEALTH);
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);

        if (attackTicks > 0) attackTicks--;

        if (!this.level().isClientSide) {
            float currentHealth = this.getTrueHealth();
            this.bossEvent.setProgress(Math.max(0.0f, currentHealth / MAX_TRUE_HEALTH));
            // バニラのHP表示をTRUE_HEALTHに同期。ただし上限は100万。
            super.setHealth(Math.min(currentHealth, 1000000.0f));

            handleEnvironment();

            if (!phase2Triggered && currentHealth < MAX_TRUE_HEALTH / 2) {
                startTransformation();
            }

            if (transformationTicks > 0) {
                handleTransformation();
                return;
            }

            Player target = this.level().getNearestPlayer(this, 128.0);
            
            // ボスバーの白黒動的演出
            this.bossEvent.setColor(this.tickCount % 10 < 5 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.BLUE);
            this.bossEvent.setName(DynamicTextHelper.getGradientText("ATHEOS - THE VOID GOD"));

            if (target != null) {
                handleMovement(target);
                if (phase2Triggered) applyDomainEffect();

                processBattleSequence(target);
            } else {
                this.entityData.set(ACTION_STATE, 0);
                this.sequenceTicks = 0;
            }
        }
    }

    private void handleEnvironment() {
        if (this.tickCount % 20 == 0) {
            ((ServerLevel) this.level()).setDayTime(18000);
            if (this.level().getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData serverLevelData) {
                serverLevelData.setRaining(true);
                serverLevelData.setThundering(true);
            }
        }
    }

    private void startTransformation() {
        phase2Triggered = true;
        transformationTicks = 200;
        this.entityData.set(ACTION_STATE, 5); // 専用ステート
        sendActionMessageToAll("§l【第二形態：星の終焉】");
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN,
                SoundSource.HOSTILE, 10.0f, 0.5f);
    }

    private void processBattleSequence(Player target) {
        int state = this.getActionState();
        if (state == 0) {
            if (sequenceTicks++ > (phase2Triggered ? 20 : 40)) {
                decideNextAttack(target);
            }
        } else {
            updateCurrentAttack(target);
        }
    }

    private void decideNextAttack(Player target) {
        int next;
        if (phase2Triggered && this.random.nextInt(4) == 0) {
            next = 10; // 神ノ怒
        } else {
            next = 1 + this.random.nextInt(9); // 1-9 (バグ修正: 6と9も含める)
        }
        
        this.entityData.set(ACTION_STATE, next);
        this.sequenceTicks = 0;
        
        // 技名表示と共通予兆音
        switch(next) {
            case 1 -> sendActionMessageToAll("§e§l黄金極光 (Hyperion Beam)");
            case 2 -> sendActionMessageToAll("§f§l虚空波 (Void Wave)");
            case 3 -> sendActionMessageToAll("§c§l星堕 (Meteor Fall)");
            case 4 -> sendActionMessageToAll("§b§l極星殺雨 (Platinum Rain)");
            case 5 -> sendActionMessageToAll("§5§l重力崩壊 (Gravity Wave)");
            case 6 -> sendActionMessageToAll("§8§l虚無の収束 (Void Collapse)");
            case 7 -> sendActionMessageToAll("§d§l時の支配 (Time Stop)");
            case 8 -> sendActionMessageToAll("§6§lなぎ払い重力線 (Sweep Beam)");
            case 9 -> sendActionMessageToAll("§f§l断罪の光柱 (Judgment Pillars)");
            case 10 -> sendActionMessageToAll("§4§l§n神ノ怒 (GOD'S WRATH)");
        }
        
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 5.0f, 0.5f);
        this.chargeTicks = 40; // 予兆期間(2秒)
    }

    private void sendActionMessageToAll(String message) {
        if (this.level() instanceof ServerLevel server) {
            for (ServerPlayer player : server.players()) {
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal(message)));
            }
        }
    }

    private void handleCharge(Player target) {
        chargeTicks--;
        int state = getActionState();
        
        // チャージ中のパーティクル
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 20, 2, 2, 2, 0.5);
            // 予兆としての細い線
            Vec3 dir = target.getEyePosition().subtract(this.position()).normalize();
            if (state == 1 || state == 8) { // 狙い撃ち系
                for (int i = 0; i < 60; i += 5) {
                    Vec3 p = this.position().add(dir.scale(i));
                    server.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
        }

        if (chargeTicks == 0) {
            // チャージ完了。攻撃開始
            this.attackTicks = 40; // 攻撃持続時間
        }
    }

    private void prepareRandomAttack() {
        int nextAction = 1 + this.random.nextInt(8); // 1-8
        this.entityData.set(ACTION_STATE, nextAction);

        if (nextAction == 1 || nextAction == 7 || nextAction == 8) {
            this.chargeTicks = 20; // 1秒の予兆
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 5.0f, 0.5f);
        } else if (nextAction == 7) {
            HaloTimeStopManager.toggleTimeStop(this);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.HOSTILE, 5.0f, 0.1f);
            this.attackTicks = 100; // 時の停止時間
        } else {
            this.attackTicks = 40;
        }
    }

    private void handleTransformation() {
        transformationTicks--;
        // 全方位レーザー乱射
        if (transformationTicks % 5 == 0) {
            for (int i = 0; i < 360; i += 30) {
                float angle = (float) Math.toRadians(i + (transformationTicks * 10));
                shootDirectionalBeam(new Vec3(Math.cos(angle), (Math.random() - 0.5), Math.sin(angle)));
            }
        }
        // 強力な回転
        this.setYRot(this.getYRot() + 20.0f);
        if (transformationTicks == 0) {
            this.entityData.set(ACTION_STATE, 0);
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    private void handleMovement(Player target) {
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double time = this.level().getGameTime() * 0.05;
        double orbitRadius = phase2Triggered ? 15.0 : 25.0;
        double targetY = target.getY() + 10.0 + Math.sin(time * 0.3) * 10.0;

        Vec3 targetPos = new Vec3(
                target.getX() + Math.sin(time) * orbitRadius,
                targetY,
                target.getZ() + Math.cos(time) * orbitRadius);

        Vec3 moveVec = targetPos.subtract(this.position());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.95).add(moveVec.normalize().scale(0.2)));
    }

    private void applyDomainEffect() {
        List<Player> players = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(50.0));
        for (Player p : players) {
            Vec3 toBoss = this.position().subtract(p.position()).normalize().scale(0.2);
            p.setDeltaMovement(p.getDeltaMovement().add(toBoss));
            if (this.tickCount % 10 == 0) {
                handleInfiniteDamage(p, 100000.0f);
            }
            if (this.level().isClientSide) {
                for (int i = 0; i < 5; i++) {
                    this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.getX() + (random.nextDouble() - 0.5) * 2,
                            p.getY() + 2, p.getZ() + (random.nextDouble() - 0.5) * 2, 0, 0.1, 0);
                }
            }
        }
    }

    private void handleInfiniteDamage(LivingEntity target, float damage) {
        if (target.deathTime > 0) return;
        
        target.invulnerableTime = 0;
        boolean killed = target.hurt(this.damageSources().magic(), damage);
        
        if (target instanceof Player p && p.isDeadOrDying()) {
            playDeathImpact(p);
        }
    }

    private void playDeathImpact(Player p) {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLASH, p.getX(), p.getEyeY(), p.getZ(), 50, 0.5, 0.5, 0.5, 0);
            server.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0f, 0.5f);
            server.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 5.0f, 0.1f);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (transformationTicks > 0)
            return false; // 変身中は無敵

        if (!this.level().isClientSide) {
            float newHealth = this.getTrueHealth() - amount;
            this.setTrueHealth(newHealth);

            ((net.minecraft.server.level.ServerLevel) this.level()).sendParticles(ParticleTypes.CRIT, this.getX(),
                    this.getY(), this.getZ(), 10, 1, 1, 1, 0.5);

            if (newHealth <= 0) {
                // 撃破時にトドメを刺したプレイヤーへアイテムと実績を授与
                if (source.getEntity() instanceof ServerPlayer sp) {
                    net.minecraft.world.item.ItemStack core = new net.minecraft.world.item.ItemStack(ModItems.OMNIVERSAL_CORE.get());
                    if (!sp.getInventory().add(core)) {
                        sp.drop(core, false);
                    }
                    
                    Advancement adv = sp.server.getAdvancements().getAdvancement(new ResourceLocation("inf_iron", "god_slayer"));
                    if (adv != null) {
                        sp.getAdvancements().award(adv, "killed_atheos");
                    }
                } else {
                    // 他のソースで死んだ場合は周囲のプレイヤーに配布
                    java.util.List<Player> nearPlayers = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(128.0));
                    for (Player p : nearPlayers) {
                        net.minecraft.world.item.ItemStack core = new net.minecraft.world.item.ItemStack(ModItems.OMNIVERSAL_CORE.get());
                        if (!p.getInventory().add(core)) {
                            p.drop(core, false);
                        }
                    }
                }
                
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 15.0f, true,
                        Level.ExplosionInteraction.TNT);
                this.discard();
            }
            return true;
        }
        return super.hurt(source, amount);
    }

    private void updateCurrentAttack(Player target) {
        int state = this.entityData.get(ACTION_STATE);
        
        if (chargeTicks > 0) {
            handleCommonCharge(target);
            return;
        }

        sequenceTicks++;
        switch (state) {
            case 1 -> { // Super Beam
                if (sequenceTicks < 60 && sequenceTicks % 2 == 0) shootSuperBeam(target);
                else if (sequenceTicks >= 60) finishAttack();
            }
            case 2 -> { // Golden Shards
                if (sequenceTicks < 60) spawnRandomShards(target, phase2Triggered ? 20 : 10);
                else finishAttack();
            }
            case 3 -> { // Meteor Fall (Star Fall)
                if (sequenceTicks < 80 && sequenceTicks % 10 == 0) spawnClusterMeteor(target);
                else if (sequenceTicks >= 80) finishAttack();
            }
            case 4 -> { // Platinum Rain
                if (sequenceTicks < 100) {
                    for(int i=0; i<10; i++) spawnRain(target);
                } else finishAttack();
            }
            case 5 -> { // Gravity Wave
                if (sequenceTicks == 1) performGravityWave();
                if (sequenceTicks >= 40) finishAttack();
            }
            case 6 -> { // Void Collapse (New)
                performVoidCollapse(target);
                if (sequenceTicks >= 60) finishAttack();
            }
            case 7 -> { // Time Stop
                if (sequenceTicks == 1) {
                    HaloTimeStopManager.toggleTimeStop(this);
                }
                performCircularBurst();
                if (sequenceTicks >= 100) finishAttack();
            }
            case 8 -> { // Sweep Beam
                if (sequenceTicks < 60) performSweepingBeam(target);
                else finishAttack();
            }
            case 9 -> { // Judgment Pillars (New)
                if (sequenceTicks < 60 && sequenceTicks % 5 == 0) spawnJudgmentPillar(target);
                else if (sequenceTicks >= 80) finishAttack();
            }
            case 10 -> { // God's Wrath
                handleGodsWrath(target);
            }
        }
    }

    private void handleCommonCharge(Player target) {
        chargeTicks--;
        if (this.level() instanceof ServerLevel server) {
            // 収束する光の演出
            for(int i=0; i<5; i++) {
                Vec3 p = this.position().add(new Vec3(random.nextDouble()-0.5, random.nextDouble()-0.5, random.nextDouble()-0.5).scale(10));
                Vec3 v = this.position().subtract(p).normalize().scale(0.5);
                server.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 0, v.x, v.y, v.z, 1.0);
            }
            
            // 攻撃先への赤い照準
            Vec3 dir = target.getEyePosition().subtract(this.position()).normalize();
            for (int i = 0; i < 100; i += 10) {
                Vec3 line = this.position().add(dir.scale(i));
                server.sendParticles(ParticleTypes.FLAME, line.x, line.y, line.z, 1, 0, 0, 0, 0);
            }
        }
        if (chargeTicks == 0) sequenceTicks = 0;
    }

    private void finishAttack() {
        this.entityData.set(ACTION_STATE, 0);
        this.sequenceTicks = 0;
    }

    private void handleGodsWrath(Player target) {
        if (sequenceTicks == 1) lastTargetPos = target.position();
        
        if (sequenceTicks < 60) {
            // 魔法陣と音の演出
            if (this.level() instanceof ServerLevel server) {
                // ホワイトアウト予兆：大量のFLASH
                server.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 10, 5, 5, 5, 0);
                
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = lastTargetPos.x + Math.cos(rad) * 10;
                    double z = lastTargetPos.z + Math.sin(rad) * 10;
                    server.sendParticles(ParticleTypes.ENCHANTED_HIT, x, lastTargetPos.y + 0.1, z, 5, 0, 0.5, 0, 0.1);
                    server.sendParticles(ParticleTypes.FLASH, x, lastTargetPos.y + 0.1, z, 1, 0, 0, 0, 0);
                }
                if (sequenceTicks % 10 == 0) {
                     server.playSound(null, lastTargetPos.x, lastTargetPos.y, lastTargetPos.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 10.0f, 0.1f * (sequenceTicks/10f));
                }
            }
        } else if (sequenceTicks == 60) {
            // 発動：極太光柱 ＆ ホワイトアウト
            if (this.level() instanceof ServerLevel server) {
                // 画面を真っ白にする「ホワイトアウト」をActionBarで擬似再現＋パーティクル
                sendActionMessageToAll("§f§l" + "█".repeat(100));
                for(int i=0; i<50; i++) {
                    server.sendParticles(ParticleTypes.FLASH, lastTargetPos.x + (random.nextDouble()-0.5)*20, lastTargetPos.y + random.nextDouble()*20, lastTargetPos.z + (random.nextDouble()-0.5)*20, 10, 1, 1, 1, 0);
                }

                for (int y = 0; y < 100; y += 2) {
                    server.sendParticles(ParticleTypes.SONIC_BOOM, lastTargetPos.x, lastTargetPos.y + y, lastTargetPos.z, 10, 5, 1, 5, 0.1);
                    server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, lastTargetPos.x, lastTargetPos.y + y, lastTargetPos.z, 5, 5, 1, 5, 0.1);
                }
                // 地形ごと吹き飛ばす爆発
                server.explode(this, null, null, lastTargetPos.x, lastTargetPos.y, lastTargetPos.z, 20.0f, true, Level.ExplosionInteraction.BLOCK);
                
                // 超広範囲ダメージ
                List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(lastTargetPos.subtract(15, 15, 15), lastTargetPos.add(15, 15, 15)));
                for(LivingEntity e : targets) {
                    if (e != this) handleInfiniteDamage(e, 1000000.0f);
                }
            }
        } else if (sequenceTicks > 100) {
            finishAttack();
        }
    }

    private void shootSuperBeam(Player target) {
        shootDirectionalBeam(target.getEyePosition().subtract(this.position()).normalize());
    }

    private void shootDirectionalBeam(Vec3 dir) {
        Vec3 start = this.position();
        for (int i = 0; i < 150; i++) {
            Vec3 point = start.add(dir.scale(i));
            if (this.level() instanceof ServerLevel server) {
                if (i % 2 == 0) {
                    server.sendParticles(ParticleTypes.SONIC_BOOM, point.x, point.y, point.z, 1, 0.5, 0.5, 0.5, 0);
                    server.sendParticles(ParticleTypes.FLASH, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
                List<LivingEntity> hit = server.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(point.subtract(3, 3, 3), point.add(3, 3, 3)));
                for (LivingEntity e : hit) {
                    if (e != this) handleInfiniteDamage(e, 50000.0f);
                }
            }
        }
        if (this.tickCount % 5 == 0) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.HOSTILE, 2.0f, 0.5f);
        }
    }

    private void performSweepingBeam(Player target) {
        if (attackTicks == 40) lastDir = target.position().subtract(this.position()).normalize();
        
        // なぎ払い：ターゲットの周囲をスキャンするようにビームを動かす
        double angle = (40 - attackTicks) * 2.0 - 40.0;
        Vec3 sweepDir = lastDir.yRot((float) Math.toRadians(angle));
        shootDirectionalBeam(sweepDir);
    }

    private void spawnRandomShards(Player target, int count) {
        // 脱・ゾルトラーク。黄金の破片を射出。
        for (int i = 0; i < count; i++) {
            Vec3 spread = new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5,
                    this.random.nextDouble() - 0.5).scale(0.5);
            Vec3 dir = target.position().subtract(this.position()).normalize().add(spread);
            // 黄金パーティクルで見える弾丸を模倣（Entityは重いのでパーティクル+AABBチェック）
            shootGildedShard(dir);
        }
    }

    private void shootGildedShard(Vec3 dir) {
        Vec3 start = this.position();
        if (this.level() instanceof ServerLevel server) {
            for (int i = 0; i < 40; i++) {
                Vec3 point = start.add(dir.scale(i));
                if (i % 2 == 0)
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                
                List<LivingEntity> hit = server.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(point.subtract(1, 1, 1), point.add(1, 1, 1)));
                for (LivingEntity e : hit) {
                    if (e != this) handleInfiniteDamage(e, 25000.0f);
                }
            }
        }
    }

    private void spawnClusterMeteor(Player target) {
        if (this.level() instanceof ServerLevel server) {
            Vec3 center = target.position().add((this.random.nextDouble() - 0.5) * 10, 50, (this.random.nextDouble() - 0.5) * 10);
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (random.nextDouble() < 0.4) {
                        Vec3 spawnPos = center.add(x, random.nextDouble() * 5, z);
                        FallingBlockEntity fallingBlock = FallingBlockEntity.fall(server, BlockPos.containing(spawnPos), 
                            random.nextBoolean() ? Blocks.IRON_BLOCK.defaultBlockState() : Blocks.GOLD_BLOCK.defaultBlockState());
                        fallingBlock.dropItem = false;
                        fallingBlock.setDeltaMovement(0, -3.5, 0);
                        server.addFreshEntity(fallingBlock);
                    }
                }
            }
            server.playSound(null, center.x, target.getY(), center.z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE, 5.0f, 0.5f);
        }
    }


    private void spawnRain(Player target) {
        // 青白い雷光の粒子を降らせる（極星殺雨）
        Vec3 spawnPos = target.position().add((this.random.nextDouble() - 0.5) * 60, 50,
                (this.random.nextDouble() - 0.5) * 60);
        if (this.level().isClientSide) {
            for (int i = 0; i < 10; i++) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, spawnPos.x, spawnPos.y - i * 2, spawnPos.z, 0,
                        -1, 0);
            }
        } else {
            // 見えない弾を発射してダメージ（黄金の弾幕。黒い弾ではない）
            List<LivingEntity> hit = this.level().getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(spawnPos.subtract(2, 50, 2), spawnPos.add(2, 0, 2)));
            for (LivingEntity e : hit)
                if (e != this) {
                    e.invulnerableTime = 0;
                    e.hurt(this.damageSources().magic(), 12000.0f);
                }
        }
    }

    private void performGravityWave() {
        List<Player> nearby = this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(80.0));
        for (Player p : nearby) {
            Vec3 push = p.position().subtract(this.position()).normalize().scale(20.0);
            p.setDeltaMovement(push.x, 5.0, push.z);
            p.hurt(this.damageSources().magic(), 20000.0f);
        }
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE, 10.0f, 0.1f);
    }

    private void performCircularBurst() {
        // yabakune?.java を参考にした円状爆発
        if (this.level().isClientSide) {
            for (int i = 0; i < 360; i += 5) {
                double rad = Math.toRadians(i);
                double dist = (this.tickCount % 20) * 2.5;
                double px = this.getX() + Math.cos(rad) * dist;
                double pz = this.getZ() + Math.sin(rad) * dist;
                this.level().addParticle(ParticleTypes.SONIC_BOOM, px, this.getY(), pz, 0, 0, 0);
                this.level().addParticle(ParticleTypes.GLOW, px, this.getY() + (this.random.nextDouble() - 0.5) * 10,
                        pz, 0, 0, 0);
            }
        } else {
            double dist = (this.tickCount % 20) * 2.5;
            List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(dist + 2));
            for (LivingEntity e : targets) {
                if (e != this && e.distanceTo(this) > dist - 2 && e.distanceTo(this) < dist + 2) {
                    e.invulnerableTime = 0;
                    e.hurt(this.damageSources().magic(), 15000.0f);
                }
            }
        }
    }

    private void performVoidCollapse(Player target) {
        if (this.level() instanceof ServerLevel server) {
            // 全プレイヤーを引き寄せる
            server.players().forEach(p -> {
                Vec3 toBoss = this.position().subtract(p.position()).normalize().scale(1.5);
                p.setDeltaMovement(p.getDeltaMovement().add(toBoss));
                p.hurtMarked = true;
                
                if (this.tickCount % 5 == 0) {
                    server.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                }
            });
            
            // ボス周囲にブラックホール演出
            server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 50, 2, 2, 2, 0);
            server.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
        }
    }

    private void spawnJudgmentPillar(Player target) {
        if (this.level() instanceof ServerLevel server) {
            Vec3 pos = target.position();
            for (int y = 0; y < 50; y += 2) {
                server.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + y, pos.z, 10, 0.5, 1, 0.5, 0.1);
                server.sendParticles(ParticleTypes.FLASH, pos.x, pos.y + y, pos.z, 1, 0, 0, 0, 0);
            }
            server.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0f, 1.5f);
            
            // ダメージ
            List<LivingEntity> hit = server.getEntitiesOfClass(LivingEntity.class, 
                new net.minecraft.world.phys.AABB(pos.subtract(2, 2, 2), pos.add(2, 50, 2)));
            for(LivingEntity e : hit) {
                if (e != this) handleInfiniteDamage(e, 40000.0f);
            }
        }
    }

    public float getTrueHealth() {
        return this.entityData.get(TRUE_HEALTH);
    }

    public void setTrueHealth(float health) {
        this.entityData.set(TRUE_HEALTH, health);
    }

    public int getActionState() {
        return this.entityData.get(ACTION_STATE);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("TrueHealth"))
            setTrueHealth(t.getFloat("TrueHealth"));
        if (t.contains("P2"))
            phase2Triggered = t.getBoolean("P2");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("TrueHealth", getTrueHealth());
        t.putBoolean("P2", phase2Triggered);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        this.bossEvent.addPlayer(p);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) {
        super.stopSeenByPlayer(p);
        this.bossEvent.removePlayer(p);
    }

    @Override
    public void setCustomName(Component n) {
        super.setCustomName(n);
        this.bossEvent.setName(this.getDisplayName());
    }
}