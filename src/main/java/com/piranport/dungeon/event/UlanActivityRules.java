package com.piranport.dungeon.event;

import com.piranport.registry.ModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * 《副本/18》ulan 彩蛋：按地球日（UTC）统计玩家当天第几次用新钥匙进本。
 *
 * <p>计数只在钥匙第一次成功进本时调用一次；同一实例的死亡重进、记录点返场不再计数。
 * 第 1–3 次生成补给苏赫巴托尔，第 4 次及以后生成航空战列舰苏赫巴托尔。
 * 计数按玩家，不按服务器、不按钥匙；跨日归零。</p>
 */
public final class UlanActivityRules {
    private UlanActivityRules() {}

    public static final String VARIANT_SUPPLY = "supply";
    public static final String VARIANT_AVIATION = "aviation";

    /** 每次新实例首次进本调用一次，返回本实例锁定的形态。 */
    public static String pickVariant(ServerPlayer player) {
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        long storedDay = player.getData(ModAttachmentTypes.ULAN_DAILY_DAY);
        int count = storedDay == today ? player.getData(ModAttachmentTypes.ULAN_DAILY_COUNT) : 0;
        count++;
        player.setData(ModAttachmentTypes.ULAN_DAILY_DAY, today);
        player.setData(ModAttachmentTypes.ULAN_DAILY_COUNT, count);
        return count <= 3 ? VARIANT_SUPPLY : VARIANT_AVIATION;
    }
}
