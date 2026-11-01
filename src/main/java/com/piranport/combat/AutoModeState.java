package com.piranport.combat;

import com.piranport.registry.ModDataComponents;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.item.ItemStack;

/**
 * 数值/05 最终定稿：H 键是自动近防炮、防空导弹和战斗机共用的二态总开关。
 *
 * <p>2026-11-01 决定：新产生的舰装核心默认开启自动模式。默认值由 {@code WeaponItems}
 * 写在 4 种核心的 {@code Item.Properties} 上，而不是改 {@link #resolve} 的兜底方向。</p>
 *
 * <p>旧档兼容：MC 1.21.1 反序列化 ItemStack 时以“当前物品默认组件”为 prototype，
 * 旧核心缺失的组件会继承新的默认值。因此 {@link #fromStack} 先读组件补丁：旧版本按 H
 * 只写 {@code SHIP_AUTO_LAUNCH}，玩家显式关掉的核心优先保持关闭，不会被升级静默打开。</p>
 */
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
        // WHY 先查补丁而不是直接 get：get 会回落到物品默认组件，分不清“存档里明确写过”
        // 和“没写过、继承了新默认值”。旧版本 H 键只写 SHIP_AUTO_LAUNCH，把它的显式值
        // 视为旧核心的真实意图保留；只有从未写过状态的核心才走新的默认值（ON）。
        DataComponentPatch patch = stack.getComponentsPatch();
        Optional<? extends Integer> explicitMode = patch.get(ModDataComponents.SHIP_AUTO_MODE.get());
        if (explicitMode != null) {
            // 补丁里显式移除（Optional.empty）按关闭处理。
            return explicitMode.isPresent() ? fromId(explicitMode.get()) : OFF;
        }
        Optional<? extends Boolean> explicitLegacy = patch.get(ModDataComponents.SHIP_AUTO_LAUNCH.get());
        if (explicitLegacy != null) {
            return Boolean.TRUE.equals(explicitLegacy.orElse(null)) ? ON : OFF;
        }
        return resolve(stack.get(ModDataComponents.SHIP_AUTO_MODE.get()),
                stack.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false));
    }

    public void writeToStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        stack.set(ModDataComponents.SHIP_AUTO_MODE.get(), id);
        stack.set(ModDataComponents.SHIP_AUTO_LAUNCH.get(), this == ON);
    }
}
