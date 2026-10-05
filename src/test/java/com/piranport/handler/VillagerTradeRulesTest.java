package com.piranport.handler;

import com.piranport.config.ModEquipmentConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** 经济/02 交易表规则：源码级检查，避免触发 MC 注册表引导。 */
class VillagerTradeRulesTest {
    private static final Path SRC = Path.of("src/main/java/com/piranport/handler/VillagerTradeHandler.java");
    /** sell(cost, n, result, m, uses, level) */
    private static final Pattern SELL = Pattern.compile(
            "sell\\(([^,]+),\\s*\\d+,\\s*([^,]+),\\s*\\d+,\\s*(\\w+),\\s*\\d\\)");

    @Test
    void trophyTradesRestockLessThanEmeraldTrades() {
        // 补货次数已下沉为终端参数，这里校验默认值仍满足「战利品 < 绿宝石」的设计约束。
        // 直接读 config 而非 VillagerTradeHandler：后者类初始化会拉起 ModItems/BuiltInRegistries，
        // 单测环境未 bootstrap 会抛 ExceptionInInitializerError（见 mc_bootstrap_in_tests）。
        assertTrue(ModEquipmentConfig.VILLAGER_TROPHY_USES.get() < ModEquipmentConfig.VILLAGER_EMERALD_USES.get());
    }

    @Test
    void trophyNeverResultAndNeverSwappedWithEmerald() throws Exception {
        Matcher m = SELL.matcher(Files.readString(SRC));
        int trophyTrades = 0;
        while (m.find()) {
            String cost = m.group(1), result = m.group(2), uses = m.group(3);
            assertFalse(result.contains("HENTAI_TROPHY"), "战利品不得作为交易产出: " + m.group());
            if (cost.contains("HENTAI_TROPHY")) {
                trophyTrades++;
                assertFalse(result.contains("EMERALD"), "战利品不得换绿宝石: " + m.group());
                assertEquals("TROPHY_USES", uses, "战利品交易须用 TROPHY_USES: " + m.group());
            }
        }
        assertTrue(trophyTrades > 0);
    }

    @Test
    void noLongerInjectsIntoVanillaProfessions() throws Exception {
        String src = Files.readString(SRC);
        assertFalse(src.contains("VillagerProfession.FARMER"));
        assertFalse(src.contains("VillagerProfession.TOOLSMITH"));
    }
}
