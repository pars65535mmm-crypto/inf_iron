package mod.inf_iron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = InfIron.MODID)
public class ModEvents {

    // --- Eternal Armor / Halo / Orichalcum: 完全無敵 (全ダメージソース無効化) ---
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (ExpToolExecutionContext.isAnnihilationTarget(victim)) {
            if (event.getAmount() <= 0.0F) {
                event.setCanceled(true);
            }
            return;
        }
        if (victim instanceof Player player) {
            if (isWearingFullEternalArmor(player) || hasHaloEquipped(player) || isWearingFullOrichalcumArmor(player)) {
                event.setCanceled(true);
                if (event.getSource().is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)) {
                    player.teleportTo(player.getX(), 100.0, player.getZ());
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (ExpToolExecutionContext.isAnnihilationTarget(event.getEntity())) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            if (isWearingFullEternalArmor(player) || hasHaloEquipped(player) || isWearingFullOrichalcumArmor(player)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth());
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c§l[IMMORTAL] §r§fDeath is not permitted."), true);
            }
        }
    }

    // --- God Killer / ExpTool & Ultima Weapon: ダメージ処理 ---
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (ExpToolExecutionContext.isAnnihilationTarget(target)) {
            if (event.getAmount() <= 0.0F) {
                event.setCanceled(true);
                return;
            }
            event.setAmount(Float.MAX_VALUE);
            return;
        }

        if (event.getSource() != null) {
            Entity source = event.getSource().getDirectEntity();
            if (source instanceof Player player) {
                ItemStack heldItem = player.getMainHandItem();
                if (OrichalcumExpToolItem.isExpTool(heldItem)) {
                    ExpToolExecutionContext.markForAnnihilation(target);
                    ExpToolAnnihilator.annihilateSingle(player, target);
                    event.setAmount(Float.MAX_VALUE);
                    return;
                }
                if (heldItem.getItem() == ModItems.GOD_KILLER.get()) {
                    ExpToolAnnihilator.godKillerWipe(target);
                    event.setAmount(Float.MAX_VALUE);
                } else if (heldItem.getItem() == ModItems.ULTIMA_WEAPON.get()) {
                    // アルテマウェポン：HP割合に応じたダメージ (最大255)
                    float hpRatio = player.getHealth() / player.getMaxHealth();
                    float newDamage = 255.0F * hpRatio;
                    // バニラの基本攻撃力を上書き
                    event.setAmount(newDamage);
                }
            }
        }
    }

    // --- Eternal Armor: 飛行 & 自動修復 & クールダウン無効化 & 範囲消去 ---
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        
        Player player = event.player;
        Level level = player.level();

        ItemStack mainHand = player.getMainHandItem();

        // ExpTool: 神殺しの剣同等 — クールダウン抹消 + スイング中AoE処刑
        if (OrichalcumExpToolItem.isExpTool(mainHand)) {
            player.resetAttackStrengthTicker();
            clearAllCooldowns(player);
            if (player.swingTime > 0) {
                ExpToolAnnihilator.annihilateAoE(player, level, 10.0D);
            }
        }

        // God Killer: クールダウン強制リセット & 範囲消去
        if (mainHand.getItem() == ModItems.GOD_KILLER.get()) {
            player.resetAttackStrengthTicker();
            clearAllCooldowns(player);
            if (player.swingTime > 0) {
                GodKillerItem.performAoeWipe(player, level);
            }
        }

        // 天上天下唯我独尊 (Death Aura)
        if (player.getPersistentData().getBoolean("OrichalcumDeathAura")) {
            if (player.tickCount % 5 == 0) {
                java.util.List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20.0));
                boolean killed = false;
                for (LivingEntity target : targets) {
                    if (target != player) {
                        if (target instanceof Player p) p.getInventory().clearContent();
                        else {
                            for(EquipmentSlot slot : EquipmentSlot.values()) target.setItemSlot(slot, ItemStack.EMPTY);
                        }
                        target.invulnerableTime = 0;
                        target.setHealth(0.0f);
                        target.die(level.damageSources().playerAttack(player));
                        if(level instanceof net.minecraft.server.level.ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0, target.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
                        }
                        killed = true;
                    }
                }
                if(killed && level instanceof net.minecraft.server.level.ServerLevel sl) {
                    sl.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_SWEEP, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.5f);
                }
            }
        }

        if (isWearingFullEternalArmor(player)) {
            // 絶対不死: 消去(discard)されてもリフレクションで復活し、内部状態を固定
            GodReflector.forceRevive(player);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);

            // 飛行権限 & 超高速化 (0.5f = クリエイティブの10倍)
            player.getAbilities().mayfly = true;
            if (player.getAbilities().flying) {
                // スニーク中は通常のスピードに戻せるように調整 (操作性向上のため)
                float speed = player.isShiftKeyDown() ? 0.05f : 0.5f;
                player.getAbilities().setFlyingSpeed(speed);
            }
            player.onUpdateAbilities();
            
            // 自動修復 (耐久値を最大に)
            player.getArmorSlots().forEach(stack -> {
                if (stack.isDamaged()) {
                    stack.setDamageValue(0);
                }
            });
            
            // デバフ除去
            player.removeAllEffects();
        } else if (isWearingFullOrichalcumArmor(player)) {
            // OrichalcumArmorも同様の処理
            GodReflector.forceRevive(player);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);

            player.getAbilities().mayfly = true;
            if (player.getAbilities().flying) {
                float speed = player.isShiftKeyDown() ? 0.05f : 0.5f;
                player.getAbilities().setFlyingSpeed(speed);
            }
            player.onUpdateAbilities();
            
            player.getArmorSlots().forEach(stack -> {
                if (stack.isDamaged()) {
                    stack.setDamageValue(0);
                }
            });
            player.removeAllEffects();
            
            // 夢幻加速 (Phantom Acceleration)
            if (!player.isShiftKeyDown()) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 219, 49, false, false, false));
            }

            // 神殺しのオーラ (God-Surpassing Aura)
            if (level.isClientSide && player.tickCount % 2 == 0) {
                double x = player.getX() + (level.random.nextDouble() - 0.5) * 2;
                double y = player.getY() + level.random.nextDouble() * 2;
                double z = player.getZ() + (level.random.nextDouble() - 0.5) * 2;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 0.0, 0.1, 0.0);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0, 0.0, 0.0);
            }
        } else {
            // 防具を脱いだら飛行停止 (クリエイティブモード以外)
            if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    // --- Eternal Armor: 強制装着 (ボスの消去対策) ---
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack oldStack = event.getFrom();
            ItemStack newStack = event.getTo();
            EquipmentSlot slot = event.getSlot();

            // 防具スロットで、Eternal/Orichalcum Armorが消されようとした場合
            if (slot.getType() == EquipmentSlot.Type.ARMOR) {
                if ((isEternalArmorPiece(oldStack) && !isEternalArmorPiece(newStack)) ||
                    (isOrichalcumArmorPiece(oldStack) && !isOrichalcumArmorPiece(newStack))) {
                    // クリエイティブモードで意図的に脱ぐ場合以外は、強制的に戻す
                    if (!player.isCreative()) {
                        ItemStack copyStack = oldStack.copy();
                        player.setItemSlot(slot, copyStack);
                    }
                }
            }
        }
    }

    private static boolean isEternalArmorPiece(ItemStack stack) {
        return stack.getItem() instanceof EternalArmorItem;
    }

    private static boolean isOrichalcumArmorPiece(ItemStack stack) {
        return stack.getItem() instanceof OrichalcumArmorItem;
    }

    private static void clearAllCooldowns(Player player) {
        player.getCooldowns().tick();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (!invStack.isEmpty()) {
                player.getCooldowns().removeCooldown(invStack.getItem());
            }
        }
    }

    // --- Omni Tool / Paxel: 超速採掘 ---
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack heldItem = event.getEntity().getMainHandItem();
        if (heldItem.getItem() == ModItems.OMNI_TOOL.get() || heldItem.getItem() == ModItems.ORICHALCUM_PAXEL.get()) {
            event.setNewSpeed(Float.MAX_VALUE);
        }
    }

    // --- Omni Tool / Paxel: 一括破壊 & 岩盤破壊 (Bedrock) ---
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        ItemStack heldItem = player.getMainHandItem();
        
        if (heldItem.getItem() == ModItems.OMNI_TOOL.get() || heldItem.getItem() == ModItems.GOD_KILLER.get() || heldItem.getItem() == ModItems.ORICHALCUM_PAXEL.get()) {
            if (event.getState().getBlock() == Blocks.BEDROCK) {
                event.getLevel().setBlock(event.getPos(), Blocks.AIR.defaultBlockState(), 3);
            }
        }

        if (heldItem.getItem() == ModItems.ORICHALCUM_PAXEL.get()) {
            if (!player.isShiftKeyDown()) {
                if (event.getLevel() instanceof Level level) {
                    int radius = OrichalcumPaxelItem.getMiningRadius(heldItem);
                    if (radius > 0) {
                        breakRadiusAsPaxel(level, event.getPos(), player, radius);
                    }
                }
            }
        } else if (heldItem.getItem() == ModItems.OMNI_TOOL.get() || heldItem.getItem() == ModItems.GOD_KILLER.get()) {
            // 通常の一括破壊 (God Killer または Omni Tool)
            if (!player.isShiftKeyDown()) { 
                if (event.getLevel() instanceof Level level) {
                    chainBreak(level, event.getPos(), event.getState(), player);
                }
            }
        }
    }

    private static boolean isWearingFullEternalArmor(Player player) {
        return isEternalArmorPiece(player.getItemBySlot(EquipmentSlot.HEAD)) &&
               isEternalArmorPiece(player.getItemBySlot(EquipmentSlot.CHEST)) &&
               isEternalArmorPiece(player.getItemBySlot(EquipmentSlot.LEGS)) &&
               isEternalArmorPiece(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean hasHaloEquipped(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.is(ModItems.HALO_CURIOS.get())).isPresent();
    }

    private static void chainBreak(Level level, BlockPos startPos, BlockState targetState, Player player) {
        Block targetBlock = targetState.getBlock();
        Queue<BlockPos> queue = new LinkedList<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(startPos.immutable());
        visited.add(startPos.immutable());

        int count = 0;
        int maxCount = 4096; // 16x16x16程度の範囲

        while (!queue.isEmpty() && count < maxCount) {
            BlockPos pos = Objects.requireNonNull(queue.poll());
            
            for (BlockPos neighbor : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                BlockPos immutablePos = neighbor.immutable();
                if (!visited.contains(immutablePos)) {
                    visited.add(immutablePos);
                    if (level.getBlockState(immutablePos).getBlock() == targetBlock) {
                        level.destroyBlock(immutablePos, true, player);
                        queue.add(immutablePos);
                        count++;
                    }
                }
            }
        }
    }

    private static boolean isWearingFullOrichalcumArmor(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.ORICHALCUM_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.ORICHALCUM_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.ORICHALCUM_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.ORICHALCUM_BOOTS.get();
    }

    private static void breakRadiusAsPaxel(Level level, BlockPos startPos, Player player, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) continue; // Original block already breaking
                    BlockPos target = startPos.offset(x, y, z);
                    BlockState state = level.getBlockState(target);
                    if (!state.isAir()) {
                        level.destroyBlock(target, true, player);
                    }
                }
            }
        }
    }
}
