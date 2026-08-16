package com.piranport.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * 包结构架构测试 — 验证 Phase 2 重组后的包结构规范。
 */
class PackageStructureTest {

    private static JavaClasses CLASSES;

    @BeforeAll
    static void setUp() {
        CLASSES = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.piranport");
    }

    /** 根包只保留 PiranPort 主类,其他类应迁移到子包 */
    @Test
    void rootPackageShouldOnlyContainMainClass() {
        classes()
                .that().resideInAPackage("com.piranport")
                .should().haveSimpleName("PiranPort")
                .because("Root package should only contain the main mod class")
                .check(CLASSES);
    }

    /** event 包应只包含事件类和事件监听器 */
    @Test
    void eventPackageShouldOnlyContainEventsAndListeners() {
        classes()
                .that().resideInAPackage("com.piranport.event..")
                .should().haveSimpleNameEndingWith("Event")
                .orShould().haveSimpleNameEndingWith("Events")
                .orShould().haveSimpleNameEndingWith("EventBus")
                .orShould().haveSimpleNameEndingWith("Listener")
                .orShould().haveSimpleNameEndingWith("Handler")
                .because("Event package should only contain events and listeners")
                .check(CLASSES);
    }

    /** service 包应只包含 Service 接口和实现类 */
    @Test
    void servicePackageShouldOnlyContainServices() {
        classes()
                .that().resideInAPackage("com.piranport.service..")
                .and().doNotHaveSimpleName("package-info")
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl")
                .because("Service package should only contain service interfaces and implementations")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    /** util 包应只包含 Utils 工具类 */
    @Test
    void utilPackageShouldOnlyContainUtils() {
        classes()
                .that().resideInAPackage("com.piranport.util..")
                .and().doNotHaveSimpleName("package-info")
                .should().haveSimpleNameEndingWith("Utils")
                .orShould().haveSimpleNameEndingWith("Helper")
                .because("Util package should only contain utility classes")
                .allowEmptyShould(true)
                .check(CLASSES);
    }
}
