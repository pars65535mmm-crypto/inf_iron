package mod.inf_iron;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * ExpTool — 目的は「どんな mob でも消し飛ばす」ことのみ。
 * 旧オリハルコン絶技・採掘機能は廃止。
 */
public class OrichalcumExpToolItem extends SwordItem {

    private static final String NBT_MODE_ID = "ExpToolMode";
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    public static final String[] MODE_NAMES = {
            "絶対処刑 (Annihilate)",
            "範囲処刑 (AoE Wipe)",
            "ステコンオーバーフロー",
            "時空凍結・処刑"
    };

    public OrichalcumExpToolItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                9_999_999_999.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                1000.0D, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    public static int getMode(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_MODE_ID)) {
            return stack.getTag().getInt(NBT_MODE_ID);
        }
        return 0;
    }

    public static void setMode(ItemStack stack, int mode) {
        stack.getOrCreateTag().putInt(NBT_MODE_ID, Math.floorMod(mode, MODE_NAMES.length));
    }

    public static boolean isExpTool(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof OrichalcumExpToolItem;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide) {
            executeMode(player, entity, getMode(stack), true);
            ExpToolAnnihilator.annihilateAoE(player, player.level(), 10.0D);
        }
        return true;
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                LivingEntity nearest = findNearestLiving(player, 64.0);
                executeMode(player, nearest, getMode(stack), false);
            } else {
                ExpToolAnnihilator.annihilateAoE(player, level, 16.0D);
            }
        }
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void executeMode(Player player, Entity target, int mode, boolean fromAttack) {
        if (player.level().isClientSide) return;

        switch (mode) {
            case 1 -> ExpToolAnnihilator.annihilateAoE(player, player.level(), fromAttack ? 12.0D : 24.0D);
            case 2 -> {
                if (target instanceof LivingEntity living) {
                    ExpToolAnnihilator.statusEffectOverflow(living);
                    ExpToolAnnihilator.annihilateSingle(player, living);
                } else {
                    ExpToolAnnihilator.annihilateAoE(player, player.level(), 12.0D);
                }
            }
            case 3 -> {
                if (target instanceof LivingEntity living) {
                    ExpToolAnnihilator.spacetimeAnnihilate(player, living);
                } else {
                    ExpToolAnnihilator.annihilateAoE(player, player.level(), 16.0D);
                }
            }
            default -> {
                if (target != null) {
                    ExpToolAnnihilator.annihilateSingle(player, target);
                } else {
                    ExpToolAnnihilator.annihilateAoE(player, player.level(), 12.0D);
                }
            }
        }
    }

    private static LivingEntity findNearestLiving(Player player, double range) {
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range));
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            if (e == player || !e.isAlive()) continue;
            double d = e.distanceToSqr(player);
            if (d < best) {
                best = d;
                nearest = e;
            }
        }
        return nearest;
    }

    @Override
    public Component getName(ItemStack stack) {
        return DynamicTextHelper.getRainbowText(super.getName(stack).getString());
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int mode = getMode(stack);
        tooltip.add(Component.literal("§4§l処刑モード: §r§f" + MODE_NAMES[mode]));
        tooltip.add(DynamicTextHelper.getRainbowText("「どんな mob でも、消し飛ばす。」"));
        tooltip.add(Component.literal("§8左クリック: 対象＋周囲を処刑"));
        tooltip.add(Component.literal("§8右クリック: 広域処刑"));
        tooltip.add(Component.literal("§8スニーク+右クリック: モードの特殊処刑"));
        tooltip.add(Component.literal("§8Gキー: モード切替"));
    }
}
