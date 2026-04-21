package mod.inf_iron;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.UUID;
import java.util.List;
import javax.annotation.Nonnull;

public class EternalArmorItem extends ArmorItem {
    // Unique UUID per equipment slot to avoid stacking issues
    private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{
        UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
        UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
        UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
        UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")
    };
    public EternalArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack pStack, @javax.annotation.Nullable Level pLevel, @Nonnull List<Component> pTooltipComponents, @Nonnull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(DynamicTextHelper.getGradientText("The embodiment of absolute invulnerability."));
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot pEquipmentSlot) {
        if (pEquipmentSlot == this.type.getSlot()) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.putAll(super.getDefaultAttributeModifiers(pEquipmentSlot));
            // Add ridiculous health limit (around ~2 billion max health)
            UUID uuid = ARMOR_MODIFIER_UUID_PER_SLOT[pEquipmentSlot.getIndex()];
            builder.put(Attributes.MAX_HEALTH, new AttributeModifier(uuid, "Armor health boost", (double)Integer.MAX_VALUE, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getDefaultAttributeModifiers(pEquipmentSlot);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        player.getCooldowns().tick(); 
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty()) {
                player.getCooldowns().removeCooldown(invStack.getItem());
            }
        }
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }
}
