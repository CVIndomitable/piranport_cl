package com.piranport.combat;

/**
 * 自动模式三态 — 策划决策/副本/14 §H 键总开关 + 数值/05 定稿。
 *
 * <p>三态循环：OFF → AA_ONLY → FULL_AUTO → OFF ...</p>
 *
 * <p><b>语义</b>：
 * <ul>
 *   <li>{@link #OFF} — 全关：无任何自动行为（防空炮/导弹/战斗机均不触发）。</li>
 *   <li>{@link #AA_ONLY} — 仅防空：自动近防炮 + 防空导弹正常运作，战斗机不自动升空。</li>
 *   <li>{@link #FULL_AUTO} — 全自动：自动近防炮 + 防空导弹 + 战斗机自动升空全部启用。</li>
 * </ul>
 * </p>
 *
 * <p>持久化：{@link com.piranport.registry.ModDataComponents#SHIP_AUTO_MODE} 存储序数值
 * （0/1/2）。客户端与服务端均通过该 DataComponent 读写，无需额外同步协议。</p>
 */
public enum AutoModeState {
    /** 全关 — 无自动行为 */
    OFF(0),
    /** 仅防空 — 近防炮 + 防空导弹 */
    AA_ONLY(1),
    /** 全自动 — 近防炮 + 防空导弹 + 战斗机 */
    FULL_AUTO(2);

    private final int id;

    AutoModeState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** 从持久化序数反解枚举值，无效值安全回退 OFF */
    public static AutoModeState fromId(int id) {
        for (AutoModeState s : values()) {
            if (s.id == id) return s;
        }
        return OFF;
    }

    /** 三态循环：OFF → AA_ONLY → FULL_AUTO → OFF ... */
    public AutoModeState next() {
        return switch (this) {
            case OFF -> AA_ONLY;
            case AA_ONLY -> FULL_AUTO;
            case FULL_AUTO -> OFF;
        };
    }

    /** 读取 ItemStack 上存储的自动模式，无组件或无效值时返回 OFF */
    public static AutoModeState fromStack(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return OFF;
        Integer value = stack.get(com.piranport.registry.ModDataComponents.SHIP_AUTO_MODE.get());
        return fromId(value != null ? value : OFF.getId());
    }

    /** 将当前模式写入 ItemStack（不触发网络同步，写入后若需同步须调用 set） */
    public void writeToStack(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.set(com.piranport.registry.ModDataComponents.SHIP_AUTO_MODE.get(), id);
    }
}
