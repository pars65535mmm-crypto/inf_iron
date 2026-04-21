package mod.inf_iron;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class AtheosSummonEntity extends Entity {
    private int ticksExisted = 0;
    private static final int MAX_TICKS = 100; // 5秒

    public AtheosSummonEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ticksExisted = tag.getInt("TicksExisted");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TicksExisted", this.ticksExisted);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticksExisted++;

        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();
            
            // 天候と時間を召喚中に変え始める
            serverLevel.setDayTime(18000); // 夜
            if (serverLevel.getLevelData() instanceof net.minecraft.world.level.storage.ServerLevelData serverLevelData) {
                serverLevelData.setRaining(true);
                serverLevelData.setThundering(true);
            }

            if (this.ticksExisted % 10 == 0) {
                // 落雷演出 (ダメージなしの雷を周囲に)
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightning != null) {
                    lightning.moveTo(this.getX() + (random.nextDouble() - 0.5) * 20, this.getY(), this.getZ() + (random.nextDouble() - 0.5) * 20);
                    lightning.setVisualOnly(true);
                    serverLevel.addFreshEntity(lightning);
                }
                serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 10.0f, 0.5f);
            }

            // 特殊パーティクル
            serverLevel.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 5, this.getZ(), 5, 2, 2, 2, 0.1);
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY(), this.getZ(), 20, 5, 0, 5, 0.2);

            if (this.ticksExisted >= MAX_TICKS) {
                // ボス召喚
                AtheosEntity atheos = ModEntities.ATHEOS.get().create(serverLevel);
                if (atheos != null) {
                    atheos.moveTo(this.getX(), this.getY() + 10, this.getZ(), 0, 0);
                    serverLevel.addFreshEntity(atheos);
                    serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 5.0f, 0.5f);
                }
                this.discard();
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
