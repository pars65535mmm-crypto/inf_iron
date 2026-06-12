package mod.inf_iron.mixin;

import mod.inf_iron.ExpToolExecutionContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 10000) // 優先度を限界突破させて先手必勝
public abstract class LivingEntityMixin {

    // 1. hurtのHEADをハイジャックして、相手の無敵ロジックが動く前に強制死亡トリガーを引く
    @Inject(method = "m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$absoluteKillAndDropBypass(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            // 内部呼び出しフラグを立てて、安全にHPを0にする
            ExpToolExecutionContext.runWithInternalSetHealth(() -> {
                self.setHealth(0.0F);
            });
            // 相手がdie()をキャンセルする前に、マイクラ純正の死亡・ドロップ処理を直接叩く
            self.die(source);
            
            // 相手のボスMOD側のhurt処理をこれ以上一切実行させずに、ここで完全終了
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    // 2. setHealthのフック。カッコの対応を修正し、外部（ボスMOD自身）からのHP書き換えのみを完全に拒否
    @Inject(method = "m_6030_(F)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$expToolSetHealth(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            if (!ExpToolExecutionContext.isInternalSetHealth()) {
                ci.cancel(); // 処刑実行中以外の、ボスによる生存偽装（HP回復）を一切拒否
            }
        }
    }

    // 3. getHealthのフック。システムがHPを参照した時、処刑対象なら常に0.0Fを返して「死んでいる」と誤認させる
    @Inject(method = "m_21223_()F", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$expToolGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            cir.setReturnValue(0.0F);
            cir.cancel();
        }
    }

    // 4. isAliveのフック。どんな生存偽装があろうが、システムに対して「こいつはもう死んでるで」と伝える
    @Inject(method = "m_6084_()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$absoluteIsAliveBypass(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    // 5. isDeadOrDyingのフック。強制的に死亡アニメーションやドロップ処理のコンテキストへ流す
    @Inject(method = "m_21224_()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$absoluteIsDeadBypass(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    // 6. 【最終奥義】相手が discard() や setRemoved() をハックして生存しようとする処理の「入り口(HEAD)」を完全封鎖
    // 相手の拒否コードが読まれる前に、リフレクションで内部の removalReason を直接 DISCARDED に書き換えて消滅させる
    @Inject(method = "m_142687_(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$forceDiscardBypass(net.minecraft.world.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            try {
                // 難読化環境（本番環境）用のフィールド名：f_140517_ (removalReason)
                java.lang.reflect.Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("f_140517_");
                field.setAccessible(true);
                field.set(self, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            } catch (Exception e) {
                try {
                    // 開発環境用のフィールド名
                    java.lang.reflect.Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("removalReason");
                    field.setAccessible(true);
                    field.set(self, net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                } catch (Exception ignored) {}
            }
            // ボスMOD側が仕込んでいるであろう、これ以降の「消滅を拒否するロジック」をすべてゴミ箱に捨てる
            ci.cancel();
        }
    }

    // 7. 虚空による戦闘ログクラッシュの防止
    @Inject(method = "m_6667_(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$preventVoidCombatTrackerCrash(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            if (source == null) {
                ci.cancel();
            }
        }
    }
}