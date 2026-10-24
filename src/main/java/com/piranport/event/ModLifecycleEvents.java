package com.piranport.event;

import com.piranport.PiranPort;

import com.piranport.block.entity.CookingPotBlockEntity;
import com.piranport.block.entity.CuttingBoardBlockEntity;
import com.piranport.block.entity.StoneMillBlockEntity;
import com.piranport.block.entity.YubariWaterBucketBlockEntity;
import com.piranport.entity.FloatingTargetEntity;
import com.piranport.entity.LowTierDestroyerEntity;
import com.piranport.entity.GoldencatcatEntity;
import com.piranport.npc.deepocean.DeepOceanArmoredCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanBossEntity;
import com.piranport.npc.deepocean.DeepOceanBattleCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanBattleshipEntity;
import com.piranport.npc.deepocean.DeepOceanCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanAirDefenseDestroyerEntity;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
import com.piranport.npc.deepocean.DeepOceanFlagshipEntity;
import com.piranport.npc.deepocean.DeepOceanHeavyCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanLightCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import com.piranport.npc.deepocean.DeepOceanShinanoEntity;
import com.piranport.npc.deepocean.DeepOceanSupplyEntity;
import com.piranport.npc.deepocean.DeepOceanTaihouEntity;
import com.piranport.npc.deepocean.DeepOceanTorpedoCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanTransportEntity;
import com.piranport.npc.deepocean.DeepOceanZuikakuEntity;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModEntityTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * 模组生命周期事件 — 注册 Capability 和实体属性。
 *
 * <p>本类监听 NeoForge Mod EventBus 的初始化事件:
 * <ul>
 *   <li>{@link RegisterCapabilitiesEvent} — 注册方块实体的物品/流体容器能力</li>
 *   <li>{@link EntityAttributeCreationEvent} — 注册实体的默认属性(血量/速度等)</li>
 * </ul>
 *
 * <h2>重构历史</h2>
 * <p>原 {@code CommonModEvents} 类,重命名为 {@code ModLifecycleEvents} 以明确职责范围。
 *
 * @see com.piranport.registry.ModBlockEntityTypes
 * @see com.piranport.registry.ModEntityTypes
 * @since 1.0.0
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ModLifecycleEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.STONE_MILL.get(),
                StoneMillBlockEntity::getItemHandler
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.COOKING_POT.get(),
                CookingPotBlockEntity::getItemHandler
        );
        // Phase 30: single-slot handler for hopper input; extract is always EMPTY (output drops to world)
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.CUTTING_BOARD.get(),
                CuttingBoardBlockEntity::getItemHandler
        );
        // 装填设施：方块实体已按方向拆分输入/输出（顶部发射器、侧面弹药、底部产出），
        // 但此前没有登记 Capability，漏斗/管道实际接不上。
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntityTypes.RELOAD_FACILITY.get(),
                com.piranport.block.entity.ReloadFacilityBlockEntity::getItemHandler
        );
        // 夕张水桶：管道/水槽的无限水源
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntityTypes.YUBARI_WATER_BUCKET.get(),
                YubariWaterBucketBlockEntity::getFluidHandler
        );
    }

    @SubscribeEvent
    public static void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FLOATING_TARGET.get(),
                FloatingTargetEntity.createAttributes().build());
        event.put(ModEntityTypes.PINGHAI.get(),
                com.piranport.entity.PinghaiEntity.createAttributes().build());
        event.put(ModEntityTypes.LOW_TIER_DESTROYER.get(),
                LowTierDestroyerEntity.createAttributes().build());
        // 深海 NPC 实体
        event.put(ModEntityTypes.DEEP_OCEAN_SUPPLY.get(),
                DeepOceanSupplyEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_DESTROYER.get(),
                DeepOceanDestroyerEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(),
                DeepOceanLightCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HEAVY_CRUISER.get(),
                DeepOceanHeavyCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_BATTLE_CRUISER.get(),
                DeepOceanBattleCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_BATTLESHIP.get(),
                DeepOceanBattleshipEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_LIGHT_CARRIER.get(),
                DeepOceanLightCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_CARRIER.get(),
                DeepOceanCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_SUBMARINE.get(),
                DeepOceanSubmarineEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_TRANSPORT.get(),
                DeepOceanTransportEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_ARMORED_CARRIER.get(),
                DeepOceanArmoredCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_TORPEDO_CRUISER.get(),
                DeepOceanTorpedoCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_TAIHOU.get(),
                DeepOceanTaihouEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_SHINANO.get(),
                DeepOceanShinanoEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_ZUIKAKU.get(),
                DeepOceanZuikakuEntity.createAttributes().build());
        // 高级档复用低级的实体类，属性同样由 JSON 覆盖；旗舰类继承战列舰的属性表
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_DESTROYER.get(),
                DeepOceanDestroyerEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_AIR_DEFENSE_DESTROYER.get(),
                DeepOceanAirDefenseDestroyerEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_AIR_DEFENSE_DESTROYER.get(),
                DeepOceanAirDefenseDestroyerEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_LIGHT_CRUISER.get(),
                DeepOceanLightCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_HEAVY_CRUISER.get(),
                DeepOceanHeavyCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_BATTLE_CRUISER.get(),
                DeepOceanBattleCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_BATTLESHIP.get(),
                DeepOceanBattleshipEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_LIGHT_CARRIER.get(),
                DeepOceanLightCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_CARRIER.get(),
                DeepOceanCarrierEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_TORPEDO_CRUISER.get(),
                DeepOceanTorpedoCruiserEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_HIGH_SUBMARINE.get(),
                DeepOceanSubmarineEntity.createAttributes().build());
        event.put(ModEntityTypes.DEEP_OCEAN_FLAGSHIP.get(),
                DeepOceanFlagshipEntity.createAttributes().build());
        // 舰娘 NPC
        event.put(ModEntityTypes.SHIP_GIRL.get(),
                ShipGirlEntity.createAttributes().build());
        // 副本 Boss 与活动训练靶也必须注册属性，否则实体可生成但客户端会报无属性。
        event.put(ModEntityTypes.DEEP_OCEAN_BOSS.get(),
                DeepOceanBossEntity.createAttributes().build());
        event.put(ModEntityTypes.GOLDENCATCAT.get(),
                GoldencatcatEntity.createAttributes().build());
    }
}
