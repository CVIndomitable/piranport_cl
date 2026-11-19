package com.piranport.crafting;

import com.piranport.component.EquipmentTier;

import java.util.List;

import static com.piranport.component.EquipmentTier.*;

/** 制造台和专属蓝图共用《数值/09》的分档；注册蓝图时物品实例尚不可用。 */
public record CraftableAircraft(String id, EquipmentTier tier) {
    // 制造台存档保存配方下标。保留原有 21 个位置，新配方从表尾追加。
    public static final List<CraftableAircraft> ALL = List.of(
            new CraftableAircraft("swordfish_torpedo", INITIAL),
            new CraftableAircraft("swordfish_asw", INITIAL),
            new CraftableAircraft("tbf_torpedo", IMPROVED),
            new CraftableAircraft("tbf_asw", IMPROVED),
            new CraftableAircraft("tenzan_torpedo", IMPROVED),
            new CraftableAircraft("type97_torpedo", STANDARD),
            new CraftableAircraft("petrel_bomber", INITIAL),
            new CraftableAircraft("type99_dive_bomber", INITIAL),
            new CraftableAircraft("sbd_dauntless", STANDARD),
            new CraftableAircraft("firefly_as_mk5", STANDARD),
            new CraftableAircraft("suisei_bomber", IMPROVED),
            new CraftableAircraft("seiun_kai_bomber", ADVANCED),
            new CraftableAircraft("b25_bomber", ADVANCED),
            new CraftableAircraft("f6f_hellcat_rocket", IMPROVED),
            new CraftableAircraft("seafire", STANDARD),
            new CraftableAircraft("zero_model52", STANDARD),
            new CraftableAircraft("f4f_wildcat", STANDARD),
            new CraftableAircraft("f4u_corsair", IMPROVED),
            new CraftableAircraft("type0_recon", INITIAL),
            new CraftableAircraft("c1_recon", STANDARD),
            new CraftableAircraft("saiun_recon", IMPROVED),
            new CraftableAircraft("type96_fighter", INITIAL),
            new CraftableAircraft("f2a_buffalo", INITIAL),
            new CraftableAircraft("barracuda", STANDARD),
            new CraftableAircraft("ju87c", STANDARD),
            new CraftableAircraft("btd_destroyer", IMPROVED),
            new CraftableAircraft("seafang", ADVANCED),
            new CraftableAircraft("wyvern", ADVANCED),
            new CraftableAircraft("xf5u", ADVANCED),
            new CraftableAircraft("f4u7", ADVANCED),
            new CraftableAircraft("skyraider", ADVANCED),
            new CraftableAircraft("reppu", ADVANCED),
            new CraftableAircraft("tenga", ADVANCED),
            new CraftableAircraft("ryusei", ADVANCED));

    public boolean needsBlueprint() {
        return tier == IMPROVED || tier == ADVANCED;
    }
}
