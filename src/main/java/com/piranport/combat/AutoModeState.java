package com.piranport.combat;

import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** 数值/05 最终定稿：H 键是自动近防炮、防空导弹和战斗机共用的二态总开关。 */
public enum AutoModeState {
    OFF(0),
    ON(1);

    private final int id;

    AutoModeState(int id) { this.id = id; }

    public int getId() { return id; }

    /** 旧的仅防空和全自动存档均归入开启；无效值安全关闭。 */
    public static AutoModeState fromId(int id) {
        return id == 1 || id == 2 ? ON : OFF;
    }

    public AutoModeState next() { return this == OFF ? ON : OFF; }

    /** 明确保存的关闭状态优先，旧布尔标记不能再次把它打开。 */
    public static AutoModeState resolve(Integer currentId, boolean legacyEnabled) {
        return currentId != null ? fromId(currentId) : legacyEnabled ? ON : OFF;
    }

    public static AutoModeState fromStack(ItemStack stack) {
        if (stack.isEmpty()) return OFF;
        return resolve(stack.get(ModDataComponents.SHIP_AUTO_MODE.get()),
                stack.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false));
    }

    public void writeToStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponents.SHIP_AUTO_MODE.get(), id);
        stack.set(ModDataComponents.SHIP_AUTO_LAUNCH.get(), this == ON);
    }
}
