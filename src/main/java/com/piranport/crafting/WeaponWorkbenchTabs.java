package com.piranport.crafting;

/**
 * 武器制造台页签的共享常量（common 侧，服务端与客户端共用）。
 *
 * <p>页签数量曾经分散成三份字面量：BlockEntity 5、Menu 5、Screen 6，导致第 6 个「装备」页
 * 被服务端 {@code setSelectedTab} 直接拒绝、整页配方玩不到。现在 BE / Menu / Screen
 * 全部引用这里的 {@link #TAB_COUNT}，添加页签时只改这一处。
 *
 * <p>新增页签时还要同步 {@code WeaponWorkbenchScreen} 的 {@code TAB_TOOLTIPS} 与
 * {@code tabIcons} 两个数组，并在 {@code WeaponWorkbenchRecipeRegistry#registerAll}
 * 里注册对应 tab 的配方。
 */
public final class WeaponWorkbenchTabs {
    /** 页签总数（合法索引 0..TAB_COUNT-1）。 */
    public static final int TAB_COUNT = 6;

    private WeaponWorkbenchTabs() {}
}
