package mod.inf_iron;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ZoltraakEntity extends Projectile {
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ZoltraakEntity.class,
            EntityDataSerializers.INT);
    private UUID targetUUID;
    private int age = 0;
    private static final double SPEED = 1.2D; // 24 blocks/sec = 1.2 blocks/tick

    public ZoltraakEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public ZoltraakEntity(Level level, LivingEntity owner, Vec3 initialVelocity) {
        super(ModEntities.ZOLTRAAK.get(), level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        this.setDeltaMovement(initialVelocity.scale(SPEED));
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(LIFETIME, 200);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.age > 200 || this.level().isClientSide && this.age > 200) {
            this.discard();
            return;
        }

        Vec3 pos = this.position();
        Vec3 vel = this.getDeltaMovement();

        // ホーミングロジック
        if (!this.level().isClientSide) {
            Entity target = findTarget();
            if (target != null) {
                Vec3 targetDir = target.getBoundingBox().getCenter().subtract(pos).normalize();
                // 緩やかに方向転換 (現在の速度とターゲット方向を混ぜる)
                vel = vel.scale(0.8).add(targetDir.scale(0.2)).normalize().scale(SPEED);
                this.setDeltaMovement(vel);
            }
        }

        // 移動
        Vec3 nextPos = pos.add(vel);
        this.setPos(nextPos.x, nextPos.y, nextPos.z);

        // パーティクル演出 (ゾルトラーク風：重厚な黒と紫)
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(ParticleTypes.SQUID_INK,
                        pos.x + (Math.random() - 0.5) * 0.2,
                        pos.y + (Math.random() - 0.5) * 0.2,
                        pos.z + (Math.random() - 0.5) * 0.2,
                        0, 0, 0);
                if (this.age % 2 == 0) {
                    this.level().addParticle(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 0, 0, 0);
                }
            }
        }

        // 当たり判定
        if (!this.level().isClientSide) {
            EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(this.level(), this, pos, nextPos,
                    this.getBoundingBox().expandTowards(vel).inflate(1.0D), this::canHitEntity);
            if (hitResult != null) {
                onHitEntity(hitResult);
            }
        }
    }

    private Entity findTarget() {
        List<Entity> enemies = this.level().getEntities(this.getOwner(), this.getBoundingBox().inflate(30.0),
                e -> e instanceof LivingEntity && e.isAlive() && e != this.getOwner());
        return enemies.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(this)))
                .orElse(null);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living && target != this.getOwner()) {
            target.hurt(this.level().damageSources().magic(), 500.0F);
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS, 0.5F, 1.2F);
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Age", this.age);
    }

    @Nonnull
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
