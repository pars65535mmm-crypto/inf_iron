package mod.inf_iron.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// ターゲットクラス（難読化回避のため文字列指定が安全）
@Mixin(targets = "net.minecraft.world.level.chunk.storage.EntityStorage", priority = 9999) // ★優先度を9999（最強）にして敵のMixinより先に動かす
public class EntityStorageMixin {

    // メソッドの開始直後（HEAD）に割り込み、相手に処理を渡さず強制キャンセル（cancellable = true）できるようにする
    @Inject(method = "beforeWrite", at = @At("HEAD"), cancellable = true)
    private void onBeforeWrite(Entity entity, CallbackInfoReturnable<CompoundTag> cir) {
        
        // 1のソースコードにある「ExpToolExecutionContext」を疑いなく参照
        // もし殴られて戸籍抹消リスト（markForAnnihilation）に入っているMobなら
        if (mod.inf_iron.ExpToolExecutionContext.isAnnihilationTarget(entity)) {
            
            // プレイヤーだけは絶対に巻き込まない防衛ライン
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                
                // 【超重要】後続の全Mixinやバニラの処理を完全に拒絶し、空のデータを返してセーブを終わらせる
                cir.setReturnValue(new CompoundTag()); 
            }
        }
    }
}