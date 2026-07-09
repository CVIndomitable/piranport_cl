#!/bin/bash
# Phase 2: Handler/Helper 批量重组脚本

echo "=== Phase 2: Handler/Helper 重组开始 ==="

# 1. 迁移事件监听器到 event/ 包
echo "步骤 1: 迁移事件监听器..."

# ServerGameEvents 已在正确位置,只需更新包引用
if [ -f "src/main/java/com/piranport/server/ServerGameEvents.java" ]; then
    echo "  - ServerGameEvents 已在 server/ 包中,保持不变"
fi

# 2. 创建工具类包结构
echo "步骤 2: 创建工具类包结构..."
mkdir -p src/main/java/com/piranport/util

# 3. 创建 package-info.java 文档
echo "步骤 3: 为关键包添加 package-info.java..."

# combat 包文档
cat > src/main/java/com/piranport/combat/package-info.java << 'EOF'
/**
 * 战斗系统 — 玩家变身、伤害计算、武器系统等核心战斗逻辑。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.combat.TransformationManager} — 变身状态管理</li>
 *   <li>{@link com.piranport.combat.BallisticSolver} — 弹道求解器</li>
 *   <li>{@link com.piranport.combat.SalvoManager} — 齐射调度器</li>
 *   <li>{@link com.piranport.combat.TorpedoGuidanceManager} — 鱼雷制导</li>
 * </ul>
 *
 * <h2>职责范围</h2>
 * <ul>
 *   <li>变身系统: 玩家从人类形态到舰娘形态的切换</li>
 *   <li>负重系统: 武器/装甲的重量计算与超重惩罚</li>
 *   <li>伤害计算: 护甲减免/穿透/友军伤害判定</li>
 *   <li>弹道系统: 火炮弹道求解/鱼雷制导</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.piranport.combat;
EOF

# aviation 包文档
cat > src/main/java/com/piranport/aviation/package-info.java << 'EOF'
/**
 * 航空系统 — 飞机召唤、火控锁定、侦察模式等航空作战逻辑。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.aviation.FireControlManager} — 火控锁定管理</li>
 *   <li>{@link com.piranport.aviation.ReconManager} — 侦察模式管理</li>
 *   <li>{@link com.piranport.aviation.AircraftIndex} — 飞机实体索引</li>
 * </ul>
 *
 * <h2>职责范围</h2>
 * <ul>
 *   <li>火控系统: 多目标锁定/自动瞄准</li>
 *   <li>侦察系统: 远程视野/身体锁定</li>
 *   <li>飞机管理: 召回/燃料/索引维护</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.piranport.aviation;
EOF

# handler 包文档
cat > src/main/java/com/piranport/handler/package-info.java << 'EOF'
/**
 * 事件处理器 — 玩家 Tick/连接/数据等全局事件的监听器。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.handler.PlayerTickHandler} — 玩家 Tick 驱动(变身/燃料/声呐/自动战斗)</li>
 *   <li>{@link com.piranport.handler.PlayerConnectionHandler} — 玩家登录/登出/死亡/维度切换</li>
 *   <li>{@link com.piranport.handler.PlayerAircraftHelper} — 飞机召回工具方法</li>
 * </ul>
 *
 * <h2>重构计划</h2>
 * <p>本包将逐步拆分为:
 * <ul>
 *   <li>{@code event/} 包 — 事件监听器(NeoForge EventBus)</li>
 *   <li>{@code service/} 包 — 业务服务接口</li>
 *   <li>{@code util/} 包 — 无状态工具类</li>
 * </ul>
 *
 * @see com.piranport.event
 * @since 1.0.0
 */
package com.piranport.handler;
EOF

# util 包文档
cat > src/main/java/com/piranport/util/package-info.java << 'EOF'
/**
 * 工具类集合 — 无状态的数学/碰撞/序列化等辅助工具。
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>所有工具类都是无状态的(只包含静态方法)</li>
 *   <li>私有构造函数禁止实例化</li>
 *   <li>方法参数和返回值都是不可变对象或基本类型</li>
 * </ul>
 *
 * <h2>规划中的工具类</h2>
 * <ul>
 *   <li>{@code MathUtils} — 数学计算(三角/插值/随机)</li>
 *   <li>{@code CollisionUtils} — 碰撞检测/射线追踪</li>
 *   <li>{@code NBTUtils} — NBT 序列化工具</li>
 *   <li>{@code NetworkUtils} — 网络包工具方法</li>
 * </ul>
 *
 * @since 1.1.0
 */
package com.piranport.util;
EOF

# service 包文档
cat > src/main/java/com/piranport/service/package-info.java << 'EOF'
/**
 * 业务服务层 — 有状态的业务逻辑服务接口。
 *
 * <h2>设计模式</h2>
 * <p>采用服务接口 + 单例实现模式:
 * <pre>{@code
 * public interface TransformationService {
 *     boolean isTransformed(ItemStack stack);
 * }
 *
 * public final class TransformationServiceImpl implements TransformationService {
 *     private static final TransformationService INSTANCE = new TransformationServiceImpl();
 *     public static TransformationService getInstance() { return INSTANCE; }
 *
 *     @Override
 *     public boolean isTransformed(ItemStack stack) { ... }
 * }
 * }</pre>
 *
 * <h2>vs Manager 类</h2>
 * <table>
 *   <tr><th>维度</th><th>Service</th><th>Manager</th></tr>
 *   <tr><td>接口</td><td>接口 + 实现分离</td><td>静态方法类</td></tr>
 *   <tr><td>可测试性</td><td>易 mock</td><td>难 mock</td></tr>
 *   <tr><td>扩展性</td><td>可替换实现</td><td>硬编码</td></tr>
 * </table>
 *
 * <h2>迁移计划</h2>
 * <p>现有 Manager 类将逐步迁移为 Service 接口:
 * <ul>
 *   <li>{@code TransformationManager} → {@code TransformationService}</li>
 *   <li>{@code FireControlManager} → {@code FireControlService}</li>
 *   <li>{@code ReconManager} → {@code ReconService}</li>
 * </ul>
 *
 * @since 1.1.0
 */
package com.piranport.service;
EOF

echo "步骤 3 完成: 已创建 5 个 package-info.java"

# 4. 更新架构测试规则
echo "步骤 4: 更新架构测试规则..."

cat > src/test/java/com/piranport/architecture/PackageStructureTest.java << 'EOF'
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
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("ServiceImpl")
                .because("Service package should only contain service interfaces and implementations")
                .check(CLASSES);
    }

    /** util 包应只包含 Utils 工具类 */
    @Test
    void utilPackageShouldOnlyContainUtils() {
        classes()
                .that().resideInAPackage("com.piranport.util..")
                .should().haveSimpleNameEndingWith("Utils")
                .orShould().haveSimpleNameEndingWith("Helper")
                .because("Util package should only contain utility classes")
                .check(CLASSES);
    }
}
EOF

echo "步骤 4 完成: 已创建 PackageStructureTest"

echo ""
echo "=== Phase 2 重组脚本执行完成 ==="
echo "已完成:"
echo "  - ✅ 创建 5 个 package-info.java 文档"
echo "  - ✅ 创建 PackageStructureTest 架构测试"
echo ""
echo "后续手动任务:"
echo "  - 逐步迁移 Manager → Service (保持兼容性)"
echo "  - 抽取无状态方法到 util/ 包"
echo "  - 运行测试验证: ./gradlew test"
EOF
chmod +x refactor_phase2.sh