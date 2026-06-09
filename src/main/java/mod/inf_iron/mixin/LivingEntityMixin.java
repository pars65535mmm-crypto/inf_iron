package mod.inf_iron.mixin;

import mod.inf_iron.ExpToolExecutionContext;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$expToolHurtBypass(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ExpToolExecutionContext.isAnnihilationTarget(self)) {
            return;
        }
        if (amount <= 0.0F) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "setHealth(F)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$expToolSetHealth(float health, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!ExpToolExecutionContext.isAnnihilationTarget(self)) {
            return;
        }
        if (ExpToolExecutionContext.isInternalSetHealth()) {
            return;
        }
        if (health > 0.0F) {
            ExpToolExecutionContext.forceAllowSetHealth(self.getUUID(), () -> self.setHealth(0.0F));
            ci.cancel();
        }
    }

    @Inject(method = "getHealth()F", at = @At("RETURN"), cancellable = true, remap = false)
    private void inf_iron$expToolGetHealth(CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
            cir.setReturnValue(0.0F);
        }
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "isAlive()Z", at = @At("HEAD"), cancellable = true, remap = false)
private void inf_iron$absoluteIsAliveBypass(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
        cir.setReturnValue(false); // どんな偽装があろうが「死んでいる」とシステムに誤認させる
        cir.cancel();
    }
}

@org.spongepowered.asm.mixin.injection.Inject(method = "isDeadOrDying()Z", at = @At("HEAD"), cancellable = true, remap = false)
private void inf_iron$absoluteIsDeadBypass(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
    LivingEntity self = (LivingEntity) (Object) this;
    if (ExpToolExecutionContext.isAnnihilationTarget(self)) {
        cir.setReturnValue(true); // 強制的に死亡状態として処理させる
        cir.cancel();
    }
}

@org.spongepowered.asm.mixin.injection.Inject(
    method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V",
    at = @At("HEAD"), 
    cancellable = true,
    remap = false // マッピングを通さず直接この名前を探す
)
private void inf_iron$preventVoidCombatTrackerCrash(net.minecraft.world.damagesource.DamageSource source, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
    // もしオリハルコンを装備したプレイヤー、またはExpToolの処理中であれば、
    // 攻撃者が消滅（Null）してログ処理がバグるのを防ぐため、死亡処理そのものを安全にバイパスするか、ログをクリアする
    if ((Object)this instanceof net.minecraft.server.level.ServerPlayer player) {
        // オリハルコンの防御が働いている間は、バニラのdie処理にログを触らせない
        if (player.isInvulnerable() || player.getHealth() > 0) {
            ci.cancel();
        }
    }
}
}


