package com.piranport.aviation;

import com.piranport.component.AircraftInfo;

/**
 * 舰载机出击准备判定（纯函数，便于单测）。
 *
 * <p>与 R 键装填读条共用同一判定：读条前的"已准备"提示、读条到期的结算条件、
 * 放飞前的校验都必须给出同一个答案，否则会出现"提示已准备却放飞被拦"这类自相矛盾。
 * 因此这里不依赖任何 Minecraft 运行时状态（不读背包、不读注册表），只吃三个不可变数据。
 */
public final class AircraftSortieReadiness {

    private AircraftSortieReadiness() {}

    /**
     * 是否已完成出击准备。
     *
     * <p>战斗机/侦察机没有对海挂载概念（{@link AircraftDefinition#requiresPayload()} 为 false），
     * 只看燃油；攻击机还必须有挂载。
     */
    public static boolean isReady(AircraftInfo info, AircraftDefinition definition,
                                  ResolvedAircraftStats stats) {
        boolean fuelReady = info.currentFuel() >= stats.fuelCapacity();
        boolean payloadReady = !definition.requiresPayload() || info.payloadLoaded();
        return fuelReady && payloadReady;
    }

    /** 是否还缺燃油（缺 1 个航空燃料才能加满）。 */
    public static boolean needsFuel(AircraftInfo info, ResolvedAircraftStats stats) {
        return info.currentFuel() < stats.fuelCapacity();
    }

    /** 是否还缺对海挂载物（无挂载概念的机型恒为 false）。 */
    public static boolean needsPayload(AircraftDefinition definition, AircraftInfo info) {
        return definition.requiresPayload() && !info.payloadLoaded();
    }
}
