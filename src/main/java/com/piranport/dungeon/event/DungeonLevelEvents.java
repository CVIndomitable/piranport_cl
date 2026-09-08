package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * 整合版 §3.3：副本维度死亡总是不掉落（无论世界规则设置如何）。
 *
 * <p>在 LevelEvent.Load 监听中，对 dungeon 维度强制设置 KEEP_INVENTORY=true，
 * 作为 {@code onPlayerDeath} 取消事件之外的第二道兜底——若有人误改优先级或未来重构移除
 * setCanceled，第二道防线仍能保证不掉落。</p>
 *
 * <p>PlayerChangedDimensionEvent 在 P2-B 阶段统一加（玩家离开副本维度时恢复 false）。</p>
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class DungeonLevelEvents {

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (!serverLevel.dimension().equals(DungeonEventHandler.DUNGEON_DIMENSION)) return;

        serverLevel.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, serverLevel.getServer());
        PiranPort.LOGGER.info("Dungeon level loaded — KEEP_INVENTORY rule forced to true");
    }
}