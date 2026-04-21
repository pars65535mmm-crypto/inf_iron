package mod.inf_iron;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class ModTiers {
    public static final Tier COMPRESSED_IRON = new ForgeTier(
            5, // level (Netherite is 4)
            -1, // durability (unbreakable)
            25.0F, // efficiency
            20.0F, // attack damage
            25, // enchantment value
            null,
            () -> Ingredient.EMPTY
    );

    public static final Tier GOD_TIER = new ForgeTier(
            10, // level (Bedrock breaker)
            -1, // durability
            Float.MAX_VALUE, // efficiency
            Float.MAX_VALUE, // attack damage
            100,
            null,
            () -> Ingredient.EMPTY
    );

    public static final Tier ORICHALCUM_TIER = new ForgeTier(
            20, 
            -1, 
            Float.MAX_VALUE, 
            Float.MAX_VALUE, 
            Short.MAX_VALUE,
            null,
            () -> Ingredient.EMPTY
    );
}
