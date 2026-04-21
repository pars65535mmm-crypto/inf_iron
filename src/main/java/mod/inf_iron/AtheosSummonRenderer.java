package mod.inf_iron;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AtheosSummonRenderer extends EntityRenderer<AtheosSummonEntity> {
    public AtheosSummonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AtheosSummonEntity entity) {
        return null;
    }
}
