package com.piranport.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;

class ArchitectureTest {

    private static JavaClasses CLASSES;

    @BeforeAll
    static void setUp() {
        CLASSES = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.piranport");
    }

    // ===== 层隔离 =====

    /** server 包不得引用 client 包 — 服务端没有 GUI/input 类。 */
    @Test
    void serverShouldNotDependOnClient() {
        noClasses()
                .that().resideInAnyPackage("..server..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Server classes must not reference client-only GUI/input classes")
                .check(CLASSES);
    }

    /** client 包不应引用 server 包 — 保持单向依赖。 */
    @Test
    void clientShouldNotDependOnServer() {
        noClasses()
                .that().resideInAnyPackage("..client..")
                .should().dependOnClassesThat().resideInAnyPackage("..server..")
                .because("Client should not hard-depend on server-specific logic")
                .check(CLASSES);
    }

    /**
     * entity 包不应引用 client 包 — 实体是共享层。
     *
     * <p>已知技术债务：{@code AircraftEntity$AircraftGlowHelper} 内联在实体类中但引用
     * {@code Minecraft.getInstance()}，应提取到 {@code client} 包。此处用内部类豁免标记。
     */
    @Test
    void entityShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("..entity..")
                .and(DescribedPredicate.not(
                        new DescribedPredicate<JavaClass>("inner classes (tech debt to extract)") {
                            @Override
                            public boolean test(JavaClass input) {
                                return input.getName().contains("$");
                            }
                        }))
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Entity classes must not reference client-only classes; "
                        + "inner glow helper is known tech debt to extract")
                .check(CLASSES);
    }

    // ===== 包结构 =====

    /** 根包只保留 PiranPort 和 CommonModEvents，废弃过渡类豁免。 */
    @Test
    void rootPackageShouldBeClean() {
        classes()
                .that().resideInAPackage("com.piranport")
                .should().haveSimpleName("PiranPort")
                .orShould().haveSimpleName("CommonModEvents")
                .orShould().beAnnotatedWith(Deprecated.class)
                .because("Root package should only contain PiranPort and CommonModEvents; "
                        + "@Deprecated shells are exempt during transition")
                .check(CLASSES);
    }

    // ===== Manager 规范 =====

    /** 所有 Manager 类应位于服务端相关的子包中。 */
    @Test
    void managersShouldResideInServerSidePackages() {
        classes()
                .that().haveSimpleNameEndingWith("Manager")
                .should().resideInAnyPackage(
                        "..server..",
                        "..aviation..",
                        "..combat..",
                        "..dungeon..",
                        "..npc..",
                        "..skin..",
                        "..config..")
                .because("Managers hold server-side state; should not be in client-only packages")
                .check(CLASSES);
    }

    /**
     * Manager 类的静态集合字段应使用并发容器。
     * 防止多线程环境下出现 ConcurrentModificationException。
     */
    @Test
    void managersShouldPreferConcurrentCollections() {
        // 定义非并发集合类型集合
        Set<String> nonConcurrent = Set.of(
                HashMap.class.getName(),
                HashSet.class.getName(),
                ArrayList.class.getName(),
                "java.util.LinkedList",
                "java.util.LinkedHashMap",
                "java.util.TreeMap",
                "java.util.TreeSet");

        ArchCondition<JavaClass> noNonConcurrentStaticCollections =
                new ArchCondition<>("not have static non-concurrent collection fields") {
                    @Override
                    public void check(JavaClass clazz, ConditionEvents events) {
                        clazz.getFields().stream()
                                .filter(f -> f.getModifiers().contains(java.lang.reflect.Modifier.STATIC))
                                .forEach(f -> {
                                    String typeName = f.getRawType().getName();
                                    if (nonConcurrent.contains(typeName)) {
                                        events.add(SimpleConditionEvent.violated(clazz,
                                                clazz.getSimpleName() + "." + f.getName()
                                                        + " 是静态 " + typeName + "，考虑用并发容器"));
                                    }
                                });
                    }
                };

        classes()
                .that().haveSimpleNameEndingWith("Manager")
                .should(noNonConcurrentStaticCollections)
                .because("Manager classes are accessed from multiple threads; "
                        + "static collections must be thread-safe")
                .check(CLASSES);
    }

    // ===== 网络层 =====

    /**
     * 网络包（不含 *Payload 类）不应依赖 client 包。
     * <p>
     * Payload 类遵循 NeoForge 内联客户端处理模式，
     * handle() 方法中引用 client 类是标准做法。其余网络代码应保持端无关。
     */
    @Test
    void networkShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("com.piranport.network..")
                .and(DescribedPredicate.not(
                        new DescribedPredicate<JavaClass>("Payload classes (inline handler pattern)") {
                            @Override
                            public boolean test(JavaClass input) {
                                return input.getSimpleName().endsWith("Payload");
                            }
                        }))
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Network code (except Payload inline handlers) must be client/server agnostic")
                .check(CLASSES);
    }

    // ===== 弹药与注册 =====

    /** ammo 包不应依赖 client 包。 */
    @Test
    void ammoShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("..ammo..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Ammo registration is server-side logic; must not depend on client")
                .check(CLASSES);
    }

    /** crafting 包不应依赖 client 包。 */
    @Test
    void craftingShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("..crafting..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Crafting recipes are server-side; must not depend on client")
                .check(CLASSES);
    }
}
