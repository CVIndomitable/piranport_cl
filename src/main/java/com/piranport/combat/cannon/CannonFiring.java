package com.piranport.combat.cannon;

import com.piranport.combat.data.WeaponState;
import com.piranport.component.LoadedAmmo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import static com.piranport.combat.cannon.CannonStats.getBarrelCount;
import static com.piranport.combat.cannon.CannonStats.isCannonDamaged;
import static com.piranport.combat.cannon.CannonAmmoRules.isHEShell;
import static com.piranport.combat.cannon.CannonAmmoRules.isVTShell;
import static com.piranport.combat.cannon.CannonAmmoRules.isType3Shell;
import static com.piranport.combat.cannon.CannonAmmoRules.isLoadedCannonAmmoValid;
import static com.piranport.combat.cannon.CannonAmmoRules.createAmmoStack;
import static com.piranport.combat.cannon.CannonProjectiles.fireCannonSalvo;

/** 火炮射击事务：校验已装弹状态、派发炮弹并进入下一轮装填。 */
public final class CannonFiring {
    private CannonFiring() {}

    public static boolean fireLoadedCannon(Level level, Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, CannonAim aim) {
        if (level.isClientSide() || !player.isAlive() || player.isSpectator()) return false;
        if (new WeaponState(weapon).isOnCooldown(level.getGameTime())) return false;
        int barrelCount = getBarrelCount(weapon, level);

        if (isCannonDamaged(weapon, level)) {
            player.displayClientMessage(Component.translatable("message.piranport.cannon_damaged"), true);
            return true;
        }

        WeaponState ws = new WeaponState(weapon);
        LoadedAmmo loaded = ws.getLoadedAmmo();
        if (!isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level)) {
            ws.clearLoadedAmmo();
            // 火炮固定手动装填：未装填时射击只提示，不能隐式启动读条。
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        ItemStack shellForRender = createAmmoStack(loaded.ammoItemId());
        if (shellForRender.isEmpty()) {
            ws.clearLoadedAmmo();
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        boolean isType3 = isType3Shell(shellForRender);
        boolean isVT = isVTShell(shellForRender);
        boolean isHE = isHEShell(shellForRender) || isVT;
        var result = fireCannonSalvo(level, player, weapon, shellForRender, loaded.count(),
                isType3, isVT, isHE, aim);
        if (result.shots() == 0) {
            return true;
        }
        int remaining = loaded.count() - result.shots();
        if (remaining > 0) ws.setLoadedAmmo(remaining, loaded.ammoItemId());
        else ws.clearLoadedAmmo();

        // 决策/数值/05 §定稿修订 #3：大口径主炮开火触发 5 秒防空静默窗口
        com.piranport.combat.AASilenceManager.onCannonFire(player, shellForRender);

        // 开火后不自动进入下一轮装填；下一轮只能由手动 R/右键流程启动。
        com.piranport.debug.PiranPortDebug.event(
                "Fire cannon | weapon={} ammo={} barrels={}",
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                loaded.ammoItemId(),
                barrelCount);
        return true;
    }
}
