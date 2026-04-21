package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class TranscendentalCoreEntity extends Entity {
    private static final int MAX_TICKS = 100;
    private static final float MAX_RADIUS = 256.0f;

    public TranscendentalCoreEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();

        if (this.tickCount >= MAX_TICKS) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) this.level();

            float prevRadius = MAX_RADIUS * ((float) (this.tickCount - 1) / MAX_TICKS);
            float currRadius = MAX_RADIUS * ((float) this.tickCount / MAX_TICKS);

            if (prevRadius < 0) prevRadius = 0;

            int prInt = (int) Math.floor(prevRadius);
            int crInt = (int) Math.ceil(currRadius);

            BlockPos center = this.blockPosition();

            // Progressively remove blocks in the expanding shell
            for (int x = -crInt; x <= crInt; x++) {
                for (int y = -crInt; y <= crInt; y++) {
                    for (int z = -crInt; z <= crInt; z++) {
                        double distSq = x * x + y * y + z * z;
                        if (distSq >= prevRadius * prevRadius && distSq <= currRadius * currRadius) {
                            BlockPos targetPos = center.offset(x, y, z);
                            
                            // Don't go below bedrock layer 
                            if (targetPos.getY() >= serverLevel.getMinBuildHeight() && targetPos.getY() < serverLevel.getMaxBuildHeight()) {
                                BlockState state = serverLevel.getBlockState(targetPos);
                                if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                                    serverLevel.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 2 | 16); 
                                    // 2: send to clients, 16: no observer update (avoids insane lag)
                                }
                            }
                        }
                    }
                }
            }

            // At specific intervals, deal massive damage to everything within 256 radius
            if (this.tickCount % 10 == 0) { // Deal 10 bursts over 100 ticks
                AABB damageArea = this.getBoundingBox().inflate(MAX_RADIUS);
                List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, damageArea);
                for (LivingEntity e : entities) {
                    if (!e.isSpectator() && !e.hasCustomName()) { // Exclude named entities or perhaps don't? Prompt doesn't specify. Exclude spectator.
                        for(int i = 0; i < 10; i++) { // Approx 10 * 10 bursts = 100 times, close to 99
                            e.hurt(serverLevel.damageSources().magic(), Float.MAX_VALUE);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
