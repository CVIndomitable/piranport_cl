package com.piranport;

import com.mojang.logging.LogUtils;
import com.piranport.compat.ModCompats;
import com.piranport.config.ModClientConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.config.ModWeaponsConfig;
import com.piranport.config.ModAircraftConfig;
import com.piranport.config.ModShipsConfig;
import com.piranport.config.ModProjectilesConfig;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.recipe.ModBrewingRecipes;
import com.piranport.registry.ModBiomeModifiers;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModCreativeTabs;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMenuTypes;
import com.piranport.registry.ModMobEffects;
import com.piranport.registry.ModArmorMaterials;
import com.piranport.registry.ModAttachmentTypes;
import com.piranport.registry.ModRecipeTypes;
import com.piranport.registry.ModSounds;
import com.piranport.worldgen.ModStructureProcessors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.slf4j.Logger;

/**
 * 皮兰港 (Piran Port) — Minecraft 舰娘模组主入口 (NeoForge 1.21.1)
 *
 * 子系统注册清单（按构造顺序）：
 *   1. ModDataComponents     — 自定义 DataComponent（SHIP_CORE_FUEL 等）
 *   2. ModBlocks             — 方块注册
 *   3. ModItems              — 物品注册
 *   4. ModCreativeTabs       — 创造模式标签页
 *   5. ModMenuTypes          — GUI 菜单类型
 *   6. ModEntityTypes        — 实体类型（飞机/鱼雷/炮弹/导弹等）
 *   7. ModMobEffects         — 药水效果（规避/装填加速/经验加成等）
 *   8. ModBlockEntityTypes   — 方块实体
 *   9. ModRecipeTypes        — 自定义配方与序列化器
 *  10. ModBiomeModifiers     — 生物群系修改器（盐矿/深海）
 *  11. ModStructureProcessors— 结构处理器（废墟劣化/战利品箱）
 *  12. ModArmorMaterials     — 护甲材料
 *  13. ModAttachmentTypes    — 数据附着类型
 *  14. ModCompats             — 第三方模组兼容（女仆/地牢等）
 *
 * 配置文件（COMMON 类型，运行时热重载安全）：
 *   piranport-common.toml       — 通用开关（友伤/爆炸破坏/水面行走等）
 *   piranport-weapons.toml      — 火炮数值（伤害/冷却/散布）
 *   piranport-aircraft.toml     — 飞机数值（伤害/冷却/航速/血量）
 *   piranport-ships.toml        — 舰装数值（护甲/速度/击退抗性）
 *   piranport-projectiles.toml  — 弹药数值（鱼雷/深弹/炸弹/火箭）
 *   piranport-equipment.toml    — 装备数值（声纳/雷达/火控/装甲板）
 *   piranport-client.toml       — 客户端配置（HUD/火控面板位置）
 *
 * 事件处理（通过 @EventBusSubscriber 自动注册）：
 *   CommonEvents    — 实体属性/击杀/方块交互等通用事件
 *   GameEvents      — 服务器 tick/命令注册/拾取等游戏事件
 *   ClientEvents    — 客户端按键/渲染事件
 *   PlayerTickHandler       — 玩家 tick（变身/燃料/声纳/自动战斗）
 *   PlayerConnectionHandler — 玩家登录/登出/死亡/维度切换
 *   PlayerAircraftHelper    — 飞机召回工具方法
 *   VillagerTradeHandler    — 村民交易注入
 */
@Mod(PiranPort.MOD_ID)
public class PiranPort {
    public static final String MOD_ID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
        ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ModStructureProcessors.STRUCTURE_PROCESSORS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        ModAttachmentTypes.ATTACHMENT_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::registerBrewingRecipes);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModWeaponsConfig.SPEC, "piranport-weapons.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModAircraftConfig.SPEC, "piranport-aircraft.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModShipsConfig.SPEC, "piranport-ships.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModProjectilesConfig.SPEC, "piranport-projectiles.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModEquipmentConfig.SPEC, "piranport-equipment.toml");
        modContainer.registerConfig(ModConfig.Type.COMMON, ModArtilleryConfig.SPEC, "piranport-artillery.toml");

        ModCompats.initialize(modEventBus);

        LOGGER.info("Piran Port mod initialized!");
    }

    private void registerBrewingRecipes(final RegisterBrewingRecipesEvent event) {
        ModBrewingRecipes.register(event.getBuilder());
    }
}
