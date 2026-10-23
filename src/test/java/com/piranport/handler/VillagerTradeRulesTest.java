package com.piranport.handler;

import com.piranport.config.ModEquipmentConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** 经济/02 交易表规则：源码级检查，避免触发 MC 注册表引导。 */
class VillagerTradeRulesTest {
    private static final Path SRC = Path.of("src/main/java/com/piranport/handler/VillagerTradeHandler.java");

    @Test
    void trophyTradesRestockLessThanEmeraldTrades() {
        // 补货次数已下沉为终端参数，这里校验默认值仍满足「战利品 < 绿宝石」的设计约束。
        // 直接读 config 而非 VillagerTradeHandler：后者类初始化会拉起 ModItems/BuiltInRegistries，
        // 单测环境未 bootstrap 会抛 ExceptionInInitializerError（见 mc_bootstrap_in_tests）。
        assertTrue(ModEquipmentConfig.VILLAGER_TROPHY_USES.get() < ModEquipmentConfig.VILLAGER_EMERALD_USES.get());
    }

    @Test
    void trophyNeverResultAndNeverSwappedWithEmerald() throws Exception {
        int trophyTrades = 0;
        for (String call : tradeCalls(Files.readString(SRC))) {
            // 交易表现在统一走 sellItem / sellRandomItem，战利品只能出现在第一个参数（支付侧）。
            boolean trophyCost = call.startsWith("sellItem(\"piranport:hentai_trophy\"")
                    || call.startsWith("sellRandomItem(\"piranport:hentai_trophy\"");
            if (!trophyCost) {
                assertFalse(call.contains("hentai_trophy"), "战利品不得作为交易产出: " + call);
                continue;
            }
            trophyTrades++;
            assertFalse(call.toLowerCase(Locale.ROOT).contains("emerald"),
                    "战利品不得换绿宝石: " + call);
            assertTrue(call.contains("TROPHY_USES"), "战利品交易须用 TROPHY_USES: " + call);
        }
        assertTrue(trophyTrades > 0);
    }

    /**
     * 抽取源码中的交易构造调用，保留完整括号（sellRandomItem 里嵌套 pool(...)）。
     * 字符串字面量里的括号不参与配对，否则商品 ID 含括号时会截断。
     */
    private static List<String> tradeCalls(String src) {
        List<String> calls = new ArrayList<>();
        for (String name : new String[]{"sellItem(", "sellRandomItem(", "sellRandom("}) {
            int from = 0;
            while (true) {
                int start = src.indexOf(name, from);
                if (start < 0) break;
                int depth = 0;
                int i = start + name.length() - 1;
                for (; i < src.length(); i++) {
                    char c = src.charAt(i);
                    if (c == '"') {
                        i++;
                        while (i < src.length() && src.charAt(i) != '"') {
                            i += src.charAt(i) == '\\' ? 2 : 1;
                        }
                    } else if (c == '(') {
                        depth++;
                    } else if (c == ')' && --depth == 0) {
                        break;
                    }
                }
                calls.add(src.substring(start, Math.min(i + 1, src.length())));
                from = i + 1;
            }
        }
        return calls;
    }

    @Test
    void noLongerInjectsIntoVanillaProfessions() throws Exception {
        String src = Files.readString(SRC);
        assertFalse(src.contains("VillagerProfession.FARMER"));
        assertFalse(src.contains("VillagerProfession.TOOLSMITH"));
    }
}
