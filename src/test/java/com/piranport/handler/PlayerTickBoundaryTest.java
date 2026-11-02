package com.piranport.handler;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/** 防止模块重新自行订阅 Tick、绕过主入口或引入客户端依赖。 */
class PlayerTickBoundaryTest {
    private static final List<Class<?>> MODULES = List.of(
            PlayerFuelTick.class, PlayerLoadoutTick.class, PlayerEquipmentTick.class,
            PlayerReconTick.class, PlayerDetectionTick.class, PlayerAutoCombatTick.class);
    private static final JavaClasses CLASSES = new ClassFileImporter().importClasses(MODULES);

    @Test
    void modulesStayInternalAndCannotSubscribeIndependently() {
        classes().should().notBePublic().check(CLASSES);
        noClasses().should().beAnnotatedWith(EventBusSubscriber.class).check(CLASSES);
        noMethods().should().beAnnotatedWith(SubscribeEvent.class).check(CLASSES);
    }

    @Test
    void modulesDoNotDependOnClientOnlyClasses() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "net.minecraft.client..", "net.neoforged.neoforge.client..", "com.piranport..client..")
                .check(CLASSES);
    }

    @Test
    void modulesDoNotCallCoordinatorOrEachOther() {
        noClasses().should().dependOnClassesThat().haveFullyQualifiedName(PlayerTickHandler.class.getName())
                .check(CLASSES);
        for (Class<?> source : MODULES) {
            for (Class<?> target : MODULES) {
                if (source == target) continue;
                noClasses().that().haveFullyQualifiedName(source.getName())
                        .should().dependOnClassesThat().haveFullyQualifiedName(target.getName())
                        .check(CLASSES);
            }
        }
    }
}
