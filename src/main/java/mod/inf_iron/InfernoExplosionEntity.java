package mod.inf_iron;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class InfernoExplosionEntity extends Entity {
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public InfernoExplosionEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.noPhysics = true;
    }

    public void setOwner(@Nullable LivingEntity pOwner) {
        this.owner = pOwner;
        this.ownerUUID = pOwner == null ? null : pOwner.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUUID != null && this.level() instanceof ServerLevel) {
            Entity entity = ((ServerLevel)this.level()).getEntity(this.ownerUUID);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            }
        }
        return this.owner;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        int castTicks = 150; // 詠唱
        int maxTicks = 400; // 20秒間続く超連鎖爆発
        
        if (this.tickCount >= castTicks + maxTicks) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            ServerLevel server = (ServerLevel) this.level();
            
            // --- 詠唱フェーズ (0 ~ 150 tick) ---
            if (this.tickCount < castTicks) {
                // 赤い閃光と雷
                if (this.tickCount % 5 == 0) {
                    server.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 30, 10, 1, 10, 0.5);
                    server.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY(), this.getZ(), 10, 5, 1, 5, 1.0);
                }
                if (this.tickCount % 25 == 0) {
                    server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0f, 0.1f + (this.tickCount / (float)castTicks));
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX() + (this.random.nextDouble() - 0.5)*40, this.getY() + 20, this.getZ() + (this.random.nextDouble() - 0.5)*40, 20, 0, 10, 0, 1.0);
                }
                // 周囲にプレッシャー
                if (this.tickCount > 50 && this.tickCount % 2 == 0) { // 細かくFLASH
                    double rx = this.getX() + (this.random.nextDouble() - 0.5) * 50;
                    double ry = this.getY() + (this.random.nextDouble() - 0.5) * 20;
                    double rz = this.getZ() + (this.random.nextDouble() - 0.5) * 50;
                    server.sendParticles(ParticleTypes.FLASH, rx, ry, rz, 1, 0, 0, 0, 0);
                    server.playSound(null, rx, ry, rz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 5.0f, 0.5f);
                }
                return; // 詠唱中は爆発処理は行わない
            }

            // --- 発動フェーズ ---
            int activeTick = this.tickCount - castTicks;
            double progress = (double) activeTick / maxTicks;
            
            // 時間経過とともに円形の爆発半径が広がる [5.0 ~ 60.0]
            double currentRadius = 5.0 + progress * 55.0;

            // --- 1. キノコ雲の傘（連鎖爆発） ---
            if (activeTick % 2 == 0) { // ペース倍増
                // 円周上の複数地点で爆発
                int explosionsPerTick = 10;
                for (int i = 0; i < explosionsPerTick; i++) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double ex = this.getX() + Math.cos(angle) * currentRadius;
                    double ey = this.getY() + (this.random.nextDouble() - 0.5) * 5.0; // 地表這うように
                    double ez = this.getZ() + Math.sin(angle) * currentRadius;

                    server.explode(this.getOwner() == null ? this : this.getOwner(), null, null, ex, ey, ez, 10.0f, true, Level.ExplosionInteraction.TNT);
                }
                
                // ドーン！というすさまじい音
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 20.0f, 0.5f);
            }

            // --- 2. プラズマと熱による超範囲焼却ダメージ ---
            if (activeTick % 5 == 0) {
                AABB heatArea = this.getBoundingBox().inflate(currentRadius + 15.0, 80.0, currentRadius + 15.0);
                List<LivingEntity> entities = server.getEntitiesOfClass(LivingEntity.class, heatArea);
                for (LivingEntity e : entities) {
                    if (e != this.getOwner() && !e.isSpectator()) {
                        e.setSecondsOnFire(100);
                        e.hurt(server.damageSources().magic(), 1000000.0f); // 強力な熱ダメージ100万
                        // プラズマ落下演出 (大増量)
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY()+5, e.getZ(), 20, 1.0, 1.0, 1.0, 0.5);
                    }
                }
            }

            // --- 3. 絶望的なキノコ雲のパーティクル演出 ---
            // 雲の柱 (炎と煙が逆巻く)
            double pillarRadius = 8.0 + progress * 8.0;
            for(int i = 0; i < 60; i++) {
                double px = this.getX() + (this.random.nextDouble() - 0.5) * pillarRadius;
                double py = this.getY() + this.random.nextDouble() * 60.0 * progress; // 凄まじく伸びる
                double pz = this.getZ() + (this.random.nextDouble() - 0.5) * pillarRadius;
                server.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, px, py, pz, 1, 0.1, 0.5, 0.1, 0.1);
                server.sendParticles(ParticleTypes.FLAME, px, py, pz, 2, 0.1, 0.2, 0.1, 0.2);
            }

            // 雲の傘 (上空を覆い尽くす地獄のドーム)
            double canopyHeight = this.getY() + 50.0 + 30.0 * progress;
            for(int i = 0; i < 200; i++) { // 超膨大
                double angle = this.random.nextDouble() * Math.PI * 2;
                double r = this.random.nextDouble() * currentRadius * 1.5; // 広がり拡大
                double heightOffset = (this.random.nextDouble() - 0.5) * (r / 1.5); // ドームの厚み
                double px = this.getX() + Math.cos(angle) * r;
                double py = canopyHeight + heightOffset; 
                double pz = this.getZ() + Math.sin(angle) * r;

                // 黒々とした煙とマグマの雨
                server.sendParticles(ParticleTypes.LARGE_SMOKE, px, py, pz, 1, 0, 0, 0, 0.01);
                if (this.random.nextDouble() < 0.3) {
                    server.sendParticles(ParticleTypes.LAVA, px, py, pz, 1, 0, 0, 0, 0);
                    server.sendParticles(ParticleTypes.FLASH, px, py, pz, 1, 0, 0, 0, 0);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.hasUUID("Owner")) {
            this.ownerUUID = pCompound.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        if (this.ownerUUID != null) {
            pCompound.putUUID("Owner", this.ownerUUID);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
