package com.piranport.aviation.combat;

import com.piranport.aviation.AircraftClass;
import com.piranport.aviation.AircraftDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AircraftCombatServiceTest {
    @AfterEach
    void restoreRegistry() { AttackStrategyRegistry.resetToBuiltIns(); }

    @Test
    void mapsEveryProfileToStableStrategyId() {
        for (AircraftDefinition.AttackProfile profile : AircraftDefinition.AttackProfile.values()) {
            AircraftDefinition definition = definition(profile);
            assertEquals(profile.id(), AircraftCombatService.strategyIdFor(definition));
            assertTrue(AircraftCombatService.supports(definition, profile.id().toUpperCase()));
        }
    }

    @Test
    void reconWithoutProfileFallsBackToNoneStrategy() {
        AircraftDefinition recon = new AircraftDefinition(
                ResourceLocation.parse("piranport:test_recon"),
                AircraftClass.RECON, null, null, "recon",
                100, 0, 0.0f, 1.0f, 1, 20, 5, null);

        assertSame(AttackStrategyRegistry.forProfile(AircraftDefinition.AttackProfile.NONE),
                AircraftCombatService.strategyFor(recon));
        assertEquals("none", AircraftCombatService.strategyIdFor(recon));
    }

    @Test
    void customStrategyCanReplaceStableId() {
        AttackStrategy custom = strategy("gun", AircraftDefinition.AttackProfile.GUN, ctx -> {});
        AttackStrategyRegistry.register(custom);
        assertSame(custom, AttackStrategyRegistry.byId(" GUN ").orElseThrow());
        assertSame(custom, AircraftCombatService.strategyFor(definition(AircraftDefinition.AttackProfile.GUN)));
    }

    @Test
    void registeredStrategyControlsExecutionThroughService() {
        AtomicBoolean executed = new AtomicBoolean();
        AttackStrategyRegistry.register(strategy("gun", AircraftDefinition.AttackProfile.GUN,
                ctx -> executed.set(true)));

        // 使用包级执行 seam，避免为纯分发测试启动 Minecraft 世界。
        AircraftCombatService.executeAttacking(
                definition(AircraftDefinition.AttackProfile.GUN), null, null);
        assertTrue(executed.get());
    }

    @Test
    void unknownStrategyIsRejectedWithoutChangingRuntimePath() {
        AircraftDefinition definition = definition(AircraftDefinition.AttackProfile.TORPEDO);
        assertFalse(AircraftCombatService.supports(definition, "missing"));
        AttackStrategy missing = new AttackStrategy() {
            @Override public String id() { return "missing"; }
            @Override public void execute(@Nullable AircraftAttackContext context,
                                          @Nullable Player owner) {}
            @Override public void executeAutonomous(@Nullable AircraftAttackContext context) {}
        };
        assertThrows(IllegalArgumentException.class, missing::profile);
    }

    private static AttackStrategy strategy(String id, AircraftDefinition.AttackProfile profile,
                                           java.util.function.Consumer<AircraftAttackContext> action) {
        return new AttackStrategy() {
            @Override public String id() { return id; }
            @Override public AircraftDefinition.AttackProfile profile() { return profile; }
            @Override public void execute(@Nullable AircraftAttackContext context, @Nullable Player owner) {
                action.accept(context);
            }
            @Override public void executeAutonomous(@Nullable AircraftAttackContext context) {}
        };
    }

    private static AircraftDefinition definition(AircraftDefinition.AttackProfile profile) {
        AircraftClass type = switch (profile) {
            case GUN -> AircraftClass.FIGHTER;
            case ROCKET -> AircraftClass.ROCKET_FIGHTER;
            case DIVE_BOMB -> AircraftClass.DIVE_BOMBER;
            case LEVEL_BOMB -> AircraftClass.LEVEL_BOMBER;
            case TORPEDO -> AircraftClass.TORPEDO_BOMBER;
            case DEPTH_CHARGE -> AircraftClass.ASW;
            case NONE -> AircraftClass.FIGHTER;
        };
        ResourceLocation payload = switch (type) {
            case TORPEDO_BOMBER -> ResourceLocation.parse("piranport:aerial_torpedo");
            case DIVE_BOMBER, LEVEL_BOMBER -> ResourceLocation.parse("piranport:aerial_bomb");
            case ASW -> ResourceLocation.parse("piranport:depth_charge");
            default -> null;
        };
        String visual = switch (type) {
            case FIGHTER -> "fighter";
            case ROCKET_FIGHTER -> "rocket_fighter";
            case DIVE_BOMBER -> "dive_bomber";
            case LEVEL_BOMBER -> "level_bomber";
            case TORPEDO_BOMBER -> "torpedo_bomber";
            case ASW -> "asw";
            case RECON -> "recon"; // 当前 profile 不会派生 RECON，仅为穷尽机种
        };
        return new AircraftDefinition(ResourceLocation.parse("piranport:test_" + profile.id()),
                type, profile, payload, visual, 100, 8, 4.0f, 1.0f, 1, 20, 5, null);
    }
}
