package com.piranport.dungeon.event;

import com.piranport.PiranPort;
import com.piranport.dungeon.VictoryEvaluator;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.DeepOceanProjectileEntity;
import com.piranport.entity.TorpedoEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** 《副本/12》夺旗：圈内玩家被敌方弹药命中，进度倒退 1%。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class DungeonCaptureFlagHandler {
    private DungeonCaptureFlagHandler() {}

    @SubscribeEvent
    public static void onPlayerHitByEnemyOrdnance(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!isEnemyOrdnance(event.getSource().getDirectEntity())) return;
        DungeonInstance instance = DungeonInstanceManager.get(level).getInstanceForPlayer(player);
        if (instance == null) return;
        VictoryEvaluator.onFlagPlayerHit(level, instance, player);
    }

    private static boolean isEnemyOrdnance(Entity direct) {
        if (!(direct instanceof DeepOceanProjectileEntity)
                && !(direct instanceof AerialBombEntity)
                && !(direct instanceof TorpedoEntity)) return false;
        Entity owner = ((Projectile) direct).getOwner();
        // 玩家自己的弹药不计入：己方飞机挂的弹同样归玩家。
        if (owner instanceof Player) return false;
        return !(owner instanceof AircraftEntity aircraft && aircraft.getOwner() instanceof Player);
    }
}
