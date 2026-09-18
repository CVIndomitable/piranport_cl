package com.piranport.combat.cannon;

/**
 * 延迟任务捕获实例身份；UUID、维度键和物品类型相同都不代表仍是原来的一次射击。
 * 保留调度时玩家与世界实例，重生或跨世界后任务立即失效。
 */
public record SalvoContext(Object player, Object level, Object heldWeapon) {
    public boolean isValid(Object currentPlayer, Object currentLevel, Object currentHeldWeapon,
            boolean alive, boolean spectator) {
        return alive && !spectator && player == currentPlayer && level == currentLevel
                && heldWeapon == currentHeldWeapon;
    }
}
