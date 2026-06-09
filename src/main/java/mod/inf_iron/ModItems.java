package mod.inf_iron;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
        public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS,
                        InfIron.MODID);
        public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
                        .create(Registries.CREATIVE_MODE_TAB, InfIron.MODID);

        // オリジナルレアリティ：Eschaton
        public static final Rarity ESCHATON = Rarity.create("ESCHATON", net.minecraft.ChatFormatting.WHITE);

        // 1-18段階の圧縮鉄
        public static final List<RegistryObject<Item>> COMPRESSED_IRONS = new ArrayList<>();
        // 1-9段階の圧縮ネザースター (9^9 = 387,420,489)
        public static final List<RegistryObject<Item>> COMPRESSED_NETHER_STARS = new ArrayList<>();

        static {
                for (int i = 1; i <= 18; i++) {
                        final int level = i;
                        COMPRESSED_IRONS.add(ITEMS.register("iron_compressed_" + level,
                                        () -> new Item(new Item.Properties().rarity(Rarity.RARE))));
                }
                for (int i = 1; i <= 9; i++) {
                        final int level = i;
                        COMPRESSED_NETHER_STARS.add(ITEMS.register("nether_star_compressed_" + level,
                                        () -> new Item(new Item.Properties().rarity(ESCHATON).fireResistant())));
                }
        }

        // 武器：神殺しの剣 (God Killer)
        public static final RegistryObject<Item> GOD_KILLER = ITEMS.register("god_killer",
                        () -> new GodKillerItem(ModTiers.GOD_TIER, 3, -2.4F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 変形武器：Ferrum of Execution
        public static final RegistryObject<Item> FERRUM_OF_EXECUTION = ITEMS.register("ferrum_of_execution",
                        () -> new FerrumOfExecutionItem(ModTiers.GOD_TIER, 3, -2.4F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 防具：不滅の権限 (Eternal Armor)
        public static final RegistryObject<Item> ETERNAL_HELMET = ITEMS.register("eternal_helmet",
                        () -> new EternalArmorItem(ModArmorMaterial.ETERNAL, ArmorItem.Type.HELMET,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ETERNAL_CHESTPLATE = ITEMS.register("eternal_chestplate",
                        () -> new EternalArmorItem(ModArmorMaterial.ETERNAL, ArmorItem.Type.CHESTPLATE,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ETERNAL_LEGGINGS = ITEMS.register("eternal_leggings",
                        () -> new EternalArmorItem(ModArmorMaterial.ETERNAL, ArmorItem.Type.LEGGINGS,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ETERNAL_BOOTS = ITEMS.register("eternal_boots",
                        () -> new EternalArmorItem(ModArmorMaterial.ETERNAL, ArmorItem.Type.BOOTS,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        // ツール：万能破壊 (Omni Tool)
        // 多機能ツールとしてPickaxeItemをベースにし、イベントで斧の挙動などを追加
        public static final RegistryObject<Item> OMNI_TOOL = ITEMS.register("omni_tool",
                        () -> new PickaxeItem(ModTiers.GOD_TIER, 1, -2.8F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()) {
                                @javax.annotation.Nonnull
                                @Override
                                public net.minecraft.network.chat.Component getName(
                                                @javax.annotation.Nonnull ItemStack pStack) {
                                        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
                                }

                                @Override
                                public void appendHoverText(@javax.annotation.Nonnull ItemStack pStack,
                                                @javax.annotation.Nullable net.minecraft.world.level.Level pLevel,
                                                @javax.annotation.Nonnull java.util.List<net.minecraft.network.chat.Component> pTooltipComponents,
                                                @javax.annotation.Nonnull net.minecraft.world.item.TooltipFlag pIsAdvanced) {
                                        pTooltipComponents.add(DynamicTextHelper.getGradientText(
                                                        "The ultimate tool of creation and destruction."));
                                }
                        });

        // 武器：必中ホーミング弓 (Homing Bow)
        public static final RegistryObject<Item> HOMING_BOW = ITEMS.register("homing_bow",
                        () -> new HomingBowItem(new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 光輪 (Halo Curios)
        public static final RegistryObject<Item> HALO_CURIOS = ITEMS.register("halo_curios",
                        () -> new HaloCurioItem(new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 特製Curio：虚空の真影 (Veritas Curios)
        public static final RegistryObject<Item> VERITAS_CURIOS = ITEMS.register("veritas_curios",
                        () -> new VeritasCuriosItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));

        // 終焉のファンネル (Funnel Curios)
        public static final RegistryObject<Item> FUNNEL_CURIOS = ITEMS.register("funnel_curios",
                        () -> new FunnelCurioItem(new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 零の指輪 (Ring of Zero)
        public static final RegistryObject<Item> INFINITY_RING = ITEMS.register("infinity_ring",
                        () -> new InfinityRingItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));

        // 召喚アイテム：アテオスの核 (Atheos Core)
        public static final RegistryObject<Item> ATHEOS_CORE = ITEMS.register("atheos_core",
                        () -> new Item(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)) {
                                @Override
                                public net.minecraft.world.InteractionResult useOn(
                                                net.minecraft.world.item.context.UseOnContext context) {
                                        if (!context.getLevel().isClientSide) {
                                                AtheosSummonEntity summon = ModEntities.ATHEOS_SUMMON.get()
                                                                .create(context.getLevel());
                                                if (summon != null) {
                                                        summon.moveTo(context.getClickLocation());
                                                        context.getLevel().addFreshEntity(summon);
                                                        context.getItemInHand().shrink(1);
                                                        return net.minecraft.world.InteractionResult.SUCCESS;
                                                }
                                        }
                                        return net.minecraft.world.InteractionResult.SUCCESS;
                                }
                        });

        // 万象の塊魂 (Omniversal Core)
        public static final RegistryObject<Item> OMNIVERSAL_CORE = ITEMS.register("omniversal_core",
                        () -> new Item(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(64)) {
                                @javax.annotation.Nonnull
                                @Override
                                public net.minecraft.network.chat.Component getName(
                                                @javax.annotation.Nonnull ItemStack pStack) {
                                        return DynamicTextHelper.getGradientText(super.getName(pStack).getString());
                                }
                        });

        // 夢見る金属 (Orichalcum)
        public static final RegistryObject<Item> ORICHALCUM = ITEMS.register("orichalcum",
                        () -> new OrichalcumItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(64)));

        public static final RegistryObject<Item> ORICHALCUM_BLADE = ITEMS.register("orichalcum_blade",
                        () -> new OrichalcumBladeItem(ModTiers.ORICHALCUM_TIER, 0, 0, new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> ORICHALCUM_PAXEL = ITEMS.register("orichalcum_paxel",
                        () -> new OrichalcumPaxelItem(ModTiers.ORICHALCUM_TIER, 20.0f, 0, new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> ORICHALCUM_EXPTOOL = ITEMS.register("orichalcum_exptool",
                        () -> new OrichalcumExpToolItem(ModTiers.ORICHALCUM_TIER, 0, 0, new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> ORICHALCUM_HELMET = ITEMS.register("orichalcum_helmet",
                        () -> new OrichalcumArmorItem(ModArmorMaterial.ORICHALCUM, ArmorItem.Type.HELMET, new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ORICHALCUM_CHESTPLATE = ITEMS.register("orichalcum_chestplate",
                        () -> new OrichalcumArmorItem(ModArmorMaterial.ORICHALCUM, ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ORICHALCUM_LEGGINGS = ITEMS.register("orichalcum_leggings",
                        () -> new OrichalcumArmorItem(ModArmorMaterial.ORICHALCUM, ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(ESCHATON).fireResistant()));
        public static final RegistryObject<Item> ORICHALCUM_BOOTS = ITEMS.register("orichalcum_boots",
                        () -> new OrichalcumArmorItem(ModArmorMaterial.ORICHALCUM, ArmorItem.Type.BOOTS, new Item.Properties().rarity(ESCHATON).fireResistant()));

        // オリハルコン光輪
        public static final RegistryObject<Item> ORICHALCUM_HALO_CURIOS = ITEMS.register("orichalcum_halo_curios",
                        () -> new OrichalcumHaloCurioItem(new Item.Properties().rarity(ESCHATON).fireResistant()));

        // 裂核の杖 (Staff of Fissure)
        public static final RegistryObject<Item> STAFF_OF_FISSURE = ITEMS.register("staff_of_fissure",
                        () -> new StaffOfFissureItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));


        // 旋龗の杖 (Staff of Tornado)
        public static final RegistryObject<Item> STAFF_OF_TORNADO = ITEMS.register("staff_of_tornado",
                        () -> new StaffOfTornadoItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));

        // 劫焔の杖 (Staff of Inferno)
        public static final RegistryObject<Item> STAFF_OF_INFERNO = ITEMS.register("staff_of_inferno",
                        () -> new StaffOfInfernoItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));

        // 今回追加したロマン武器・ツール
        public static final RegistryObject<Item> EXCALIPOOR = ITEMS.register("excalipoor",
                        () -> new ExcalipoorItem(Tiers.NETHERITE, 0, -2.4F,
                                        new Item.Properties().rarity(Rarity.EPIC)));

        public static final RegistryObject<Item> EXCALIBUR = ITEMS.register("excalibur",
                        () -> new ExcaliburItem(Tiers.NETHERITE, 0, -2.4F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> ULTIMA_WEAPON = ITEMS.register("ultima_weapon",
                        () -> new UltimaWeaponItem(Tiers.NETHERITE, -2.4F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> HEAVY_IRON_DRILL = ITEMS.register("heavy_iron_drill",
                        () -> new HeavyIronDrillItem(ModTiers.COMPRESSED_IRON, 0, -2.8F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> OMEGA_WEAPON = ITEMS.register("omega_weapon",
                        () -> new OmegaWeaponItem(Tiers.NETHERITE, -2.4F,
                                        new Item.Properties().rarity(ESCHATON).fireResistant()));

        public static final RegistryObject<Item> STAFF_OF_ULTIMA = ITEMS.register("staff_of_ultima",
                        () -> new StaffOfUltimaItem(new Item.Properties().rarity(ESCHATON).fireResistant().stacksTo(1)));

        // クリエイティブタブ
        public static final RegistryObject<CreativeModeTab> COMPRESSED_INFINITY_TAB = CREATIVE_MODE_TABS.register(
                        "compressed_infinity_tab",
                        () -> CreativeModeTab.builder()
                                        .title(net.minecraft.network.chat.Component
                                                        .translatable("itemGroup.compressed_infinity"))
                                        .icon(() -> COMPRESSED_IRONS.get(17).get().getDefaultInstance()) // 18重をアイコンに
                                        .displayItems((params, output) -> {
                                                COMPRESSED_IRONS.forEach(item -> output.accept(item.get()));
                                                COMPRESSED_NETHER_STARS.forEach(item -> output.accept(item.get()));
                                                output.accept(GOD_KILLER.get());
                                                output.accept(FERRUM_OF_EXECUTION.get());
                                                output.accept(ETERNAL_HELMET.get());
                                                output.accept(ETERNAL_CHESTPLATE.get());
                                                output.accept(ETERNAL_LEGGINGS.get());
                                                output.accept(ETERNAL_BOOTS.get());
                                                output.accept(OMNI_TOOL.get());
                                                output.accept(HOMING_BOW.get());
                                                output.accept(HALO_CURIOS.get());
                                                output.accept(VERITAS_CURIOS.get());
                                                output.accept(FUNNEL_CURIOS.get());
                                                output.accept(INFINITY_RING.get());
                                                output.accept(ATHEOS_CORE.get());
                                                output.accept(OMNIVERSAL_CORE.get());
                                                output.accept(STAFF_OF_FISSURE.get());
                                                output.accept(STAFF_OF_TORNADO.get());
                                                output.accept(STAFF_OF_INFERNO.get());
                                                output.accept(STAFF_OF_ULTIMA.get());
                                                output.accept(EXCALIPOOR.get());
                                                output.accept(EXCALIBUR.get());
                                                output.accept(ULTIMA_WEAPON.get());
                                                output.accept(OMEGA_WEAPON.get());
                                                output.accept(HEAVY_IRON_DRILL.get());
                                                output.accept(ModBlocks.IRON_GENERATOR.get());
                                                ModBlocks.COMPRESSED_IRON_GENERATORS.forEach(block -> output.accept(block.get()));
                                                output.accept(ModBlocks.EXCALIPOOR_ALTAR.get());
                                                output.accept(ORICHALCUM.get());
                                                output.accept(ORICHALCUM_BLADE.get());
                                                output.accept(ORICHALCUM_PAXEL.get());
                                                output.accept(ORICHALCUM_HELMET.get());
                                                output.accept(ORICHALCUM_CHESTPLATE.get());
                                                output.accept(ORICHALCUM_LEGGINGS.get());
                                                output.accept(ORICHALCUM_BOOTS.get());
                                                output.accept(ORICHALCUM_HALO_CURIOS.get());
                                                output.accept(ModBlocks.TRANSCENDENTAL_CORE.get());
                                        })
                                        .build());

        public static void register(IEventBus eventBus) {
                ITEMS.register(eventBus);
                CREATIVE_MODE_TABS.register(eventBus);
        }
}
