package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.Comparator;

public class FerrumOfExecutionItem extends TieredItem implements Vanishable {
    private static final String NBT_MODE = "ExecutionMode";
    private static final UUID ATTACK_DAMAGE_MODIFIER_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    public FerrumOfExecutionItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        super(tier, properties);
    }

    public enum Mode {
        GREATSWORD("Greatsword", 2000.0D, -2.4D, 0),
        AXE("Executioner's Axe", 3500.0D, -3.2D, 1),
        SCYTHE("Soul Reaper", 1500.0D, -2.0D, 2),
        WAND("Last Prism", 500.0D, 0.0D, 3);

        final String name;
        final double damage;
        final double speed;
        final int id;

        Mode(String name, double damage, double speed, int id) {
            this.name = name;
            this.damage = damage;
            this.speed = speed;
            this.id = id;
        }

        public static Mode byId(int id) {
            for (Mode m : values())
                if (m.id == id)
                    return m;
            return GREATSWORD;
        }
    }

    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player,
            @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int nextMode = (getMode(stack).id + 1) % Mode.values().length;
                setMode(stack, Mode.byId(nextMode));
                player.displayClientMessage(Component.literal("Mode Switched: ")
                        .append(DynamicTextHelper.getGradientText(Mode.byId(nextMode).name)), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_BUTTON_CLICK.get(),
                        SoundSource.PLAYERS, 1.0F, 1.5F);
            }
            return InteractionResultHolder.success(stack);
        } else {
            Mode mode = getMode(stack);
            if (mode == Mode.WAND || mode == Mode.GREATSWORD) {
                player.startUsingItem(hand);
                return InteractionResultHolder.consume(stack);
            } else if (mode == Mode.AXE) {
                if (!level.isClientSide) {
                    performSeaSplit(level, player);
                }
                return InteractionResultHolder.success(stack);
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private void performSeaSplit(Level level, Player player) {
        Vec3 start = player.position();
        Vec3 look = player.getLookAngle().multiply(1, 0, 1).normalize();
        int maxBlocks = 100;
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0F, 0.5F);
        for (int i = 0; i < maxBlocks; i++) {
            Vec3 targetPos = start.add(look.scale(i));
            BlockPos basePos = BlockPos.containing(targetPos);
            boolean hit = false;
            for (int dy = -2; dy <= 1; dy++) {
                BlockPos p = basePos.offset(0, dy, 0);
                if (level.getBlockState(p).getBlock() != Blocks.BEDROCK && !level.getBlockState(p).isAir()) {
                    level.destroyBlock(p, false);
                    hit = true;
                }
            }
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, targetPos.x, targetPos.y, targetPos.z, 1, 0.5, 0.5, 0.5, 0.05);
            }
            if (i > 5 && !hit && level.getBlockState(basePos.below(3)).isAir()) break;
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int count) {
        if (living instanceof Player player) {
            Mode mode = getMode(stack);
            if (mode == Mode.WAND) {
                spawnCompositeWandAttack(level, player, count);
            } else if (mode == Mode.GREATSWORD) {
                spawnEnhancedGateOfBabylon(level, player, count);
            }
        }
    }

    private void spawnEnhancedGateOfBabylon(Level level, Player player, int count) {
        // 頻度を 1tick 毎にアップ
        Vec3 look = player.getLookAngle();
        Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
        
        // 1tickにつき1〜2枚発射
        for (int i = 0; i < 2; i++) {
            double offsetSide = (Math.random() - 0.5) * 8.0;
            double offsetUp = 1.0 + Math.random() * 5.0;
            double offsetBehind = 1.0 + Math.random() * 3.0;
            Vec3 portalPos = player.position().add(0, 1.5, 0).subtract(look.scale(offsetBehind)).add(right.scale(offsetSide)).add(0, offsetUp, 0);
            
            if (level.isClientSide) {
                level.addParticle(ParticleTypes.GLOW, portalPos.x, portalPos.y, portalPos.z, 0, 0, 0);
            } else {
                level.playSound(null, portalPos.x, portalPos.y, portalPos.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3F, 1.8F);
                
                // ホーミングを考慮した弾道
                Vec3 fireDir = look;
                Entity target = findNearestTarget(level, player, 40.0D);
                if (target != null) {
                    // ターゲット方向に少し寄せる
                    Vec3 toTarget = target.getBoundingBox().getCenter().subtract(portalPos).normalize();
                    fireDir = look.scale(0.7).add(toTarget.scale(0.3)).normalize();
                }

                for (double d = 0; d < 40; d += 1.0) {
                    Vec3 bulletPos = portalPos.add(fireDir.scale(d));
                    AABB aabb = new AABB(bulletPos.x - 0.8, bulletPos.y - 0.8, bulletPos.z - 0.8, bulletPos.x + 0.8, bulletPos.y + 0.8, bulletPos.z + 0.8);
                    List<Entity> targets = level.getEntities(player, aabb, e -> e instanceof LivingEntity && e.isAlive());
                    for (Entity targetEnt : targets) {
                        targetEnt.hurt(level.damageSources().playerAttack(player), 400.0F);
                    }
                    if (level instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CRIT, bulletPos.x, bulletPos.y, bulletPos.z, 1, 0, 0, 0, 0);
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, bulletPos.x, bulletPos.y, bulletPos.z, 1, 0.1, 0.1, 0.1, 0.01);
                    }
                    if (!level.getBlockState(BlockPos.containing(bulletPos)).isAir() && d > 2.0) break;
                }
            }
        }
    }

    private void spawnCompositeWandAttack(Level level, Player player, int count) {
        // 1. 正面螺旋ビーム (Last Prism)
        spawnSpiralBeam(level, player);

        // 2. ゾルトラーク生成 (1秒3発 = 約7tickに1発)
        if (count % 7 == 0 && !level.isClientSide) {
            Vec3 look = player.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            
            // 側面から少し外向きに発射
            double side = Math.random() > 0.5 ? 1.0 : -1.0;
            Vec3 spawnPosOffset = right.scale(side * 2.0).add(0, 1.0, 0);
            Vec3 initialVel = look.add(right.scale(side * 0.5)).normalize();
            
            ZoltraakEntity zoltraak = new ZoltraakEntity(level, player, initialVel);
            zoltraak.setPos(player.getX() + spawnPosOffset.x, player.getY() + 1.5 + spawnPosOffset.y, player.getZ() + spawnPosOffset.z);
            level.addFreshEntity(zoltraak);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
    }

    private void spawnSpiralBeam(Level level, Player player) {
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        double range = 30.0D;

        for (double d = 0; d < range; d += 0.8D) {
            Vec3 pos = start.add(look.scale(d));
            if (!level.isClientSide) {
                AABB aabb = new AABB(pos.x - 1.2, pos.y - 1.2, pos.z - 1.2, pos.x + 1.2, pos.y + 1.2, pos.z + 1.2);
                for (Entity e : level.getEntities(player, aabb, e -> e instanceof LivingEntity)) {
                    e.hurt(level.damageSources().playerAttack(player), 120.0F);
                    ((LivingEntity)e).invulnerableTime = 0;
                }
            } else {
                double time = level.getGameTime() * 0.6D;
                double angle = d * 0.4D + time;
                double radius = 0.4D;
                Vec3 offset = new Vec3(Math.cos(angle), Math.sin(angle * 0.5), Math.sin(angle)).scale(radius);
                level.addParticle(ParticleTypes.SQUID_INK, pos.x + offset.x, pos.y + offset.y, pos.z + offset.z, 0, 0, 0);
                level.addParticle(ParticleTypes.END_ROD, pos.x - offset.x, pos.y - offset.y, pos.z - offset.z, 0, 0, 0);
            }
            if (!level.getBlockState(BlockPos.containing(pos)).isAir() && d > 1.0) break;
        }
    }

    private Entity findNearestTarget(Level level, Player player, double range) {
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(range), e -> e instanceof LivingEntity && e.isAlive());
        return targets.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(player))).orElse(null);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        Mode mode = getMode(stack);
        return (mode == Mode.WAND || mode == Mode.GREATSWORD) ? UseAnim.BOW : UseAnim.NONE;
    }

    @Nonnull
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(@Nonnull EquipmentSlot slot,
            @Nonnull ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            Mode mode = getMode(stack);
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER_UUID, "Weapon modifier",
                    mode.damage, AttributeModifier.Operation.ADDITION));
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER_UUID, "Weapon modifier",
                    mode.speed, AttributeModifier.Operation.ADDITION));
            return builder.build();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        Mode mode = getMode(stack);
        if (!player.level().isClientSide) {
            if (mode == Mode.SCYTHE) {
                GodKillerItem.performAoeWipe(player, player.level());
            } else if (mode == Mode.GREATSWORD) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX(), entity.getY() + 1, entity.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
                }
            }
        }
        return super.onLeftClickEntity(stack, player, entity);
    }

    @Nonnull
    @Override
    public Component getName(@Nonnull ItemStack pStack) {
        String baseName = Component.translatable("item.inf_iron.ferrum_of_execution").getString();
        if (baseName.equals("item.inf_iron.ferrum_of_execution")) baseName = "Ferrum of Execution";
        return DynamicTextHelper.getGradientText(baseName + " [" + getMode(pStack).name + "]");
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel,
            List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(DynamicTextHelper.getGradientText("The final verdict has been passed."));
        pTooltipComponents.add(Component.literal("§7Shift + Right Click to switch modes."));
        pTooltipComponents.add(Component.literal("§7Normal Right Click for special ability."));
    }

    public static Mode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? Mode.byId(tag.getInt(NBT_MODE)) : Mode.GREATSWORD;
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateTag().putInt(NBT_MODE, mode.id);
    }
}
