package mod.inf_iron;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ZoltraakRenderer extends EntityRenderer<ZoltraakEntity> {
    public ZoltraakRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ZoltraakEntity entity) {
        return null;
    }
}
