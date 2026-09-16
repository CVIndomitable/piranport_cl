package com.piranport.npc.follower;

import com.piranport.npc.shipgirl.ShipGirlData;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * B 方案：场内临时征召池。
 * <p>教学关固定配给 + 无随从玩家兜底。
 * 初期用硬编码配置，后续改为关卡 JSON 声明（与 enemy_sets 同构）。</p>
 */
public class FollowerSummonPool {
    // 教学关固定舰娘配置
    private static final Map<String, String[]> TUTORIAL_FIXED_SHIPGIRLS = new HashMap<>();

    static {
        // 教学关卡固定配给舰娘 ID
        TUTORIAL_FIXED_SHIPGIRLS.put("t-1", new String[]{"tutorial_destroyer_1"});
        TUTORIAL_FIXED_SHIPGIRLS.put("1-1", new String[]{"tutorial_cruiser_1"});
        TUTORIAL_FIXED_SHIPGIRLS.put("1-2", new String[]{"tutorial_destroyer_1"});
        TUTORIAL_FIXED_SHIPGIRLS.put("1-3", new String[]{"tutorial_cruiser_1", "tutorial_destroyer_1"});
        TUTORIAL_FIXED_SHIPGIRLS.put("1-4", new String[]{"tutorial_cruiser_1"});
    }

    // 兜底征召池（无随从玩家使用）
    private static final String[][] FALLBACK_POOL = {
            {"tutorial_destroyer_1", "tutorial_cruiser_1"}
    };

    private FollowerSummonPool() {}

    /**
     * 获取指定关卡的舰娘列表。
     * 教学关返回固定配置；其他关卡返回兜底池。
     */
    public static String[] getShipGirlsForStage(String stageId) {
        if (TUTORIAL_FIXED_SHIPGIRLS.containsKey(stageId)) {
            return TUTORIAL_FIXED_SHIPGIRLS.get(stageId);
        }
        return FALLBACK_POOL[0];
    }

    /**
     * 获取玩家在指定关卡的可用舰娘列表。
     * 教学关无视玩家养成状态，返回固定配置。
     * 其他关卡：有养成数据返回养成舰娘，否则返回兜底池。
     */
    public static String[] getAvailableShipGirls(Player player, String stageId) {
        if (isTutorialStage(stageId)) {
            return getShipGirlsForStage(stageId);
        }
        // TODO: 后续接入局外养成数据（ShipGirlData）
        // 目前玩家无养成数据，返回兜底池
        return FALLBACK_POOL[0];
    }

    /**
     * 在指定位置生成舰娘实体。
     */
    public static ShipGirlEntity summonShipGirl(ServerLevel level, String shipGirlId, double x, double y, double z) {
        ShipGirlEntity entity = ModEntityTypes.SHIP_GIRL.get().create(level);
        if (entity == null) return null;

        entity.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
        entity.setPersistenceRequired();
        level.addFreshEntity(entity);

        // TODO: 后续根据 shipGirlId 应用 ShipGirlData（皮肤/属性/装备）
        // 目前使用默认属性

        return entity;
    }

    /**
     * 判断是否为教学关卡。
     */
    public static boolean isTutorialStage(String stageId) {
        return TUTORIAL_FIXED_SHIPGIRLS.containsKey(stageId);
    }
}
