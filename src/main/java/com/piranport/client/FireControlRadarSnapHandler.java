package com.piranport.client;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.CombatTargeting;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 火控雷达准星吸附 —— 火炮瞄准辅助。
 *
 * <p><b>与中键火控锁定无关</b>。中键维护的是「火控锁定列表」（{@code FireControlManager} /
 * {@code ClientFireControlData}，供航空投放与导弹使用）；本处理器做的是「把准星往敌对目标
 * 上吸」，纯瞄准手感，不产生任何锁定状态、不发任何包。两套系统互不引用。
 *
 * <h2>玩法口径（2026-09-23 定稿）</h2>
 * <ol>
 *   <li><b>仅开镜状态生效</b>。闭镜时准星是「随手一甩」的自由瞄准，任何自动吸附都会让玩家
 *       觉得鼠标被抢；且开镜才有倍率去分辨"靠近哪个目标"。所以 {@code !ClientScopeHandler.isScoping()}
 *       直接返回。</li>
 *   <li><b>进入吸附有阈值，且带回滞</b>：视线与目标的夹角小于
 *       {@link #ENTER_RADIUS_DEGREES} 才吸上去；吸上后放宽到
 *       {@link #EXIT_RADIUS_DEGREES} 才松手。两个阈值不相等是刻意的 —— 相等会让准星在
 *       阈值边缘反复吸上/松开，看起来像抖动。</li>
 *   <li><b>吸上就锁死</b>：锁定期间即使别的目标夹角更小也不换目标（{@link SnapDecision} 的
 *       第一优先级分支）。要换目标必须先挣脱退出阈值。</li>
 * </ol>
 *
 * <h2>修正的历史 bug（首版实现）</h2>
 * <ul>
 *   <li><b>不管遮挡</b>：首版用 {@code ProjectileUtil.getEntityHitResult} 做射线求交，
 *       它只判断「包围盒被射线穿过」，完全看不见方块 —— 于是射线擦过墙、山、船体后仍会
 *       把墙后的怪选成吸附目标，表现为「按 0 过一会儿吸到奇怪的地方」。现在选新目标前
 *       用 {@link ClipContext} 做一次真正的方块遮挡检测。</li>
 *   <li><b>瞄准点与渲染位置不一致</b>：首版沿用射线求交返回的<b>根部</b>命中坐标，与屏幕上
 *       看到的实体位置对不上，目标一动就明显错开。修的过程中还踩了第二个坑：
 *       {@code getPosition(1.0f)} 插值的是 {@code xo→getX()}，而 {@link AircraftEntity}
 *       的网络落点存在自己的 {@code clientLerpX/Y/Z}（见其 {@code lerpTo} 注释，
 *       官方就是为防「xo==x 导致镜头抖动」才拆的），于是对飞机读出来永远是滞后一拍的整点位置。
 *       现在统一取 {@code position()} —— 与渲染同源，不带任何自家插值假设。</li>
 *   <li><b>忽略俯仰翻转</b>：{@link Entity#turn} 内部对 xRot 做了 ±90° 钳制，
 *       脸朝下的目标多转 0.1° 就会从 -90 翻到 +90；而 {@code xRotO} 那个方向<b>不</b>钳制，
 *       两个值分道扬镳后渲染插值会甩出一个巨大的假旋转 —— 这就是「吸到奇奇怪怪的地方」的
 *       另一半原因。现在解算出的角度先钳制再喂给 turn，使两者永远一致。</li>
 *   <li><b>不校验目标是否真的存在</b>：首版只靠服务端下发的模拟距离上限当「存在性证明」，
 *       但那个上限是服务端按<b>玩家</b>的模拟距离算的，与客户端<b>区块</b>加载进度无关。
 *       方块与实体是两条独立同步流，实体包可以比区块先到，于是能吸到一个站在未建区块上、
 *       坐标却没坐实的实体 —— 玩家看到的就是准星指向一片空白。现在按
 *       {@code level.isLoaded(blockPos)} 逐个校验（见 {@link #collectCandidates}）。</li>
 * </ol>
 *
 * <h2>转向写法（关键，别乱改）</h2>
 * <ul>
 *   <li>必须走 {@link Entity#turn(double, double)}，不能直接 {@code setYRot}。
 *       已逐行核对 1.21.1 反编译源码（{@code Entity#turn}）：它除了 {@code setXRot/setYRot}
 *       还会同步推进 {@code xRotO/yRotO}，并在末尾调用 {@code vehicle.onPassengerTurned(this)}。
 *       最后那句才是不能用 {@code setYRot} 顶替的硬理由：骑乘时载具朝向靠它跟视角走，
 *       手写 set 会让载具和镜头分家。
 *       （至于 {@code xRotO}：{@code Entity#baseTick} 开头本来就会把它对齐到当前值，
 *       所以直接 set 并不会「上一帧角度滞后」—— 这里选 {@code turn} 图的是它
 *       让 O 与当前值同进同出、旋转无插值瞬切，以及上面那句载具传播。）</li>
 *   <li>{@code turn} 的第一个参数是<b>偏航</b>原始量、第二个是<b>俯仰</b>原始量，
 *       且内部固定乘 0.15 后才变成角度。所以「想转 N 度」的原始量是 {@code N / 0.15}。
 *       <br>已核对反编译源码：{@code Entity#turn(double p1, double p2)} 里
 *       {@code f = p2*0.15 → setXRot}（俯仰）、{@code f1 = p1*0.15 → setYRot}（偏航）；
 *       {@code MouseHandler#turnPlayer} 也写作 {@code player.turn(鼠标X增量, 鼠标Y增量)} ——
 *       鼠标左右移动改的是 yaw。传反不是「转得别扭」而是<b>横竖各转错一根轴</b>，
 *       准星会横着甩、竖直乱跳，正是本类要修的那个症状。</li>
 *   <li>除数<b>只有</b> 0.15，不能额外乘 {@code effSens³}。原版那个系数是乘在
 *       <b>鼠标像素增量</b>上的分子（{@code MouseHandler#turnPlayer}: {@code d3 = effSens³}，
 *       开镜用 d3、非开镜再 ×8），不是 turn 的内部分母。把它放分母会让「每 tick 实转」
 *       变成 {@code stepDeg / effSens³}：灵敏度 0.5（默认）时 10° 限速放大成 80°，
 *       灵敏度调到 0 直接饱和到 ±90° —— 限速在整个滑块范围内失效。本类要的是
 *       「每 tick 转多少度」与玩家灵敏度<b>无关</b>，所以除 0.15 就够。</li>
 * </ul>
 *
 * <p><b>线程模型</b>：客户端渲染线程（{@code ClientTickEvent}），单线程无并发。
 */
public final class FireControlRadarSnapHandler {

    private FireControlRadarSnapHandler() {}

    /** 每 tick 的最大转向角（度）。取值目标：约 0.15 秒内完成一次 90° 转向，贴脸跟枪不丢、远距离不眩晕。 */
    private static final float MAX_TURN_DEGREES_PER_TICK = 10.0f;

    /** 角度误差小于该值就不再转，避免在目标正中心来回过冲抖动。 */
    private static final float TURN_EPSILON_DEGREES = 0.35f;

    /** 进入吸附的夹角阈值（度）。准星与目标偏差小于它才吸上去。 */
    private static final double ENTER_RADIUS_DEGREES = 4.0;

    /**
     * 脱离吸附的夹角阈值（度）。必须显著大于 {@link #ENTER_RADIUS_DEGREES}，
     * 否则目标在阈值边缘移动时会「吸上 → 松开 → 再吸上」，表现为画面抖。
     */
    private static final double EXIT_RADIUS_DEGREES = 8.0;

    /**
     * 候选搜索半径（格）。
     *
     * <p>这是「找候选实体」的粗筛半径，不是吸附决策半径（决策靠角度阈值）。取 128 格：
     * 足够覆盖开镜观察范围，又不至于每 tick 遍历整个模拟距离内的全部实体。
     * 运行时还会被服务端下发的模拟距离上限二次钳制，见 {@link #effectiveRange}。
     */
    private static final double CANDIDATE_SEARCH_RADIUS = 128.0;

    /** 瞄准点取实体身高多少比例处。瞄胸口/舰桥比瞄脚底更符合火炮瞄准直觉。 */
    private static final double AIM_HEIGHT_FRACTION = 0.4;

    /**
     * 清空吸附状态机。断开连接时由 {@code ClientInputCoordinator#resetClientState} 调用。
     */
    public static void reset() {
        SnapDecision.reset();
    }

    /**
     * 每客户端 tick 调用一次。未装备/未开启/未开镜/未变身时什么也不做。
     */
    public static void tick(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        // 「仅开镜状态」：闭镜时的准星是自由瞄准，任何自动吸附都会让玩家觉得鼠标被抢。
        if (!ClientScopeHandler.isScoping()) return;

        // 侦察/鱼雷制导等模式把摄像机挂在别的实体上（见 ReconInputHandler 的 setCameraEntity）。
        // 此时 isScoping() 可能仍为残留的 true，而准星瞄的是侦察机自身的机头方向 ——
        // 若还去转 mc.player 的视角，玩家会看到侦察机在乱转。
        if (mc.getCameraEntity() != player) return;

        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        if (coreStack.isEmpty()) return;

        if (!Boolean.TRUE.equals(coreStack.get(ModDataComponents.SHIP_FC_RADAR_ON.get()))) return;

        if (!TransformationManager.hasFireControlRadarEquipped(player, coreStack)) return;

        // 火炮才谈得上「瞄准辅助」：手持非火炮时吸附没有落点意义，且会跟其他交互抢镜头。
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ArtilleryItem)) return;

        List<SnapDecision.Candidate> candidates = collectCandidates(mc, player);
        SnapDecision.Candidate chosen = SnapDecision.decide(
                candidates, ENTER_RADIUS_DEGREES, EXIT_RADIUS_DEGREES,
                SnapDecision.isSnapping() ? SnapDecision.lockedTarget().id() : -1);

        if (chosen == null) return;

        aimAt(mc, player, chosen);
    }

    /**
     * 收集本 tick 的可吸附候选。
     *
     * <p>候选 = 「准星附近、敌对、且与视线之间有明确夹角」的实体，其中夹角超过退出阈值的
     * 提前剪掉。遮挡（{@code hasLineOfSight}）在这里就解算好，交给 {@link SnapDecision}
     * 做决策，避免决策函数依赖 Minecraft 运行时（那样就没法写单测）。
     */
    private static List<SnapDecision.Candidate> collectCandidates(Minecraft mc, Player player) {

        List<SnapDecision.Candidate> out = new ArrayList<>();

        double range = Math.min(CANDIDATE_SEARCH_RADIUS, serverSimulationLimitBlocks);
        if (range <= 1.0) return out;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();

        // 以眼睛为中心的正方体粗筛，再逐个做角度/遮挡判定。
        // 不沿视线延伸成盒子：吸附要能在「目标略微离开准星」时仍被抓住，
        // 沿视线延伸的盒子在贴脸时会漏掉大半个视锥。
        AABB searchBox = new AABB(eyePos, eyePos).inflate(range);

        for (Entity e : mc.level.getEntities(player, searchBox, candidate -> isSnapCandidate(player, candidate))) {
            // 服务端只把「已加载区块」里的实体同步下来，但方块/实体是两条独立的同步流：
            // 实体包可能比区块先到，此时实体已经在 level 里、坐标也已写入，它所在的区块却还没建出来。
            // 这种实体挡不住（hasLineOfSight 的 clip 会被 ChunkSource 自动降级成空区块检索，
            // 返回 MISS，于是判成「有视线」），炮弹也用不上（服务端按方块算命中）。
            // 位置未坐实的实体一旦被吸上，就表现为「吸到一片没有地形的位置」——
            // 必须按「它脚下有没有已加载的区块」把它们挡在候选之外。
            if (!mc.level.isLoaded(e.blockPosition())) continue;

            // 用「当前落点」而不是 getPosition(1.0f)：后者插值的是 xo→getX()，
            // 而 AircraftEntity 把网络包的落点存进自己的 clientLerpX/Y/Z，在 tick() 里平滑推进，
            // 官方实现在 lerpTo 里正是为了「别让 xo==x 导致镜头抖动」才这么拆的
            // （见 AircraftEntity#lerpTo 注释）。于是 getPosition(1.0f) 对飞机永远是
            // 上一个 tick 的整点位置，读出来的是滞后一拍的旧坐标。
            // 吸附的瞄准点必须跟渲染看得见的位置一致，所以这里取 position()。
            Vec3 aimPoint = e.position().add(0.0, e.getBbHeight() * AIM_HEIGHT_FRACTION, 0.0);
            Vec3 delta = aimPoint.subtract(eyePos);
            double distance = delta.length();
            if (distance < 1.0e-3) continue;

            Vec3 direction = delta.scale(1.0 / distance);
            double cos = direction.dot(lookDir);
            double angleDeg = Math.toDegrees(Math.acos(Mth.clamp(cos, -1.0, 1.0)));

            // 粗筛：夹角超过退出阈值（阈值里较大的那个）的实体本 tick 不可能入选 ——
            // 无论是新吸引还是维持锁定，门槛都不超过它。提前剪掉可以省掉随后的遮挡射线，
            // 而遮挡射线是本方法唯一的重活（每个候选一次带碰撞箱的体素遍历）。
            if (angleDeg > EXIT_RADIUS_DEGREES) continue;

            out.add(new SnapDecision.Candidate(
                    e.getId(),
                    direction.x, direction.y, direction.z,
                    distance,
                    angleDeg,
                    hasLineOfSight(mc, player, eyePos, aimPoint),
                    true));
        }

        return out;
    }

    /**
     * 是否可作为吸附候选。
     *
     * <p>复用 {@link CombatTargeting#isHostileTarget} —— 与近防炮同一套敌对口径。
     */
    private static boolean isSnapCandidate(Player player, Entity e) {
        if (e == player || !e.isAlive() || e.isSpectator()) return false;
        if (!(e instanceof LivingEntity || e instanceof AircraftEntity)) return false;
        if (e instanceof Container) return false;
        return CombatTargeting.isHostileTarget(player, e);
    }

    /**
     * 眼睛到瞄准点之间是否没有方块/水面遮挡。
     *
     * <p>这是首版最大的漏洞：{@code ProjectileUtil.getEntityHitResult} 只会做
     * 「射线 vs 实体包围盒」求交，对墙体完全无感知，于是能穿过山体吸到背后的怪。
     * 这里用与瞄准镜落点同一套 {@link ClipContext}（{@code Block.COLLIDER} + {@code Fluid.ANY}），
     * 保证「能吸上的目标」与「炮弹能打到的目标」在遮挡口径上一致。
     *
     * <p>用 {@code Fluid.ANY} 而不是 {@code Fluid.NONE}：水面必须算遮挡，否则潜艇在水下
     * 也能被准星吸住 —— 而炮弹打不到水下（见策划决策/武器/02 潜艇水下发射鱼雷）。
     */
    private static boolean hasLineOfSight(Minecraft mc, Player player, Vec3 eyePos, Vec3 aimPoint) {
        ClipContext ctx = new ClipContext(eyePos, aimPoint,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player);
        BlockHitResult hit = mc.level.clip(ctx);
        // 命中点与视线起点重合说明玩家的眼睛本身就埋在方块/水面上（贴墙、潜水、卡在方块里）。
        // 此时任何目标都会被判成「被挡」，吸附会毫无道理地全面失效 —— 这不是遮挡，是判据失效，
        // 一律放行，让射击的其它环节去处理这种异常站位。
        if (hit.getType() == HitResult.Type.MISS) return true;
        return hit.getLocation().distanceToSqr(eyePos) < 1.0e-4;
    }

    /**
     * 客户端可用的「搜索半径上限」缓存（格）。
     *
     * <p>WHY 要缓存而不是现算：模拟距离只有服务端才知道（{@code MinecraftServer#getPlayerList}），
     * 而本类是客户端类。直接去取会踩两条线：一是触发 ArchitectureTest 的
     * 「client 不得依赖 server」规则，二是集成服里客户端拿到的 {@code getServer()} 常为 null。
     * 所以改由服务端把 {@code 模拟距离*16 - 8} 算好、随火控雷达的开关包一起下发，
     * 客户端只读缓存。（单人存档客户端本身就是集成服，也会走同一条下发路径。）
     *
     * <p><b>它只是视野上限，不是「目标存在」的证明</b>：这个值是服务端按<b>玩家</b>的模拟距离
     * 算的，跟客户端哪几个区块已经建出来没有关系。所以候选还要额外过
     * {@code level.isLoaded} —— 别再把这里当成存在性校验（首版就是这么错的）。
     *
     * <p>初值取 0：未收到任何下发前不做任何吸附，宁可先不吸，也不要用一个猜出来的距离
     * 吸到客户端根本加载不了的实体上。
     */
    private static double serverSimulationLimitBlocks = 0.0;

    /** 服务端下发模拟距离上限。由 {@code ToggleFcRadarPayload} 在处理开关时调用。 */
    public static void setServerSimulationLimitBlocks(double blocks) {
        serverSimulationLimitBlocks = Math.max(0.0, blocks);
    }

    /**
     * 把玩家视角朝目标转过去，每 tick 限速。
     *
     * <p>用「当前视角 → 目标」的<b>绝对</b>角度差算出本 tick 该转多少，再拆成
     * （俯仰、偏航）两个原始量交给 {@link Entity#turn}。因为每帧都重新解算绝对角，
     * 目标移动时准星会持续跟住，不会因为累加误差而漂移。
     *
     * <p><b>俯仰必须钳制在 ±90° 以内</b>：{@code turn} 内部对 xRot 做了钳制，
     * 但对 xRotO 没做。若送进一个越界的俯仰量，xRot 停在下界而 xRotO 继续朝上界走，
     * 两者分道扬镳导致渲染插值甩出一个巨大假旋转（首版的「吸到奇奇怪怪的地方」）。
     * 这里先钳制解算结果，让两个字段永远一致。
     */
    private static void aimAt(Minecraft mc, Player player, SnapDecision.Candidate target) {
        Vec3 eyePos = player.getEyePosition();
        // 方向向量是长度的倍数，乘回距离即得眼睛→瞄准点的位移。
        // 不重新查实体、直接复用候选里的方向/距离：候选是同一个 tick 里算出来的，
        // 中途重取位置会让「算角度用的点」和「转向用的点」错开。
        Vec3 toTarget = new Vec3(target.directionX(), target.directionY(), target.directionZ())
                .scale(target.distance());

        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

        // 目标绝对角度（与 Entity 的约定一致：yaw 由 atan2(-x, z) 得出，pitch 向下为正）
        float desiredYaw = (float) (Mth.atan2(-toTarget.x, toTarget.z) * (180.0 / Math.PI));
        float desiredPitch = Mth.clamp(
                (float) (-Mth.atan2(toTarget.y, horizontal) * (180.0 / Math.PI)), -90.0f, 90.0f);

        float deltaYaw = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float deltaPitch = Mth.clamp(desiredPitch - player.getXRot(), -90.0f, 90.0f);

        // 死区：已经对准就别再转，否则会在目标中心左右各摆一下，看起来像抖动
        if (Math.abs(deltaYaw) < TURN_EPSILON_DEGREES && Math.abs(deltaPitch) < TURN_EPSILON_DEGREES) return;

        float stepYaw = Mth.clamp(deltaYaw, -MAX_TURN_DEGREES_PER_TICK, MAX_TURN_DEGREES_PER_TICK);
        float stepPitch = Mth.clamp(deltaPitch, -MAX_TURN_DEGREES_PER_TICK, MAX_TURN_DEGREES_PER_TICK);

        // 除数只保留 0.15 —— 这是 turn 内部唯一的固定换算（见 Entity#turn：入参 ×0.15）。
        // 千万别把原版鼠标的 effSens³ 也乘进来：那个系数在原版里是乘在「鼠标像素增量」上的
        // （MouseHandler#turnPlayer: d3 = effSens³，开镜用 d3、非开镜再 ×8），
        // 属于送进 turn 的分子。放到分母上会变成「每 tick 实转 stepDeg / effSens³」，
        // 灵敏度 0.5（默认）时 10° 限速被放大成 80°，灵敏度 0 时直接饱和到 ±90° ——
        // 既复现了首版「吸到奇奇怪怪的地方」，又和本类「手感与玩家灵敏度无关」的设计相反。
        final double divisor = 0.15;

        // turn(偏航原始量, 俯仰原始量) —— 顺序不能反。
        // Entity#turn(double p1, double p2) 内部是 setXRot(± p2*0.15)、setYRot(± p1*0.15)，
        // 即第一形参喂偏航、第二形参喂俯仰；MouseHandler#turnPlayer 也是
        // player.turn(鼠标X增量, 鼠标Y增量) —— 鼠标左右动改的是 yaw，交叉印证。
        // 传反的后果不是「转慢点」而是「横竖各转错一根轴」，准星会横着甩、竖直乱跳。
        player.turn(stepYaw / divisor, stepPitch / divisor);

        // 视角旋转原本由 ServerboundMovePlayerPacket 在客户端玩家 tick 里同步给服务端
        // （见 LocalPlayer#tick 内的 sendPosition / sendIsSprintingIfNeeded 调用链），
        // 而本处理器挂在 ClientTickEvent.Post —— 也就是那个同步环节之后。
        // 不补这一发，吸附转出来的角度要等下一个 tick 才到服务端，而炮弹的落点是服务端
        // 用「收到开火包那一刻服务端自认的视角」算的：手感上就是准星已经压在目标上，
        // 炮弹却按上一 tick 的偏角飞出去。只有真玩家才有这个上行动作；假玩家
        // （FakePlayer / 集成服内其它玩家）不发包。
        if (player instanceof LocalPlayer) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                    player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), player.onGround()));
        }
    }
}
