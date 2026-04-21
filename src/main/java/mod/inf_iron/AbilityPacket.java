package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Supplier;

public class AbilityPacket {
    private final int abilityId; // 0=StopTime, 1=Teleport, 2=Dash, 3=Gravity

    public AbilityPacket(int abilityId) {
        this.abilityId = abilityId;
    }

    public AbilityPacket(FriendlyByteBuf buffer) {
        this.abilityId = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(this.abilityId);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && hasHaloEquipped(player)) {
                switch (this.abilityId) {
                    case 0: // Time Stop
                        HaloTimeStopManager.toggleTimeStop(player);
                        break;
                    case 1: // Teleport
                        performTeleport(player);
                        break;
                    case 2: // Inertia Dash
                        performDash(player);
                        break;
                    case 3: // Gravity Control
                        performGravity(player);
                        break;
                }
            }
        });
        context.setPacketHandled(true);
    }

    private boolean hasHaloEquipped(ServerPlayer player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.is(ModItems.HALO_CURIOS.get())).isPresent();
    }

    private void performTeleport(ServerPlayer player) {
        // 視線の先ブロックへ最大150ブロック先までワープ
        HitResult result = player.pick(150.0D, 0.0F, false);
        if (result.getType() == HitResult.Type.BLOCK) {
            Vec3 target = result.getLocation();
            
            // テレポート前のエフェクト
            player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            
            player.teleportTo(target.x, target.y + 1.0, target.z);
            
            // ターゲット地点に本物の雷を落とす（ただし自分にはダメージなし）
            net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.level());
            if (lightning != null) {
                lightning.moveTo(target.x, target.y, target.z);
                lightning.setVisualOnly(true); // 音と見た目だけ
                player.level().addFreshEntity(lightning);
            }
            
            player.level().playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b§l[DIVINE STEP] §r§fRelocated with the wrath of gods."), true);
        }
    }

    private void performDash(ServerPlayer player) {
        // 視線方向に圧倒的な速さで飛ぶ
        Vec3 look = player.getLookAngle();
        double speed = 10.0; // 極限速度
        player.setDeltaMovement(look.x * speed, look.y * speed, look.z * speed);
        player.hasImpulse = true;
        player.hurtMarked = true;
        
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 2.0f);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e§l[SONIC BOOM] §r§fBreaking the laws of physics."), true);
    }

    private void performGravity(ServerPlayer player) {
        // 周囲50ブロックの全モブをまず空高く打ち上げ、1秒後に叩きつける
        AABB box = player.getBoundingBox().inflate(50.0);
        List<Entity> entities = player.level().getEntitiesOfClass(Entity.class, box);
        for (Entity entity : entities) {
            if (entity != player && entity instanceof LivingEntity living) {
                // 打ち上げ
                living.setDeltaMovement(0, 4.0, 0);
                living.hasImpulse = true;
                
                // 叩きつけ予約（1秒後）
                player.level().getServer().tell(new net.minecraft.server.TickTask(player.level().getServer().getTickCount() + 20, () -> {
                    living.setDeltaMovement(0, -10.0, 0);
                    living.hasImpulse = true;
                    // 地面に激突した時の爆発エフェクト
                    player.level().playSound(null, living.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0f, 1.0f);
                }));
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 0.5f);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§5§l[GRAVITY COLLAPSE] §r§fThe weight of the world descends."), true);
    }
}
