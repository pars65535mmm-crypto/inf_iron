package mod.inf_iron;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class FissureMagicCircleEntity extends Entity {
    private static final EntityDataAccessor<Integer> RADIUS_TICK = SynchedEntityData.defineId(FissureMagicCircleEntity.class, EntityDataSerializers.INT);
    
    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public FissureMagicCircleEntity(EntityType<?> pEntityType, Level pLevel) {
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
        this.entityData.define(RADIUS_TICK, 0);
    }

    public int getRadiusTick() {
        return this.entityData.get(RADIUS_TICK);
    }

    @Override
    public void tick() {
        super.tick();

        int tick = this.tickCount;
        if (!this.level().isClientSide) {
            this.entityData.set(RADIUS_TICK, tick);
            ServerLevel server = (ServerLevel) this.level();

            // 0 - 200 tick: チャージフェーズ
            if (tick < 200) {
                // 中心にパーティクルを集積
                if (tick % 2 == 0) {
                    server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 20, 3, 3, 3, 0.5);
                    server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 10, 5, 5, 5, 0.1);
                }

                // 少しずつ音を鳴らす
                if (tick % 20 == 0) {
                    server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 2.0f, 0.5f + (tick / 200.0f));
                    server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 3.0f, 1.0f);
                }

                // 他にも色々な「小さな魔法陣」的なものを周囲に出現させる演出
                if (tick > 30 && tick % 5 == 0) {
                    double rx = this.getX() + (this.random.nextDouble() - 0.5) * 40;
                    double ry = this.getY() + (this.random.nextDouble() - 0.5) * 30 - 5;
                    double rz = this.getZ() + (this.random.nextDouble() - 0.5) * 40;
                    
                    server.sendParticles(ParticleTypes.FLASH, rx, ry, rz, 1, 0, 0, 0, 0);
                    for (int i = 0; i < 360; i += 30) {
                        double rad = Math.toRadians(i);
                        server.sendParticles(ParticleTypes.FLAME, rx + Math.cos(rad) * 2, ry, rz + Math.sin(rad) * 2, 1, 0, 0, 0, 0);
                        server.sendParticles(ParticleTypes.SOUL, rx + Math.cos(rad) * 1, ry, rz + Math.sin(rad) * 1, 1, 0, 0, 0, 0);
                    }
                    server.playSound(null, rx, ry, rz, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 2.0f, 1.2f);
                }
                
                // 緊迫感のための雷パーティクルと、後半の連続小爆発
                if (tick > 60) {
                    server.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX() + (this.random.nextDouble() - 0.5) * 50, this.getY() - 10 + (this.random.nextDouble() - 0.5) * 30, this.getZ() + (this.random.nextDouble() - 0.5) * 50, 5, 0, 10, 0, 1.0);
                }
                
                // 100tick以降：周囲に小爆発をバチバチ発生させ、プレッシャーとダメージをばら撒く
                if (tick > 100 && tick % 2 == 0) {
                    double bx = this.getX() + (this.random.nextDouble() - 0.5) * 60;
                    double by = this.getY() - 15 + (this.random.nextDouble() * 20); // 地上寄りに
                    double bz = this.getZ() + (this.random.nextDouble() - 0.5) * 60;
                    
                    server.explode(this.getOwner() == null ? this : this.getOwner(), null, null, bx, by, bz, 3.0f, true, Level.ExplosionInteraction.BLOCK); // 小爆発でも地形破壊
                    
                    List<LivingEntity> hit = server.getEntitiesOfClass(LivingEntity.class, new AABB(bx-5, by-5, bz-5, bx+5, by+5, bz+5));
                    for (LivingEntity e : hit) {
                        if (e != this.getOwner()) {
                            e.hurt(server.damageSources().magic(), 10000.0f); // 小爆発の直撃ダメージ
                        }
                    }
                }

                // 吸い込み効果 (130tick付近から)
                if (tick > 130) {
                    List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(50.0));
                    for (LivingEntity e : targets) {
                        if (e != this.getOwner()) {
                            net.minecraft.world.phys.Vec3 toCenter = this.position().subtract(e.position()).normalize().scale(0.8);
                            e.setDeltaMovement(e.getDeltaMovement().add(toCenter));
                            e.hurtMarked = true;
                        }
                    }
                }

            } else if (tick == 200) {
                // ドカンと大爆発
                // 1. 強烈な音とFLASH
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 20.0f, 0.5f);
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 20.0f, 0.5f);
                for(int i = 0; i < 50; i++) {
                    server.sendParticles(ParticleTypes.FLASH, this.getX() + (this.random.nextDouble()-0.5)*50, this.getY() - 10 + (this.random.nextDouble()-0.5)*40, this.getZ() + (this.random.nextDouble()-0.5)*50, 10, 0, 0, 0, 0);
                    server.sendParticles(ParticleTypes.SONIC_BOOM, this.getX() + (this.random.nextDouble()-0.5)*50, this.getY() - 10 + (this.random.nextDouble()-0.5)*40, this.getZ() + (this.random.nextDouble()-0.5)*50, 1, 0, 0, 0, 0);
                }

                // 2. 直下への超大規模ダメージと地形破壊
                server.explode(this.getOwner() == null ? this : this.getOwner(), null, null, this.getX(), this.getY() - 15, this.getZ(), 40.0f, true, Level.ExplosionInteraction.TNT);

                // 3. 超範囲の即死級ダメージ
                AABB damageBox = this.getBoundingBox().inflate(60.0, 70.0, 60.0).move(0, -15, 0);
                List<LivingEntity> entities = server.getEntitiesOfClass(LivingEntity.class, damageBox);
                for (LivingEntity e : entities) {
                    if (e != this.getOwner()) {
                        e.invulnerableTime = 0;
                        e.hurt(server.damageSources().magic(), 5000000.0f); // 500万ダメージ
                        if (e instanceof Player targetPlayer) {
                            // 殺しきれなかった場合のために爆破で追加ダメージ
                            server.explode(this, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(), 10.0f, false, Level.ExplosionInteraction.NONE);
                        }
                    }
                }

                this.discard();
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
