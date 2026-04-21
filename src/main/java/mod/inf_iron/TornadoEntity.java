package mod.inf_iron;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class TornadoEntity extends Entity {

    @Nullable
    private LivingEntity owner;
    @Nullable
    private UUID ownerUUID;

    public TornadoEntity(EntityType<?> pEntityType, Level pLevel) {
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

        int castTicks = 150; // 詠唱: 7.5秒
        int durationTicks = 1200; // 発動持続: 1分
        if (this.tickCount >= castTicks + durationTicks) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            ServerLevel server = (ServerLevel) this.level();
            LivingEntity o = getOwner();

            // --- 詠唱フェーズ (0 ~ 150 tick) ---
            if (this.tickCount < castTicks) {
                // 中心にパーティクルを集積
                if (this.tickCount % 2 == 0) {
                    server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY(), this.getZ(), 20, 5, 2, 5, 0.5);
                    server.sendParticles(ParticleTypes.NAUTILUS, this.getX(), this.getY(), this.getZ(), 5, 2, 0.5, 2, 0.1);
                }
                if (this.tickCount % 20 == 0) {
                    server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_LOOP_ADDITIONS, SoundSource.PLAYERS, 2.0f, 0.5f + (this.tickCount / (float)castTicks));
                }
                // 周囲にプレッシャー
                if (this.tickCount > 50 && this.tickCount % 5 == 0) {
                    double rx = this.getX() + (this.random.nextDouble() - 0.5) * 30;
                    double ry = this.getY() + (this.random.nextDouble() - 0.5) * 10;
                    double rz = this.getZ() + (this.random.nextDouble() - 0.5) * 30;
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, rx, ry, rz, 1, 0, 0, 0, 0);
                    server.playSound(null, rx, ry, rz, SoundEvents.ILLUSIONER_PREPARE_MIRROR, SoundSource.PLAYERS, 2.0f, 1.2f);
                }
                return; // 詠唱中はこれ以降の破壊処理は行わない
            }

            // --- 発動フェーズ ---

            // オーナーへの追従 (速度制限つき：秒速3ブロック = 0.15ブロック/tick)
            if (o != null && o.isAlive()) {
                Vec3 targetPos = o.position();
                Vec3 diff = targetPos.subtract(this.position());
                double maxSpeed = 0.15; // 最高速: 0.15 block/tick
                
                if (diff.lengthSqr() > 0.01) {
                    if (diff.length() > maxSpeed) {
                        diff = diff.normalize().scale(maxSpeed);
                    }
                    this.setPos(this.getX() + diff.x, this.getY() + diff.y, this.getZ() + diff.z);
                }
            }

            double radius = 15.0; // 竜巻の影響半径 (巨大)
            double height = 50.0; // 竜巻の高さ

            // --- 1. エンティティの吸い込みと打ち上げ ---
            AABB area = this.getBoundingBox().inflate(radius, height, radius).move(0, height/2, 0);
            List<LivingEntity> entities = server.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity e : entities) {
                if (e != o && !e.isSpectator()) {
                    Vec3 toCenter = this.position().add(0, e.getY() - this.getY(), 0).subtract(e.position());
                    double dist = toCenter.length();
                    
                    // 中心へ向かう力と上へ向かう力
                    Vec3 force = toCenter.normalize().scale(0.3).add(0, 1.2, 0);
                    // 竜巻ぽく回転させる力
                    Vec3 tangential = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.5);
                    
                    e.setDeltaMovement(e.getDeltaMovement().add(force).add(tangential));
                    e.hurtMarked = true;
                    e.fallDistance = 0; // 落下ダメージリセット
                    
                    if (this.tickCount % 10 == 0) {
                        e.hurt(server.damageSources().magic(), 1000.0f); // 持続ダメージ
                    }
                }
            }

            // --- 2. 地形破壊とブロック巻き上げ ---
            // F5クラスなので周囲のブロックを空気に変えつつ、FallingBlockEntityを大量に飛ばす
            for(int i = 0; i < 20; i++) {
                double rx = this.getX() + (this.random.nextDouble() - 0.5) * radius * 2;
                double ry = this.getY() - 5.0 + this.random.nextDouble() * height; // 少し下から
                double rz = this.getZ() + (this.random.nextDouble() - 0.5) * radius * 2;
                
                BlockPos pos = BlockPos.containing(rx, ry, rz);
                BlockState state = server.getBlockState(pos);
                // 岩盤などは壊さない
                if (!state.isAir() && state.getDestroySpeed(server, pos) >= 0) {
                    server.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    
                    // 単に消すのではなく、ブロックを巻き上げる (FallingBlockEntity化)
                    net.minecraft.world.entity.item.FallingBlockEntity fallingBlock = net.minecraft.world.entity.item.FallingBlockEntity.fall(server, pos, state);
                    if (fallingBlock != null) {
                        // 竜巻に巻き込まれたような初期速度を与える
                        Vec3 toCenter = this.position().subtract(rx, ry, rz).normalize();
                        Vec3 tangential = new Vec3(-toCenter.z, 0, toCenter.x).normalize().scale(0.8);
                        fallingBlock.setDeltaMovement(toCenter.scale(0.3).add(0, 1.5, 0).add(tangential));
                    }

                    // 水飛沫や破片パーティクル
                    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, rx, ry, rz, 2, 0, 0, 0, 0.1);
                    server.sendParticles(ParticleTypes.SPLASH, rx, ry, rz, 5, 0.5, 0.5, 0.5, 0.2);
                }
            }
            
            // --- 3. 絶望的なパーティクル演出とサウンド ---
            if (this.tickCount % 20 == 0) {
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 10.0f, 0.5f);
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 5.0f, 0.1f);
                server.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 10.0f, 0.5f);
            }
            
            // F5を表現するため螺旋状の巨大な雲のエフェクトを送信
            int particlesToSpawn = 80; // 大量パーティクル
            for(int i = 0; i < particlesToSpawn; i++) {
                double h = this.random.nextDouble() * height;
                double currentRadius = 2.0 + (h / height) * radius; // 上に行くほど太くなる逆円錐
                double angle = this.tickCount * 0.5 + h * 0.1 + this.random.nextDouble() * Math.PI * 2;
                
                double px = this.getX() + Math.cos(angle) * currentRadius;
                double py = this.getY() + h;
                double pz = this.getZ() + Math.sin(angle) * currentRadius;
                
                server.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, px, py, pz, 1, 0.1, 0.1, 0.1, 0.05);
                server.sendParticles(ParticleTypes.CLOUD, px, py, pz, 1, 0, 0, 0, 0.02);
                
                if (this.random.nextDouble() < 0.1) {
                    server.sendParticles(ParticleTypes.SWEEP_ATTACK, px, py, pz, 1, 0, 0, 0, 0);
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
