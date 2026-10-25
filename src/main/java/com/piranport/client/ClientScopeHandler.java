package com.piranport.client;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.MuzzlePos;
import com.piranport.combat.BallisticSolver;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 客户端瞄准镜状态管理。
 * 跟踪玩家是否处于瞄准模式、长按 tick 数、当前武器 scopeZoom。
 * 状态存储在静态字段中（与 ClientFireControlData 模式一致）。
 * 同时在客户端运行弹道解算用于性能统计和 HUD 显示。
 */
public final class ClientScopeHandler {

    private static boolean scoping = false;
    private static int holdTicks = 0;
    private static float zoomLevel = 2.0f;
    /** 当前瞄准的目标位置（实体、方块或水面交点） */
    @Nullable
    private static Vec3 aimedPosition = null;
    /** 与目标的水平距离（格） */
    private static double targetDistance = 0;
    /** 与目标的高度差（格） */
    private static double targetVertical = 0;
    /** 当前 tick 射线是否命中有效目标，false 表示射线远端回退。 */
    private static boolean hasValidTarget = false;

    /** 进入瞄准模式之前是否持有火炮 */
    private static boolean heldCannonBeforeScope = false;

    /**
     * 开镜强制第一人称前玩家的视角。
     * null = 本次开镜没有由我们强制切过视角（进入时已在第一人称，或已恢复）。
     */
    @Nullable
    private static CameraType cameraTypeBeforeScope = null;
    /**
     * 开镜期间玩家是否自己按 F5 改过视角。
     * 改过则尊重玩家选择，退出开镜时不再把镜头拽回进入前的状态。
     */
    private static boolean cameraChangedByPlayer = false;

    /** 当前有效交点是否属于实体，用于核对准星实际取点。 */
    private static boolean aimedAtEntity = false;
    /** 当前瞄准点是否为火控预瞄圈的预测落点（HUD 显示「预瞄」）。 */
    private static boolean aimedAtLead = false;

    // ===== 客户端弹道解算相关 =====
    /** 上一次解算的结果（发射仰角，弧度） */
    private static double lastSolvedAngle = 0;
    /**
     * 上一次解算对应的落弹时间（秒）。NaN = 无有效读数（未解算 / 超射程）。
     * 与 {@link #lastSolvedAngle} 同源同周期更新，保证 HUD 的「仰角 / 落弹时间」始终配套。
     */
    private static double lastFlightSeconds = Double.NaN;
    /** 上一次解算是否判定为目标超出射程 */
    private static boolean lastOutOfRange = false;
    /** 是否已完成至少一次解算 */
    private static boolean hasSolved = false;
    /** 解算间隔（ticks）：避免每 tick 都解算，降低性能开销 */
    private static final int SOLVE_INTERVAL = 5;
    /** 射线检测绝对上限（格），防止极端情况（如测试武器）造成性能问题。 */
    private static final double SCOPE_RAYCAST_MAX = 1000.0;
    /** 上一次解算的 tick */
    private static int lastSolveTick = 0;

    // ===== 服务端解算统计（由 S2C 载荷更新） =====
    private static int serverTernaryIters = 0;
    private static int serverNewtonIters = 0;
    private static long serverTotalUs = 0;
    private static double serverVerticalError = 0.0;
    private static double serverHorizontalError = 0.0;
    private static double serverAngleDeg = 0.0;

    private ClientScopeHandler() {}

    /** 进入瞄准模式 */
    public static void enterScope(Player player, ItemStack weapon) {
        if (scoping) return;
        zoomLevel = getZoomFromWeapon(weapon);
        if (zoomLevel <= 0) return;
        // 先切视角再置 scoping：开镜的第一帧起镜头就是第一人称，不会出现
        // 「已开镜但自己的角色模型还挡在准星前」的一帧。zoomLevel 只是数值，与切换先后无关。
        forceFirstPersonForScope();
        scoping = true;
        holdTicks = 0;
        heldCannonBeforeScope = true;
        hasSolved = false;
        lastFlightSeconds = Double.NaN;
        lastOutOfRange = false;
        serverTotalUs = 0;
        serverVerticalError = 0.0;
        serverHorizontalError = 0.0;
        serverAngleDeg = 0.0;
        lastSolveTick = 0;
    }

    /** 退出瞄准模式 */
    public static void exitScope() {
        // 所有正常退出路径（松键、切武器、打开界面、死亡界面）都汇到这里，
        // 在此统一恢复开镜前的视角，避免镜头卡在第一人称。
        restoreCameraAfterScope();
        scoping = false;
        holdTicks = 0;
        aimedPosition = null;
        targetDistance = 0;
        targetVertical = 0;
        hasValidTarget = false;
        heldCannonBeforeScope = false;
        hasSolved = false;
        lastFlightSeconds = Double.NaN;
        lastOutOfRange = false;
        serverTotalUs = 0;
        serverVerticalError = 0.0;
        serverHorizontalError = 0.0;
        serverAngleDeg = 0.0;
    }

    // ===== 开镜视角切换 =====

    /**
     * 开镜强制第一人称。
     * WHY：第三人称下开镜时玩家自己的角色模型正好挡在准星前面，完全没法瞄准，
     * 所以开镜必须切第一人称；同时记录进入前的视角，退出开镜后还原。
     * 本来就在第一人称时不记录，退出时也就不会多做一次无意义的设置。
     */
    private static void forceFirstPersonForScope() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        CameraType current = mc.options.getCameraType();
        if (current == CameraType.FIRST_PERSON) return;
        cameraTypeBeforeScope = current;
        cameraChangedByPlayer = false;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
    }

    /**
     * 退出开镜时恢复进入前的视角。
     * WHY 只在「我们强切过」且「玩家没自己改过」时恢复：玩家开镜期间按 F5
     * 是明确的意图表达，退出开镜时再把视角拽回去会显得像按键失灵。
     */
    private static void restoreCameraAfterScope() {
        CameraType original = cameraTypeBeforeScope;
        boolean changedByPlayer = cameraChangedByPlayer;
        // 先清记录：即使下面的恢复因客户端未就绪而跳过，也不能把过期状态留到下一次开镜。
        cameraTypeBeforeScope = null;
        cameraChangedByPlayer = false;
        if (original == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        CameraType toRestore = cameraTypeToRestore(original, mc.options.getCameraType(), changedByPlayer);
        if (toRestore != null) {
            mc.options.setCameraType(toRestore);
        }
    }

    /**
     * 开镜期间玩家按 F5 改过视角就记下来（离开第一人称即视为改过）。
     * 用「曾经离开第一人称」而不是退出瞬间比较，是为了覆盖玩家连按 F5 又转回
     * 第一人称的情况：那同样是他自己的选择，不应被我们恢复成第三人称。
     */
    private static void detectPlayerCameraChange() {
        if (cameraTypeBeforeScope == null || cameraChangedByPlayer) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            cameraChangedByPlayer = true;
        }
    }

    /**
     * 退出开镜时该恢复的视角；null = 不改视角。抽成纯逻辑便于单测。
     *
     * @param original         开镜前记录的原视角；null 表示本次开镜不是我们强切的
     * @param current          退出瞬间的视角
     * @param changedByPlayer  开镜期间玩家是否自己切过视角
     */
    @Nullable
    static CameraType cameraTypeToRestore(@Nullable CameraType original, CameraType current,
                                          boolean changedByPlayer) {
        if (original == null || changedByPlayer) return null;
        // 仍停在我们切的第一人称 = 玩家全程没动过视角，按原样恢复；
        // 否则说明视角已被玩家接管，以玩家当前选择为准。
        return current == CameraType.FIRST_PERSON ? original : null;
    }

    /** 每客户端 tick 调用，更新长按计数和射线检测 */
    public static void tick(Player player, ItemStack weapon) {
        if (!scoping) return;
        detectPlayerCameraChange();
        holdTicks++;

        // 每 tick 更新射线检测，获取目标位置
        updateAimedPosition(player, weapon);

        // 定期进行客户端弹道解算（用于性能统计）
        if (hasValidTarget && targetDistance > 0
                && (!hasSolved || holdTicks - lastSolveTick >= SOLVE_INTERVAL)) {
            solveBallisticsClient(weapon);
            lastSolveTick = holdTicks;
        }
    }

    /**
     * 非瞄准镜状态下维护准星射线，用于闭镜开火。
     *
     * <p><b>玩法定位</b>：只要手持火炮（无论开镜与否）就每 tick 更新准星射线，
     * 闭镜开火与开镜使用同一套弹道解算落点，差别只在服务端散布 ×1.1。
     * 依据：docs/策划决策/武器/13-火炮闭镜落点算法与散布惩罚.md（方案 B）
     */
    public static void tickQuickAim(Player player, ItemStack weapon) {
        if (scoping) return;
        updateAimedPosition(player, weapon);
    }

    /** 玩家不再持有火炮时清理非瞄准镜快速瞄准状态。 */
    public static void clearQuickAim() {
        if (scoping) return;
        aimedPosition = null;
        targetDistance = 0;
        targetVertical = 0;
        hasValidTarget = false;
    }

    /**
     * 客户端弹道解算：用于性能统计和 HUD 显示
     */
    private static void solveBallisticsClient(ItemStack weapon) {
        if (!(weapon.getItem() instanceof ArtilleryItem ai)) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        ArtilleryCannonData effectiveData = ai.getEffectiveData(level);
        float velocity = effectiveData.initialSpeed();
        float drag = effectiveData.dragCoeff();
        float gravity = effectiveData.gravity();
        double mcGravity = gravity > 0f ? gravity / 196.0 : BallisticSolver.DEFAULT_GRAVITY;

        if (velocity <= 0 || drag < 0) return;

        // 射程截断：目标超出终端「最大射程」时不再对目标解算，改按最大射程点（与发射点同高）
        // 解算，并把读数标记为超程（ScopeHudLayer 据此提示超出射程）。与开火路径截断口径一致，
        // 避免瞄准镜给出一份服务端根本不会采用的目标解。
        double maxRange = com.piranport.config.ModArtilleryConfig.ARTILLERY_MAX_RANGE.get();
        boolean truncated = BallisticSolver.beyondMaxRange(targetDistance, maxRange);
        double solveDist = truncated ? maxRange : targetDistance;
        double solveVert = truncated ? 0.0 : targetVertical;

        // 单炮最大水平射程（策划决策/武器/17）：水平距离超该值时按 maxHorizontalRange 解算、
        // 高度差同比缩放，标记超射程；与服务端 CannonAiming 口径一致，否则 HUD 读数会骗人。
        double cannonCap = effectiveData.maxHorizontalRange();
        if (!truncated && cannonCap > 0 && targetDistance > cannonCap) {
            truncated = true;
            solveDist = cannonCap;
            solveVert = targetVertical * (cannonCap / targetDistance);
        }

        // 运行解算（会自动记录性能统计到 BallisticSolverStats）。
        BallisticSolver.Result result = BallisticSolver.solve(
                velocity, drag, mcGravity, solveDist, solveVert, 0.0,
                BallisticSolver.UNRESTRICTED_MIN_ANGLE,
                Math.toRadians(effectiveData.maxElevation()));
        lastSolvedAngle = result.angle();
        lastOutOfRange = truncated || result.outOfRange();
        // 落弹时间由仰角回代积分得到（与解算器同一物理模型）。
        // 超射程时不给出读数：回退角是最大射程角，弹丸到不了目标水平距离。
        double flightTicks = lastOutOfRange
                ? Double.NaN
                : BallisticSolver.flightTimeTicks(velocity, lastSolvedAngle, drag, mcGravity,
                        solveDist, 0.0);
        lastFlightSeconds = Double.isFinite(flightTicks) ? flightTicks / 20.0 : Double.NaN;
        hasSolved = true;
    }

    /** 从玩家视角发射射线，获取准星指向的命中点坐标 */
    private static void updateAimedPosition(Player player, ItemStack weapon) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getCameraEntity() == null) return;

        Entity camera = mc.getCameraEntity();
        Vec3 eyePos = camera.getEyePosition();
        Vec3 ballisticOrigin = getReferenceMuzzlePosition(player, weapon, mc.level);
        // 手持火炮没有炮塔实体，按策划决策/武器/12 直接使用准星方向。
        Vec3 lookDir = camera.getLookAngle();

        // 根据火炮实际射程动态计算射线检测范围，避免无意义地检测超射程区域
        double range = estimateRaycastRange(player, weapon);
        if (range <= 0) {
            // 无法获取射程信息时回退到绝对上限
            range = SCOPE_RAYCAST_MAX;
        }

        Vec3 end = eyePos.add(lookDir.scale(range));

        // 水面必须作为落点参与检测，否则瞄水面时会取到海床或射线远端。
        ClipContext clipCtx = new ClipContext(eyePos, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, camera);
        BlockHitResult surfaceHit = mc.level.clip(clipCtx);
        AABB searchBox = new AABB(eyePos, end).inflate(2.0);
        List<AABB> entityBounds = mc.level.getEntities(player, searchBox,
                e -> (e instanceof LivingEntity || e instanceof com.piranport.entity.AircraftEntity)
                        && e.isAlive() && !e.isSpectator() && e != player && e != camera)
                .stream().map(Entity::getBoundingBox).toList();
        ScopeTargeting.Target target = ScopeTargeting.pick(eyePos, end, surfaceHit, entityBounds);
        Vec3 hitPos = target.position();
        hasValidTarget = target.valid();
        aimedAtEntity = target.entity();
        Vec3 leadPoint = FireControlVisualRenderer.leadAimPoint();
        aimedAtLead = leadPoint != null;
        if (leadPoint != null) {
            hitPos = leadPoint;
            hasValidTarget = true;
            aimedAtEntity = false;
        }
        if (!hasValidTarget) {
            hasSolved = false;
            lastFlightSeconds = Double.NaN;
            lastOutOfRange = false;
        }
        aimedPosition = hitPos;
        targetDistance = Math.sqrt(
                (hitPos.x - ballisticOrigin.x) * (hitPos.x - ballisticOrigin.x) +
                (hitPos.z - ballisticOrigin.z) * (hitPos.z - ballisticOrigin.z));
        targetVertical = hitPos.y - ballisticOrigin.y;
    }

    /**
     * 估算当前火炮的有效射线检测范围。
     * 返回实际射程与绝对上限的最小值，避免无意义地检测超射程区域。
     */
    private static double estimateRaycastRange(Player player, ItemStack weapon) {
        if (!(weapon.getItem() instanceof ArtilleryItem ai)) return -1.0;
        ArtilleryCannonData data = ai.getEffectiveData(player.level());
        if (data.initialSpeed() <= 0) return -1.0;
        // 使用服务端物理参数计算理论最大射程
        double mcGravity = data.gravity() > 0f ? data.gravity() / 196.0 : BallisticSolver.DEFAULT_GRAVITY;
        double maxRange = BallisticSolver.calculateMaxHorizontalRange(
                data.initialSpeed(), data.dragCoeff(), mcGravity);
        // 策划决策/武器/17：配置的单炮最大水平射程可能高于物理射程，
        // 射线至少要探到该配置距离，否则超距瞄准点采不到、封顶不会触发。
        if (data.maxHorizontalRange() > 0) {
            maxRange = Math.max(maxRange, data.maxHorizontalRange());
        }
        // 加 10% 余量用于命中点判定
        return Math.min(maxRange * 1.1, SCOPE_RAYCAST_MAX);
    }

    private static Vec3 getReferenceMuzzlePosition(Player player, ItemStack weapon, Level level) {
        Vec3 eyePos = player.getEyePosition();
        if (!(weapon.getItem() instanceof ArtilleryItem ai)) return eyePos;

        ArtilleryCannonData effectiveData = ai.getEffectiveData(level);
        List<MuzzlePos> muzzles = effectiveData.muzzles();
        MuzzlePos muzzle = muzzles.isEmpty() ? new MuzzlePos(0, 0, 1.5) : muzzles.get((muzzles.size() - 1) / 2);
        return eyePos.add(rotateMuzzleByPlayerView(player, muzzle));
    }

    private static Vec3 rotateMuzzleByPlayerView(Player player, MuzzlePos muzzle) {
        double yawRad = Math.toRadians(player.getYRot());
        double pitchRad = Math.toRadians(player.getXRot());

        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        double x = muzzle.x() * cosYaw - muzzle.z() * sinYaw;
        double y = muzzle.y() * cosPitch + muzzle.z() * sinPitch;
        double z = muzzle.x() * sinYaw + muzzle.z() * cosYaw * cosPitch;

        return new Vec3(x, y, z);
    }

    // ===== Getters =====

    public static boolean isScoping() { return scoping; }

    /**
     * 是否已长按达到瞄准阈值（完全进入瞄准模式）。
     * 阈值默认 1 tick，即按下右键当 tick 就返回 true，开镜无延迟。
     */
    public static boolean isFullyScoped() {
        return scoping && holdTicks >= getScopeActivationTicks();
    }

    /** 是否为快速点击（短按未达到瞄准阈值）。阈值默认 1 时不成立。 */
    public static boolean isQuickRelease() {
        return scoping && holdTicks < getScopeActivationTicks();
    }

    public static int getHoldTicks() { return holdTicks; }

    public static int getScopeThreshold() { return getScopeActivationTicks(); }

    public static float getZoomLevel() { return zoomLevel; }

    @Nullable
    public static Vec3 getAimedPosition() { return aimedPosition; }

    public static double getTargetDistance() { return targetDistance; }

    public static double getTargetVertical() { return targetVertical; }

    /** 当前 tick 的射线是否命中有效目标（实体、方块或水面） */
    public static boolean hasValidTarget() { return hasValidTarget; }

    public static String getTargetKindTranslationKey() {
        return !hasValidTarget ? "hud.piranport.scope.target.miss"
                : aimedAtLead ? "hud.piranport.scope.target.lead"
                : aimedAtEntity ? "hud.piranport.scope.target.entity" : "hud.piranport.scope.target.surface";
    }

    /** 上一次客户端解算的发射仰角（弧度） */
    public static double getLastSolvedAngle() { return lastSolvedAngle; }

    /**
     * 上一次客户端解算对应的落弹时间（秒）。
     * 返回 NaN 表示当前没有有效读数（未解算 / 超射程），调用方需自行判断。
     */
    public static double getLastFlightSeconds() { return lastFlightSeconds; }

    /** 上一次解算是否判定为目标超出射程 */
    public static boolean isLastOutOfRange() { return lastOutOfRange; }

    /** 是否已完成至少一次客户端解算 */
    public static boolean hasSolved() { return hasSolved; }

    // ===== 服务端解算统计 =====
    public static void setServerSolverStats(int ternaryIters, int newtonIters) {
        serverTernaryIters = ternaryIters;
        serverNewtonIters = newtonIters;
    }

    public static void setServerSolverStats(int ternaryIters, int newtonIters, long totalUs,
                                            double verticalError, double horizontalError, double angleDeg) {
        serverTernaryIters = ternaryIters;
        serverNewtonIters = newtonIters;
        serverTotalUs = totalUs;
        serverVerticalError = verticalError;
        serverHorizontalError = horizontalError;
        serverAngleDeg = angleDeg;
    }

    public static int getServerTernaryIters() { return serverTernaryIters; }
    public static int getServerNewtonIters() { return serverNewtonIters; }
    public static long getServerTotalUs() { return serverTotalUs; }
    public static double getServerVerticalError() { return serverVerticalError; }
    public static double getServerHorizontalError() { return serverHorizontalError; }
    public static double getServerAngleDeg() { return serverAngleDeg; }

    /** 从武器 ItemStack 读取 scopeZoom */
    private static float getZoomFromWeapon(ItemStack weapon) {
        if (weapon.getItem() instanceof ArtilleryItem ai) {
            Level level = Minecraft.getInstance().level;
            return level != null ? ai.getEffectiveData(level).scopeZoom() : ai.getData().scopeZoom();
        }
        return 2.0f;
    }

    private static int getScopeActivationTicks() {
        // 开镜无延迟：右键按下当 tick holdTicks 即为 1，立刻满足阈值。
        // 早期该阈值是可配置项（activation_ticks，默认 5），现已移除，长按手感不再可调。
        return 1;
    }

    /** 判断玩家是否手持火炮 */
    public static boolean isHoldingCannon(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof ArtilleryItem;
    }

    /** 断开连接、换维度、死亡重生等场景清理状态 */
    public static void clear() {
        // 这些场景不走 exitScope，但同样可能卡在开镜强切的第一人称里，必须一并恢复。
        restoreCameraAfterScope();
        scoping = false;
        holdTicks = 0;
        zoomLevel = 2.0f;
        aimedPosition = null;
        targetDistance = 0;
        targetVertical = 0;
        hasValidTarget = false;
        heldCannonBeforeScope = false;
        hasSolved = false;
        lastFlightSeconds = Double.NaN;
        lastOutOfRange = false;
    }
}
