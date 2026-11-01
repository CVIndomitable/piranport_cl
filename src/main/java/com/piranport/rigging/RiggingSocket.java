package com.piranport.rigging;

/**
 * 玩家舰装的固定挂点。
 *
 * <p>布局（2026-11-01 不挠确认）：
 * <ul>
 *   <li>左/右腿：各 1 个鱼雷发射器挂点</li>
 *   <li>左舷：2 个挂点，主炮或反舰导弹/火箭弹</li>
 *   <li>右舷：2 个挂点，主炮或反舰导弹/火箭弹；<b>只要快捷栏里有飞机，右舷整侧换成飞行甲板</b></li>
 *   <li>后背：2 个挂点，深弹或防空导弹；上方是烟囱与桅杆（程序化几何）</li>
 *   <li>甲板：2 个飞机位，仅在有飞机时存在</li>
 * </ul>
 *
 * <p>枚举顺序即网络序号，新增挂点只能追加在末尾。
 */
public enum RiggingSocket {
    LEG_LEFT,
    LEG_RIGHT,
    SIDE_LEFT_1,
    SIDE_LEFT_2,
    SIDE_RIGHT_1,
    SIDE_RIGHT_2,
    DECK_1,
    DECK_2,
    BACK_1,
    BACK_2;

    private static final RiggingSocket[] VALUES = values();

    public static RiggingSocket byId(int id) {
        if (id < 0 || id >= VALUES.length) {
            throw new IllegalArgumentException("Unknown rigging socket id: " + id);
        }
        return VALUES[id];
    }

    public static int count() {
        return VALUES.length;
    }
}
