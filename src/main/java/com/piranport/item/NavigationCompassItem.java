package com.piranport.item;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.Set;

/**
 * 航海罗盘（海图/04）。
 *
 * <p>指针本身完全复用原版指南针的 {@code LODESTONE_TRACKER} +
 * {@code minecraft:compass} 模型属性，所以「有目标就指、没目标就乱转」不需要自己做渲染。
 * 本类只负责在服务端选目标并把坐标写进组件。tracked 必须为 false：原版
 * {@link LodestoneTracker#tick} 会检查目标位置是否有磁石，而这里指向的是结构或实体。</p>
 *
 * <p>遗迹坐标单独缓存在 {@code navigation_ruin} 里，而不是直接复用指针组件，
 * 否则 ship 模式每 20 tick 覆盖指针后，切回 default 就得重新做一次 20000 格结构搜索。</p>
 */
public class NavigationCompassItem extends CompassItem {
    public static final String MODE_DEFAULT = "default";
    public static final String MODE_SHIP = "ship";

    /** 海图/04：default 在 20000 格内找 abyssal_base。原版搜索参数以区块计。 */
    private static final int RUIN_SEARCH_RADIUS_BLOCKS = 20_000;
    private static final int RUIN_SEARCH_RADIUS_CHUNKS = RUIN_SEARCH_RADIUS_BLOCKS / 16;
    private static final long RUIN_SEARCH_RADIUS_SQR =
            (long) RUIN_SEARCH_RADIUS_BLOCKS * RUIN_SEARCH_RADIUS_BLOCKS;
    /** 没搜到时每 10 秒重试一次，避免结构搜索把服务器卡死。 */
    private static final int RUIN_SEARCH_RETRY_TICKS = 200;
    /** ship 模式按文档每 20 tick 重判一次。 */
    private static final int SHIP_RESCAN_TICKS = 20;
    private static final int SHIP_SEARCH_RADIUS = 128;

    private static final ResourceKey<Structure> ABYSSAL_BASE_KEY =
            ResourceKey.create(Registries.STRUCTURE,
                    ResourceLocation.fromNamespaceAndPath("piranport", "abyssal_base"));

    /**
     * 文档点名的十种深海舰船。用注册 ID 而不是 EntityType 静态字段，
     * 避免物品类在实体注册完成前被加载时触发 DeferredHolder#get()。
     */
    private static final Set<String> TRACKED_SHIP_IDS = Set.of(
            "piranport:deep_ocean_supply",
            "piranport:deep_ocean_destroyer",
            "piranport:deep_ocean_light_cruiser",
            "piranport:deep_ocean_heavy_cruiser",
            "piranport:deep_ocean_battle_cruiser",
            "piranport:deep_ocean_battleship",
            "piranport:deep_ocean_light_carrier",
            "piranport:deep_ocean_carrier",
            "piranport:deep_ocean_submarine",
            "piranport:deep_ocean_boss");

    public NavigationCompassItem(Properties properties) {
        super(properties);
    }

    public static String mode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.NAVIGATION_MODE.get(), MODE_DEFAULT);
    }

    public static boolean isShipMode(ItemStack stack) {
        return MODE_SHIP.equals(mode(stack));
    }

    /** ship 模式才有附魔光效；default 即使带有目标坐标也不发光。 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return isShipMode(stack);
    }

    /** 原版指南针有目标时会改名成「磁石指南针」，这里必须固定回本物品名。 */
    @Override
    public String getDescriptionId(ItemStack stack) {
        return getDescriptionId();
    }

    /** 不潜行时完全放手，让书台等方块的右键交互照常执行（副本/17）。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(held);
        }
        if (!level.isClientSide) {
            String next = isShipMode(held) ? MODE_DEFAULT : MODE_SHIP;
            held.set(ModDataComponents.NAVIGATION_MODE.get(), next);
            // 模式切换先清掉旧指针，下一 tick 按新模式重新选目标，避免闪一下旧方向。
            held.remove(DataComponents.LODESTONE_TRACKER);
            player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), player, context.getHand()).getResult();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        // 只处理主手/副手：背包里的罗盘不停搜索会白耗性能，也没有指针可看。
        if (stack != player.getMainHandItem() && stack != player.getOffhandItem()) {
            return;
        }
        updateTarget(serverLevel, player, stack);
    }

    private static void updateTarget(ServerLevel level, ServerPlayer player, ItemStack stack) {
        boolean inDungeon = DungeonEventHandler.isInDungeon(player);
        boolean inOverworld = level.dimension() == Level.OVERWORLD;
        // 下界、末地和其他模组维度一律乱转。
        if (!inDungeon && !inOverworld) {
            clearTarget(stack);
            return;
        }

        if (isShipMode(stack)) {
            if (player.tickCount % SHIP_RESCAN_TICKS != 0) {
                return;
            }
            Entity nearest = findNearestShip(level, player);
            setTarget(stack, nearest == null ? null
                    : GlobalPos.of(level.dimension(), nearest.blockPosition()));
            return;
        }

        if (inDungeon) {
            setTarget(stack, dungeonNodeEnd(player));
            return;
        }

        GlobalPos ruin = cachedRuin(level, player, stack);
        if (ruin == null && player.tickCount % RUIN_SEARCH_RETRY_TICKS == 0) {
            ruin = searchAbyssalBase(level, player);
            if (ruin != null) {
                stack.set(ModDataComponents.NAVIGATION_RUIN.get(), ruin);
            }
        }
        setTarget(stack, ruin);
    }

    /** 读取缓存；遗迹被移除或玩家已离它 20000 格以上时丢弃缓存。 */
    private static GlobalPos cachedRuin(ServerLevel level, ServerPlayer player, ItemStack stack) {
        GlobalPos cached = stack.get(ModDataComponents.NAVIGATION_RUIN.get());
        if (cached == null) {
            return null;
        }
        long dx = (long) cached.pos().getX() - player.getBlockX();
        long dz = (long) cached.pos().getZ() - player.getBlockZ();
        if (dx * dx + dz * dz > RUIN_SEARCH_RADIUS_SQR) {
            stack.remove(ModDataComponents.NAVIGATION_RUIN.get());
            return null;
        }
        // 区块没加载时无法判断结构是否还在，先信缓存；等玩家靠近后再校验，
        // 否则每次路过未生成区域都会重跑一次大范围结构搜索。
        if (level.hasChunkAt(cached.pos())) {
            Optional<Holder.Reference<Structure>> structure = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE)
                    .getHolder(ABYSSAL_BASE_KEY);
            if (structure.isPresent()
                    && level.structureManager().getStructureWithPieceAt(
                            cached.pos(), structure.get().value()) == StructureStart.INVALID_START) {
                stack.remove(ModDataComponents.NAVIGATION_RUIN.get());
                return null;
            }
        }
        return cached;
    }

    private static GlobalPos searchAbyssalBase(ServerLevel level, ServerPlayer player) {
        Optional<Holder.Reference<Structure>> structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getHolder(ABYSSAL_BASE_KEY);
        if (structure.isEmpty()) {
            return null;
        }
        var pair = level.getChunkSource().getGenerator().findNearestMapStructure(
                level, HolderSet.direct(structure.get()), player.blockPosition(),
                RUIN_SEARCH_RADIUS_CHUNKS, false);
        if (pair == null) {
            return null;
        }
        BlockPos pos = pair.getFirst();
        // 指南针只用 x/z；把 y 固定到海平面附近，方便之后做「结构是否还在」的方块查询。
        return GlobalPos.of(level.dimension(), new BlockPos(pos.getX(), 63, pos.getZ()));
    }

    private static Entity findNearestShip(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(SHIP_SEARCH_RADIUS, 256.0, SHIP_SEARCH_RADIUS);
        Entity nearest = null;
        double nearestSqr = Double.MAX_VALUE;
        for (Entity candidate : level.getEntities(player, area,
                e -> e.isAlive() && isTrackedShip(e))) {
            double dx = candidate.getX() - player.getX();
            double dz = candidate.getZ() - player.getZ();
            double distance = dx * dx + dz * dz;
            if (distance <= (double) SHIP_SEARCH_RADIUS * SHIP_SEARCH_RADIUS
                    && distance < nearestSqr) {
                nearestSqr = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static boolean isTrackedShip(Entity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && TRACKED_SHIP_IDS.contains(id.toString());
    }

    /**
     * 副本 default 模式指向「当前节点结束位置」。
     * 战斗/Boss 节点的完成传送门生成在节点出生点正下方（NodeBattleField#spawnCompletionPortal），
     * 资源/消耗/迂回节点生成在出生点向东 4 格（DungeonNodeRouter#finishNonBattleNode）。
     */
    private static GlobalPos dungeonNodeEnd(ServerPlayer player) {
        DungeonInstance instance = DungeonInstanceManager.get((ServerLevel) player.level())
                .getInstanceForPlayer(player);
        if (instance == null) {
            return null;
        }
        String currentNode = instance.getCurrentNode();
        if (currentNode == null || instance.getClearedNodes().contains(currentNode)) {
            return null;
        }
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        if (stage == null) {
            return null;
        }
        NodeData node = stage.nodes().get(currentNode);
        if (node == null) {
            return null;
        }
        BlockPos spawn = instance.getNodeSpawnPos(currentNode);
        BlockPos end = switch (node.type()) {
            case BATTLE, BOSS -> spawn.below();
            default -> spawn.offset(4, 0, 0);
        };
        return GlobalPos.of(player.level().dimension(), end);
    }

    private static void setTarget(ItemStack stack, GlobalPos target) {
        if (target == null) {
            clearTarget(stack);
            return;
        }
        // tracked=false：目标不是磁石，交给原版 tick 校验会被当成失效而清空。
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(target), false));
    }

    private static void clearTarget(ItemStack stack) {
        if (stack.has(DataComponents.LODESTONE_TRACKER)) {
            stack.remove(DataComponents.LODESTONE_TRACKER);
        }
    }
}
