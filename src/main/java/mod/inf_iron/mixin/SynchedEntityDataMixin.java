package mod.inf_iron.mixin;

import mod.inf_iron.ExpToolExecutionContext;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataMixin {

    @Inject(method = "set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private <T> void inf_iron$expToolDataSet(EntityDataAccessor<T> accessor, T value, boolean force, CallbackInfo ci) {
        SynchedEntityData self = (SynchedEntityData) (Object) this;
        Entity owner = ((SynchedEntityDataAccessor) self).inf_iron$getEntity();
        if (!(owner instanceof LivingEntity living)) {
            return;
        }
        if (!ExpToolExecutionContext.isAnnihilationTarget(living)) {
            return;
        }
        EntityDataAccessor<Float> healthId = ((LivingEntityAccessor) living).inf_iron$healthId();
        if (accessor == healthId && value instanceof Float f && f > 0.0F
                && !ExpToolExecutionContext.isInternalSetHealth()) {
            ExpToolExecutionContext.forceAllowSetHealth(living.getUUID(), () -> self.set(healthId, 0.0F, true));
            ci.cancel();
        }
    }
}
