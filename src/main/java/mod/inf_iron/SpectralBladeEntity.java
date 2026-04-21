package mod.inf_iron;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectralBladeEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SpectralBladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> BLADE_ID = SynchedEntityData.defineId(SpectralBladeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(SpectralBladeEntity.class, EntityDataSerializers.INT); // 0: Idle, 1: Attack, 2: Return, 3: Guard

    private LivingEntity target;
    private Projectile guardTarget;
    private int attackTicks = 0;
    private static final double MAX_SPEED = 3.0; // 秒速60ブロック (神速)

    public SpectralBladeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(BLADE_ID, 0);
        this.entityData.define(STATE, 0);
    }

    public void setOwner(Player player, int id) {
        this.entityData.set(OWNER_UUID, Optional.of(player.getUUID()));
        this.entityData.set(BLADE_ID, id);
    }

    public Player getOwner() {
        return this.entityData.get(OWNER_UUID).map(uuid -> this.level().getPlayerByUUID(uuid)).orElse(null);
    }

    @Override
    public void tick() {
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        super.tick();
        if (this.level().isClientSide) return;

        Player owner = getOwner();
        if (owner == null || !owner.isAlive() || !isCurioEquipped(owner)) {
            this.discard();
            return;
        }

        applySeparation();

        if (this.distanceTo(owner) > 40.0) {
            this.moveTo(owner.getX(), owner.getY() + 1.0, owner.getZ());
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // ガード優先ロジック
        if (this.tickCount % 2 == 0) {
             checkGuard(owner);
        }

        int state = this.entityData.get(STATE);
        if (state == 0) {
            handleIdle(owner);
            findTarget(owner);
        } else if (state == 1) {
            handleAttack(owner);
        } else if (state == 2) {
            handleReturn(owner);
        } else if (state == 3) {
            handleGuard(owner);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private void applySeparation() {
        List<SpectralBladeEntity> others = this.level().getEntitiesOfClass(SpectralBladeEntity.class, this.getBoundingBox().inflate(1.2));
        for (SpectralBladeEntity other : others) {
            if (other != this) {
                Vec3 diff = this.position().subtract(other.position());
                double dist = diff.length();
                if (dist < 0.8) {
                    this.setDeltaMovement(this.getDeltaMovement().add(diff.normalize().scale(0.1 / (dist + 0.1))));
                }
            }
        }
    }

    private void checkGuard(Player owner) {
        // プレイヤーの周囲にある飛び道具を探す
        List<Projectile> projectiles = this.level().getEntitiesOfClass(Projectile.class, owner.getBoundingBox().inflate(8.0),
            p -> p.getOwner() != owner && p.isAlive());
        
        if (!projectiles.isEmpty()) {
            // 最も近い飛び道具を取得
            Projectile p = projectiles.get(0);
            if (this.entityData.get(STATE) != 1) { // 攻撃中以外はガードへ移行
                this.guardTarget = p;
                this.entityData.set(STATE, 3);
            }
        }
    }

    private boolean isCurioEquipped(Player owner) {
        return CuriosApi.getCuriosHelper().findFirstCurio(owner, stack -> stack.is(ModItems.VERITAS_CURIOS.get())).isPresent();
    }

    private void handleIdle(Player owner) {
        int id = this.entityData.get(BLADE_ID);
        double fanAngle = Math.toRadians((id - 5.5) * 15);
        double timeOffset = Math.sin(this.tickCount * 0.1 + id * 0.5) * 0.3;
        
        Vec3 offset = new Vec3(Math.sin(fanAngle), 0.5 + timeOffset, -2.5 - Math.abs(id - 5.5) * 0.2);
        offset = offset.yRot((float) Math.toRadians(-owner.getYRot()));
        
        Vec3 targetPos = owner.position().add(offset);
        Vec3 moveVec = targetPos.subtract(this.position());
        
        this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(moveVec.scale(0.15)));
        limitSpeed();
        
        smoothRotate(owner.getYRot(), owner.getXRot());
    }

    private void findTarget(Player owner) {
        if (this.tickCount % 5 == 0) {
            List<LivingEntity> enemies = this.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(30.0),
                e -> e != owner && e.isAlive() && !e.isAlliedTo(owner))
                .stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(owner)))
                .collect(Collectors.toList());

            int id = this.entityData.get(BLADE_ID);
            // 敵が自分以上のID分存在する場合のみ、その敵を担当する
            if (id < enemies.size()) {
                this.target = enemies.get(id);
                this.entityData.set(STATE, 1);
                this.attackTicks = 0;
            }
        }
    }

    private void handleAttack(Player owner) {
        if (target == null || !target.isAlive() || target.distanceTo(owner) > 50) {
            this.entityData.set(STATE, 2);
            return;
        }

        attackTicks++;
        int id = this.entityData.get(BLADE_ID);
        
        double angle = Math.toRadians(this.tickCount * 12 + id * 30);
        Vec3 targetOffset = new Vec3(Math.cos(angle) * 1.0, target.getBbHeight() * 0.5, Math.sin(angle) * 1.0);
        Vec3 targetPos = target.position().add(targetOffset);
        
        Vec3 dir = targetPos.subtract(this.position()).normalize();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.7).add(dir.scale(0.8)));
        limitSpeed();
        
        lookAtMovement();
        
        if (this.getBoundingBox().inflate(1.2).intersects(target.getBoundingBox())) {
            target.hurt(owner.damageSources().playerAttack(owner), 300.0f);
            target.invulnerableTime = 0;
            if (attackTicks > 15) {
                this.entityData.set(STATE, 2);
            }
        }
        
        if (attackTicks > 100) this.entityData.set(STATE, 2);
    }

    private void handleGuard(Player owner) {
        if (guardTarget == null || !guardTarget.isAlive() || guardTarget.distanceToSqr(owner) > 100) {
            this.entityData.set(STATE, 2);
            return;
        }

        Vec3 targetPos = guardTarget.position();
        Vec3 dir = targetPos.subtract(this.position()).normalize();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(dir.scale(1.5)));
        limitSpeed();
        
        lookAtMovement();

        // 飛び道具を撃墜
        if (this.getBoundingBox().inflate(1.5).intersects(guardTarget.getBoundingBox())) {
            guardTarget.discard();
            this.entityData.set(STATE, 2); // 帰還へ
        }
    }

    private void handleReturn(Player owner) {
        Vec3 backPos = owner.position().add(0, 1, 0);
        Vec3 moveVec = backPos.subtract(this.position());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.8).add(moveVec.normalize().scale(0.3)));
        limitSpeed();
        
        if (this.distanceTo(owner) < 3) {
            this.entityData.set(STATE, 0);
        }
    }

    private void lookAtMovement() {
        if (this.getDeltaMovement().lengthSqr() > 0.01) {
             Vec3 move = this.getDeltaMovement();
             float targetYRot = (float) Math.toDegrees(Math.atan2(-move.x, move.z));
             float targetXRot = (float) Math.toDegrees(Math.atan2(-move.y, Math.sqrt(move.x * move.x + move.z * move.z)));
             smoothRotate(targetYRot, targetXRot);
        }
    }

    private void smoothRotate(float targetY, float targetX) {
        this.setYRot(this.getYRot() + Mth.wrapDegrees(targetY - this.getYRot()) * 0.4f);
        this.setXRot(this.getXRot() + Mth.wrapDegrees(targetX - this.getXRot()) * 0.4f);
    }

    private void limitSpeed() {
        Vec3 vel = this.getDeltaMovement();
        if (vel.length() > MAX_SPEED) {
            this.setDeltaMovement(vel.normalize().scale(MAX_SPEED));
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
