package com.piranport.combat.cannon;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    @Test
    void normalCannonCallersMustUseTheSharedProjectileBoundary() {
        var classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.piranport");

        // CannonFireService/CannonProjectileFactory are the only construction boundary. The
        // entity registration constructor has a different signature and is intentionally allowed.
        noClasses().that().resideOutsideOfPackage("..combat.cannon.fire..")
                .should().callConstructor(CannonProjectileEntity.class,
                        Level.class, LivingEntity.class, ItemStack.class,
                        float.class, boolean.class, float.class)
                .because("常规炮弹必须从统一发射服务/工厂创建，避免玩家、女仆和 NPC 分叉")
                .check(classes);

        noClasses().that().resideOutsideOfPackage("..combat.cannon.fire..")
                .should().callConstructor(SanshikiPelletEntity.class,
                        Level.class, LivingEntity.class, float.class, ItemStack.class)
                .because("三式霰弹也必须经由统一发射工厂，避免新增发射者绕过边界")
                .check(classes);
    }
}
