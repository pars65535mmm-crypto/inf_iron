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
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils; 
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;

/**
 * ExpTool — 目的は「どんな mob でも消し飛ばす」ことのみ。
 * 右クリックでMobごと世界をフォルダから物理消滅させる。
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

@Override
public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
        
        net.minecraft.world.level.chunk.LevelChunk chunk = level.getChunkAt(player.blockPosition());
        net.minecraft.world.level.ChunkPos chunkPos = chunk.getPos();

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[SYSTEM] 奈落100万ブロック転送シーケンス起動。"));

        net.minecraft.world.phys.AABB chunkArea = new net.minecraft.world.phys.AABB(
            chunkPos.getMinBlockX(), serverLevel.getMinBuildHeight(), chunkPos.getMinBlockZ(),
            chunkPos.getMaxBlockX(), serverLevel.getMaxBuildHeight(), chunkPos.getMaxBlockZ()
        );
        java.util.List<Entity> targets = new java.util.ArrayList<>();
        for (Entity entity : serverLevel.getEntitiesOfClass(Entity.class, chunkArea)) {
            targets.add(entity);
        }

        // 安全なドロップアイテム保持用のデータリスト
        java.util.List<net.minecraft.world.item.ItemStack> lootCache = new java.util.ArrayList<>();

        // ＝＝＝ 奈落転送 ＆ データキープループ ＝＝＝
        for (Entity target : targets) {
            if (target instanceof Player || target == player) continue;

            if (target instanceof net.minecraft.world.entity.LivingEntity livingTarget) {
                
                // ①【データ回収】奈落に落とす前に、装備品などをItemStackとしてキープ
                for (net.minecraft.world.item.ItemStack equipment : livingTarget.getAllSlots()) {
                    if (!equipment.isEmpty()) {
                        lootCache.add(equipment.copy()); 
                    }
                }
                // ボス討伐報酬としての確定保険（ネザースター）をデータ層に仕込む
                lootCache.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHER_STAR));

                // 提供されたパイプラインを実行（既存処理との互換性維持）
                mod.inf_iron.ExpToolExecutionContext.markForAnnihilation(target);
                mod.inf_iron.ExpToolAnnihilator.runFullPipeline(player, target);

                // ②【奈落ワープ（>>1のアイデア）】座標を正常な数値（Y = -1000000）にして遥か彼方に隔離
                // NaNではないため、IllegalStateException（ファイル破損）は絶対に起きない！
                livingTarget.setPos(livingTarget.getX(), -1000000.0D, livingTarget.getZ());
                
                // 完全に見えなくし、AIを止め、無敵化（奈落の底で置物にする）
                livingTarget.setInvisible(true); 
                livingTarget.setInvulnerable(true); 
                if (livingTarget instanceof net.minecraft.world.entity.Mob mobTarget) {
                    mobTarget.setNoAi(true);
                }

                // ボスバーをプレイヤーの画面から取り除く（距離減衰＋強制非表示）
                try {
                    java.lang.reflect.Field bossEventField = livingTarget.getClass().getDeclaredField("bossEvent");
                    bossEventField.setAccessible(true);
                    Object serverBossEvent = bossEventField.get(livingTarget);
                    if (serverBossEvent instanceof net.minecraft.server.level.ServerBossEvent sbe) {
                        sbe.removeAllPlayers();
                        sbe.setVisible(false);
                    }
                } catch (Exception ignored) {}
            }
        }

        // ③ チャンクを未セーブにして「地上には誰もいなくなったデータ」を強制上書き保存
        // 敵は正常な座標（-1000000）にいるため、保存エラーにならず完璧にセーブが成功する！
        chunk.setUnsaved(true);
        serverLevel.save(null, true, false);

        // ④ 全カスタムボスバーの強制非表示化
        try {
            net.minecraft.server.bossevents.CustomBossEvents customBossEvents = serverLevel.getServer().getCustomBossEvents();
            for (net.minecraft.server.bossevents.CustomBossEvent bossBar : customBossEvents.getEvents()) {
                bossBar.removePlayer((net.minecraft.server.level.ServerPlayer) player);
                bossBar.setVisible(false);
            }
        } catch (Exception ignored) {}

        // ⑤【安全圏での戦利品具現化】
        // 敵が奈落の底に隔離され、ファイル保存も成功した後に、キープしていた戦利品をドロップ！
        int dropCount = 0;
        for (net.minecraft.world.item.ItemStack safeItem : lootCache) {
            if (!safeItem.isEmpty()) {
                player.spawnAtLocation(safeItem, 0.5f);
                dropCount++;
            }
        }

        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "§a[SYSTEM] 敵を奈落の底（Y-1000000）に永久隔離しました。戦利品（" + dropCount + "個）を回収。"
        ));
    }
    
    return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
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
        tooltip.add(Component.literal("§8スニーク+右クリック: モードの特殊処刑"));
        tooltip.add(Component.literal("§8Gキー: モード切替"));
    }
}