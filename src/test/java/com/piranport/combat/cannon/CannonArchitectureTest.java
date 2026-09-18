package com.piranport.combat.cannon;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CannonArchitectureTest {
    @Test
    void cannonComponentsMustNotCallBackIntoTheWeaponFacade() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.piranport.combat.cannon");
        noClasses().should().dependOnClassesThat()
                .haveFullyQualifiedName("com.piranport.item.ShipCoreCombat")
                .because("火炮组件应拥有规则，入口只能向组件委托")
                .check(classes);
    }

    @Test
    void ammoRulesAndStatsMustNotDependOnReloadingOrFiring() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.piranport.combat.cannon");
        noClasses().that().haveSimpleName("CannonAmmoRules")
                .or().haveSimpleName("CannonStats")
                .should().dependOnClassesThat().haveNameMatching(".*\\.Cannon(Reloading|Firing)")
                .because("弹药与数值是下层规则，不能反向驱动装填或射击")
                .check(classes);
    }
}
