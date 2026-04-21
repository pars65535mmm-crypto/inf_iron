package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class UltimaMagicCircleEntity extends Entity {
    public static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(UltimaMagicCircleEntity.class, EntityDataSerializers.INT);
    private UUID ownerUUID;
    private static final int MAX_AGE = 200; // 10秒の予兆

    public UltimaMagicCircleEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(AGE, 0);
    }

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        int age = this.entityData.get(AGE);
        this.entityData.set(AGE, age + 1);

        if (age >= MAX_AGE) {
            performUltima();
            this.discard();
        } else {
            // 予兆演出
            if (age % 20 == 0) {
                float progress = (float) age / MAX_AGE;
                sendActionMessageToAll("§5§lULTIMA CHARGING: " + (int)(progress * 100) + "%");
                
                // 収束する光
                if (this.level() instanceof ServerLevel server) {
                    server.playSound(null, this.getX(), this.getY(), this.getZ(),
                            net.minecraft.sounds.SoundEvents.BEACON_AMBIENT,
                            net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f + progress * 1.5f);
                }
            }
            
            // 地面の揺れ演出（パーティクル）
            if (age > MAX_AGE - 40) {
                 if (this.level() instanceof ServerLevel server) {
                     server.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(), this.getZ(), 5, 5, 2, 5, 0);
                 }
            }
        }
    }

    private void performUltima() {
        if (!(this.level() instanceof ServerLevel server)) return;

        // ホワイトアウト演出
        sendActionMessageToAll("§f§l" + "█".repeat(100));
        
        server.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                net.minecraft.sounds.SoundSource.PLAYERS, 20.0f, 0.1f);

        int radius = 64;
        BlockPos center = this.blockPosition();

        // ブロックの消滅（ボイド化）
        // 負荷を考え、64x64x64の範囲を走査
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distSq = x * x + y * y + z * z;
                    if (distSq <= radius * radius) {
                        BlockPos pos = center.offset(x, y, z);
                        // 岩盤も含めて空気にする（究極なので）
                        server.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // エンティティへのダメージ
        AABB area = new AABB(center).inflate(radius);
        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area);
        for (LivingEntity target : targets) {
            target.hurt(server.damageSources().magic(), 999999.0f);
            target.invulnerableTime = 0;
        }

        // 巨大な消滅の証（パーティクル）
        server.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(), this.getZ(), 100, 10, 10, 10, 0.1);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(), 500, 32, 32, 32, 0);
    }

    private void sendActionMessageToAll(String message) {
        if (this.level() instanceof ServerLevel server) {
            for (ServerPlayer player : server.players()) {
                player.connection.send(new ClientboundSetActionBarTextPacket(net.minecraft.network.chat.Component.literal(message)));
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        if (pCompound.contains("Owner")) this.ownerUUID = pCompound.getUUID("Owner");
        this.entityData.set(AGE, pCompound.getInt("Age"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        if (this.ownerUUID != null) pCompound.putUUID("Owner", this.ownerUUID);
        pCompound.putInt("Age", this.entityData.get(AGE));
    }

    @Override
    @Nonnull
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
