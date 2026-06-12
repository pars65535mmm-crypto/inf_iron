package mod.inf_iron.mixin;

import mod.inf_iron.ExpToolExecutionContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 優先度を限界突破（10000）させて、他のMODのMixinよりも絶対先に、強力に発火させる
@Mixin(value = LivingEntity.class, priority = 10000)
public class EntityMixin {

    // 相手が「discard()」の拒否・生存偽装を挟み込む余地をなくすため、
    // Entityクラスの setRemoved (m_142687_) 自体を、処刑対象の時だけ完全にハイジャック（Overwrite）する
    /**
     * @author tyamizumoti
     * @reason クソボスの消滅拒否プロトコルを完全に粉砕し、法的に世界から抹消するため
     */
// LivingEntityMixin.java の中に追加してね！
    // setRemoved (m_142687_) の入り口(HEAD)を完全に封鎖する
    @Inject(method = "m_142687_(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$forceDiscardBypass(net.minecraft.world.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            // 相手が「discardを拒否しようとした」場合でも、
            // こっちがHEADでそれを検知し、強制的に removalReason フィールドを上書きしてメソッドを終わらせる
            try {
                java.lang.reflect.Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("f_140517_"); // removalReason の難読化フィールド名
                field.setAccessible(true);
                field.set(self, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            } catch (Exception e) {
                // 開発環境用
                try {
                    java.lang.reflect.Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("removalReason");
                    field.setAccessible(true);
                    field.set(self, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                } catch (Exception ignored) {}
            }
            
            // 相手のクソボスMODが仕込んだ「cancel()」が書かれているであろう、これ以降のコードをすべて【ゴミ箱】に捨てる
            ci.cancel();
        }
    }
}