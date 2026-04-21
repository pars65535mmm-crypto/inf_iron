package mod.inf_iron;

import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import javax.annotation.Nonnull;
import java.util.List;

public class OrichalcumExpToolItem extends DiggerItem {

    private final com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> defaultModifiers;
    private static final String NBT_SKILL_ID = "ActiveSkillId";

    public OrichalcumExpToolItem(Tier tier, float attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(attackDamageModifier, attackSpeedModifier, tier, BlockTags.MINEABLE_WITH_PICKAXE, properties);
        
        com.google.common.collect.ImmutableMultimap.Builder<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> builder = com.google.common.collect.ImmutableMultimap.builder();
        builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 9999999999.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        builder.put(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", 1000.0D, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    @Override
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier> getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        return slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }

    // Paxel Radius Control
    public static int getMiningRadius(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("MiningRadius")) {
            return stack.getTag().getInt("MiningRadius");
        }
        return 0; // 0 means 1x1 (radius 0)
    }

    public static void setMiningRadius(ItemStack stack, int radius) {
        stack.getOrCreateTag().putInt("MiningRadius", radius);
    }

    // Active Skill Control
    public static int getActiveSkill(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(NBT_SKILL_ID)) {
            return stack.getTag().getInt(NBT_SKILL_ID);
        }
        return 0;
    }

    public static void setActiveSkill(ItemStack stack, int skillId) {
        stack.getOrCreateTag().putInt(NBT_SKILL_ID, skillId % 13);
    }

    public static final String[] SKILL_NAMES = {
        "瞬閃 (FlashTeleport)",
        "星隕 (Meteor Dash)",
        "大陸砕き (Continent Breaker)",
        "剣技・因果 (Sword Skill: Causality)",
        "輪廻・拡散 (Reincarnation Diffusion)",
        "螺旋・黒穴 (Spiral Singularity)",
        "天の閃光 (Heavenly Laser)",
        "七色の嵐 (Rainbow Storm)",
        "アルテマ (Ultima Magic Circle)",
        "空間断裂 (Spatial Rift)",
        "時空凍結 (Time Freeze)",
        "夢幻至高 (Phantom Supreme)",
        "天上天下唯我独尊 (The Honored One)"
    };

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            if (!player.level().isClientSide) {
                int currentSkill = getActiveSkill(stack);
                int nextSkill = (currentSkill + 1) % 13;
                setActiveSkill(stack, nextSkill);
                player.displayClientMessage(Component.literal("§e§l現在の絶技: §r§f" + SKILL_NAMES[nextSkill]), true);
            }
            return true; // Cancel vanilla swing
        }
        return false;
    }

    // 左クリック攻撃（因果・絶ちのブレード効果）
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (!player.level().isClientSide && entity instanceof LivingEntity target && !player.isShiftKeyDown()) {
            Level level = player.level();
            level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 2.0f, 0.5f);
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
            }
            executeTrueMurder(level, player, target);
        }
        return true; 
    }

    private void executeTrueMurder(Level level, Player player, LivingEntity target) {
        if (target == player) return;
        
        if (target instanceof Player targetPlayer) {
            targetPlayer.getInventory().clearContent();
        } else {
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                target.setItemSlot(slot, ItemStack.EMPTY);
            }
        }
        
        target.invulnerableTime = 0;
        target.setHealth(0.0F);
        target.die(level.damageSources().playerAttack(player));
    }

    // 右クリックで技の発動
    @Nonnull
    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            int skill = getActiveSkill(itemstack);
            executeSkill(level, player, skill);
        }
        
        player.getCooldowns().addCooldown(this, 20); 
        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void executeSkill(Level level, Player player, int skillId) {
        switch (skillId) {
            case 0: // 瞬閃
                player.displayClientMessage(Component.literal("§b§l[瞬閃] §r§f空間を超越する...！"), true);
                net.minecraft.world.phys.Vec3 look = player.getLookAngle();
                player.teleportTo(player.getX() + look.x * 50, player.getY() + look.y * 50, player.getZ() + look.z * 50);
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                break;
            case 1: // 星隕
                player.displayClientMessage(Component.literal("§c§l[星隕] §r§f星の如く墜ちよ！"), true);
                net.minecraft.world.phys.Vec3 dash = player.getLookAngle().scale(5.0D);
                player.setDeltaMovement(dash);
                player.hurtMarked = true;
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 50, 1, 1, 1, 0.5);
                }
                break;
            case 2: // 大陸砕き
                player.displayClientMessage(Component.literal("§6§l[大陸砕き] §r§f大地を抉る！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.core.BlockPos center = player.blockPosition().relative(player.getDirection(), 15);
                    int r = 15;
                    for (int x = -r; x <= r; x++) {
                        for (int y = -r; y <= r; y++) {
                            for (int z = -r; z <= r; z++) {
                                net.minecraft.core.BlockPos pos = center.offset(x, y, z);
                                net.minecraft.world.level.block.state.BlockState bs = serverLevel.getBlockState(pos);
                                if (!bs.isAir() && bs.getDestroySpeed(level, pos) >= 0.0f) {
                                    serverLevel.destroyBlock(pos, true);
                                }
                            }
                        }
                    }
                }
                break;
            case 3: // 剣技・因果
                player.displayClientMessage(Component.literal("§7§l[剣技・因果] §r§f道を開けろ...！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.world.phys.Vec3 start = player.getEyePosition();
                    net.minecraft.world.phys.Vec3 dir = player.getLookAngle();
                    for(int i = 0; i < 30; i++) {
                        net.minecraft.world.phys.Vec3 pos = start.add(dir.scale(i));
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 2, 0.5, 0.5, 0.5, 0.0);
                        java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(2.0));
                        for(LivingEntity e : enemies) if(e != player) executeTrueMurder(level, player, e);
                    }
                }
                break;
            case 4: // 輪廻・拡散
                player.displayClientMessage(Component.literal("§d§l[輪廻・拡散] §r§f千の刃が悉くを断つ...！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64.0));
                    for (LivingEntity e : enemies) {
                        if (e != player) {
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1.0, e.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                            executeTrueMurder(level, player, e);
                        }
                    }
                }
                break;
            case 5: // 螺旋・黒穴
                player.displayClientMessage(Component.literal("§5§l[螺旋・黒穴] §r§f深淵に平伏せよ...！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.world.phys.Vec3 center = player.getEyePosition().add(player.getLookAngle().scale(20.0D));
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 500, 2.0, 2.0, 2.0, 0.5);
                    java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(100.0));
                    for (LivingEntity e : enemies) {
                        if (e != player) {
                            e.teleportTo(center.x, center.y, center.z);
                            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1.0, e.getZ(), 5, 0.5, 0.5, 0.5, 0.0);
                            executeTrueMurder(level, player, e);
                        }
                    }
                }
                break;
            case 6: // 天の閃光
                player.displayClientMessage(Component.literal("§e§l[天の閃光] §r§f光よ、穿て！"), true);
                net.minecraft.world.phys.HitResult hitResult = player.pick(100.0D, 0.0F, false);
                net.minecraft.world.phys.Vec3 tx = hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS ? hitResult.getLocation() : player.getEyePosition().add(player.getLookAngle().scale(100.0D));
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    for (int y = (int)tx.y; y < sl.getMaxBuildHeight(); y += 2) {
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, tx.x, y, tx.z, 30, 0.5, 2.0, 0.5, 0.0);
                        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, tx.x, y, tx.z, 30, 0.5, 2.0, 0.5, 0.1);
                    }
                }
                java.util.List<LivingEntity> bEnemies = level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(tx.x - 10, tx.y - 5, tx.z - 10, tx.x + 10, tx.y + 15, tx.z + 10));
                for(LivingEntity e : bEnemies) if(e!=player) executeTrueMurder(level,player,e);
                break;
            case 7: // 七色の嵐
                player.displayClientMessage(Component.literal("§a§l[七色の嵐] §r§f絶望の虹を架けようか！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    for (int i = 0; i < 100; i++) {
                        double dx = player.getX() + (level.random.nextDouble() - 0.5) * 100;
                        double dz = player.getZ() + (level.random.nextDouble() - 0.5) * 100;
                        net.minecraft.world.entity.LightningBolt lightning = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
                        if (lightning != null) {
                            lightning.moveTo(dx, player.getY(), dz);
                            lightning.setVisualOnly(true);
                            level.addFreshEntity(lightning);
                        }
                        serverLevel.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat()), 4.0F), dx, player.getY() + 5, dz, 20, 5.0, 5.0, 5.0, 1.0);
                    }
                    java.util.List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50.0));
                    for(LivingEntity e : enemies) if(e!=player) executeTrueMurder(level,player,e);
                }
                break;
            case 8: // アルテマ
                player.displayClientMessage(Component.literal("§5§l[アルテマ] §r§f全てを無に！"), true);
                net.minecraft.core.BlockPos targetPos = net.minecraft.core.BlockPos.containing(player.pick(100.0D, 0.0F, false).getLocation());
                if (level instanceof net.minecraft.server.level.ServerLevel) {
                    Entity magicCircle = ModEntities.ULTIMA_MAGIC_CIRCLE.get().create(level);
                    if (magicCircle instanceof UltimaMagicCircleEntity ultima) {
                        ultima.setOwner(player);
                        ultima.moveTo(targetPos.getX(), targetPos.getY() + 5, targetPos.getZ());
                        level.addFreshEntity(ultima);
                    }
                }
                break;
            case 9: // 空間断裂
                player.displayClientMessage(Component.literal("§9§l[空間断裂] §r§f世界を削り取る...！"), true);
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    net.minecraft.core.BlockPos c = player.blockPosition();
                    int rr = 30;
                    for (int ix = -rr; ix <= rr; ix++) {
                        for (int iy = -rr; iy <= rr; iy++) {
                            for (int iz = -rr; iz <= rr; iz++) {
                                if (ix*ix + iy*iy + iz*iz <= rr*rr) {
                                    serverLevel.setBlockAndUpdate(c.offset(ix, iy, iz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                                }
                            }
                        }
                    }
                }
                break;
            case 10: // 時空凍結
                player.displayClientMessage(Component.literal("§3§l[時空凍結] §r§f時よ止まれ...！"), true);
                java.util.List<LivingEntity> frozen = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(100.0));
                for(LivingEntity e : frozen) {
                    if (e != player) {
                        e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 600, 255, false, false));
                        // もしCustom AI Disable等があればここに入れる
                    }
                }
                break;
            case 11: // 夢幻至高
                player.displayClientMessage(Component.literal("§d§l[夢幻至高] §r§f限界を越えろ！"), true);
                player.removeAllEffects();
                player.setHealth(player.getMaxHealth());
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 255, false, false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 1200, 255, false, false));
                // 簡易的に強力なバフを付与
                break;
            case 12: // 天上天下唯我独尊
                player.displayClientMessage(Component.literal("§e§l[天上天下唯我独尊] §r§f我こそが絶対...！"), true);
                // "死のオーラ" トグルはイベント側で処理するため、タグを設定
                boolean currentAura = player.getPersistentData().getBoolean("OrichalcumDeathAura");
                player.getPersistentData().putBoolean("OrichalcumDeathAura", !currentAura);
                if (!currentAura) {
                    player.displayClientMessage(Component.literal("§c死のオーラを展開しました。近づく者は全て消滅します。"), false);
                } else {
                    player.displayClientMessage(Component.literal("§a死のオーラを解除しました。"), false);
                }
                break;
        }
    }

    @Override
    public Component getName(ItemStack pStack) {
        return DynamicTextHelper.getRainbowText(super.getName(pStack).getString()); // 更なる上位として虹色
    }

    @Override
    public void appendHoverText(ItemStack pStack, @javax.annotation.Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        int r = getMiningRadius(pStack);
        int size = (r * 2) + 1;
        int activeSkill = getActiveSkill(pStack);
        pTooltipComponents.add(Component.literal("§c⚔ 絶技: " + SKILL_NAMES[activeSkill]));
        pTooltipComponents.add(Component.literal("§bCurrent Mode: " + size + "x" + size));
        pTooltipComponents.add(DynamicTextHelper.getRainbowText("「破壊も殺害も超越も…これ一本で充分だ。神すらひれ伏す刃を見よ！」"));
        pTooltipComponents.add(Component.literal("§8(スニーク＋スクロール: 採掘範囲変更)"));
        pTooltipComponents.add(Component.literal("§8(スニーク＋左クリック: 技の切り替え)"));
        pTooltipComponents.add(Component.literal("§8(右クリック: 技の発動)"));
    }
}
