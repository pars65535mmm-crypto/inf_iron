package mod.inf_iron.mixin;

import mod.inf_iron.ExpToolExecutionContext;
import mod.inf_iron.ExpToolSpacetimeManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void inf_iron$expToolRemoveGuard(Entity.RemovalReason reason, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (ExpToolExecutionContext.isAnnihilationTarget(self) && ExpToolSpacetimeManager.isLocked(self)
                && !ExpToolExecutionContext.isAllowRemove()) {
            ci.cancel();
        }
    }
}
