package com.piranport.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** entity 包不应引用 client 包 — 实体是共享层。 */
    @Test
    void entityShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("..entity..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Entity classes must not reference client-only classes")
                .check(CLASSES);
    }

    /**
     * 高频共享包不得直接依赖 Minecraft 客户端类。
     *
     * <p>若共享物品/实体需要读取本地客户端状态，应通过 {@code platform.ClientHooks}
     * 反射桥接到 client 包，避免 dedicated server 类加载崩溃。
     */
    @Test
    void sharedGameplayPackagesShouldNotDependOnMinecraftClient() {
        noClasses()
                .that().resideInAnyPackage(
                        "..artillery..",
                        "..combat..",
                        "..entity..",
                        "..item..",
                        "..network..",
                        "..registry..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "net.minecraft.client..",
                        "net.neoforged.neoforge.client..")
                .because("Shared gameplay classes must not load Minecraft client-only classes")
                .check(CLASSES);
    }

    /** 高频共享包不得直接依赖本项目 client 包，应通过 platform.ClientHooks 桥接。 */
    @Test
    void sharedGameplayPackagesShouldNotDependOnOwnClientPackage() {
        noClasses()
                .that().resideInAnyPackage(
                        "..artillery..",
                        "..combat..",
                        "..entity..",
                        "..item..",
                        "..network..",
                        "..registry..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Shared gameplay classes must use platform.ClientHooks for client-only helpers")
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

    /** 网络包不应依赖 client 包。客户端效果应通过 platform.ClientHooks 桥接。 */
    @Test
    void networkShouldNotDependOnClient() {
        noClasses()
                .that().resideInAPackage("com.piranport.network..")
                .should().dependOnClassesThat().resideInAnyPackage("..client..")
                .because("Network code must be client/server agnostic")
                .check(CLASSES);
    }

    /** 配置覆盖入口必须走统一管理员权限，不得用创造模式替代管理权限。 */
    @Test
    void configMutationPayloadsShouldUseAdminPermissionGate() throws IOException {
        String permissionSource = Files.readString(
                Path.of("src/main/java/com/piranport/config/ConfigToolPermissions.java"));
        assertTrue(permissionSource.contains("CONFIG_ADMIN_PERMISSION_LEVEL = 2"),
                "ConfigToolPermissions must keep config-admin access at permission level 2");
        assertTrue(permissionSource.contains("hasPermissions(CONFIG_ADMIN_PERMISSION_LEVEL)"),
                "ConfigToolPermissions must accept OP-level players");
        assertTrue(permissionSource.contains("isSingleplayerOwner"),
                "ConfigToolPermissions must keep no-cheats singleplayer owner testing available");

        String[] files = {
                "src/main/java/com/piranport/network/UpdateConfigOverridePayload.java",
                "src/main/java/com/piranport/network/ImportConfigPayload.java",
                "src/main/java/com/piranport/network/ExportConfigPayload.java",
                "src/main/java/com/piranport/network/ResetConfigPayload.java",
                "src/main/java/com/piranport/item/ArtilleryConfigToolItem.java",
                "src/main/java/com/piranport/menu/ArtilleryConfigToolMenu.java"
        };

        for (String file : files) {
            String source = Files.readString(Path.of(file));
            assertTrue(source.contains("ConfigToolPermissions.canUse"),
                    file + " must gate config mutation through ConfigToolPermissions");
            assertFalse(source.contains("isCreative()"),
                    file + " must not treat creative mode as config-admin permission");
        }
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
