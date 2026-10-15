package com.piranport.dungeon.client;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 副本节点图渲染器：DungeonBookScreen 的 NODE_MAP 与 DungeonContinueScreen 共用，
 * 保证两屏的三态配色/命中判定完全一致（需求：继续界面也要有同款节点图）。
 *
 * <p>状态读自钥匙 component（{@link DungeonProgress}），事件驱动：
 * 服务端每次状态变化时 syncKey 写钥匙，客户端只在打开/重渲染时读取，不周期重读。</p>
 *
 * <p>三态：
 * <ul>
 *   <li>cleared 绿色实心</li>
 *   <li>entered−cleared 黄色描边（已进入未通关）</li>
 *   <li>未访问 灰色实心</li>
 *   <li>reachable（可从 cleared 经 edges 到达但未进入）叠加青色高亮描边</li>
 *   <li>currentNode 叠加金色描边</li>
 * </ul>
 */
public final class DungeonNodeMapRenderer {
    /** 节点半径（像素）。命中判定也用同一半径，保证点得到的一定看得见。 */
    public static final int NODE_RADIUS = 7;

    private static final int COLOR_CLEARED   = 0xFF4CAF50;
    private static final int COLOR_ENTERED   = 0xFFFFC107;
    private static final int COLOR_UNVISITED = 0xFF6B6B7B;
    private static final int COLOR_REACHABLE = 0xFF4DD0E1;
    private static final int COLOR_CURRENT   = 0xFFFFD700;
    private static final int COLOR_EDGE      = 0xFF6A6A8A;
    private static final int COLOR_TEXT      = 0xFFE8E8F0;

    private DungeonNodeMapRenderer() {}

    /** 把 displayX/displayY（0..400 设计坐标）等比缩放进给定区域，取二者较小的缩放系数并居中。 */
    public static Map<String, int[]> layout(StageData stage, int areaX, int areaY, int areaW, int areaH) {
        Map<String, int[]> out = new HashMap<>();
        if (stage == null || areaW <= 0 || areaH <= 0) return out;
        // 设计坐标系按 400×400 处理；displayX/displayY 超出范围会被裁到区域内边缘
        double scale = Math.min(areaW / 400.0, areaH / 400.0);
        int offsetX = areaX + (int) ((areaW - 400 * scale) / 2);
        int offsetY = areaY + (int) ((areaH - 400 * scale) / 2);
        for (var e : stage.nodes().entrySet()) {
            NodeData n = e.getValue();
            int px = offsetX + (int) (n.displayX() * scale);
            int py = offsetY + (int) (n.displayY() * scale);
            out.put(e.getKey(), new int[]{px, py});
        }
        return out;
    }

    /** 计算可达集合：从全部 cleared 节点经 edges 走一步能到、且未 cleared 的节点。 */
    public static Set<String> computeReachable(StageData stage, DungeonProgress progress) {
        if (stage == null) return Set.of();
        Set<String> cleared = progress.clearedNodes();
        Set<String> reachable = new HashSet<>();
        if (cleared.isEmpty()) {
            if (stage.startNode() != null) reachable.add(stage.startNode());
        } else {
            for (String c : cleared) {
                reachable.addAll(stage.getReachableFrom(c));
            }
            reachable.removeAll(cleared);
        }
        return reachable;
    }

    /**
     * 画边 + 画节点 + 画节点 ID。positions 由 {@link #layout} 给出。
     */
    public static void render(GuiGraphics gfx, Font font, StageData stage,
                              Map<String, int[]> positions, DungeonProgress progress,
                              int mouseX, int mouseY) {
        if (stage == null || positions.isEmpty()) return;
        Set<String> cleared = progress.clearedNodes();
        Set<String> entered = progress.enteredNodes();
        Set<String> reachable = computeReachable(stage, progress);
        String current = progress.currentNode();

        // 边先画，节点盖在端点上
        for (var edge : stage.edges()) {
            int[] a = positions.get(edge.from());
            int[] b = positions.get(edge.to());
            if (a != null && b != null) {
                drawLine(gfx, a[0], a[1], b[0], b[1], COLOR_EDGE);
            }
        }

        for (var e : positions.entrySet()) {
            String nodeId = e.getKey();
            int[] p = e.getValue();
            int x = p[0], y = p[1];
            NodeData node = stage.nodes().get(nodeId);
            if (node == null) continue;

            boolean isCleared = cleared.contains(nodeId);
            boolean isEntered = entered.contains(nodeId) && !isCleared;
            boolean isReachable = reachable.contains(nodeId);
            boolean isCurrent = nodeId.equals(current);

            // 填充：cleared 绿 / entered 灰底待描边 / 未访问灰
            int fill = isCleared ? COLOR_CLEARED : COLOR_UNVISITED;
            fillCircle(gfx, x, y, NODE_RADIUS, fill);

            // entered（未通关）黄描边
            if (isEntered) outlineCircle(gfx, x, y, NODE_RADIUS, COLOR_ENTERED);
            // reachable 高亮描边（可点的视觉提示）
            if (isReachable && !isCleared) outlineCircle(gfx, x, y, NODE_RADIUS + 1, COLOR_REACHABLE);
            // currentNode 金描边
            if (isCurrent) outlineCircle(gfx, x, y, NODE_RADIUS + 2, COLOR_CURRENT);

            // 鼠标悬停加亮
            if (Math.hypot(mouseX - x, mouseY - y) <= NODE_RADIUS + 2) {
                outlineCircle(gfx, x, y, NODE_RADIUS + 2, 0xFFFFFFFF);
            }

            // 节点类型小标记：Boss 画个星位点
            if (node.type() == NodeData.NodeType.BOSS) {
                gfx.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFF5252);
            }

            gfx.drawString(font, nodeId, x - font.width(nodeId) / 2, y + NODE_RADIUS + 3,
                    COLOR_TEXT, false);
        }
    }

    /**
     * 圆形命中检测：返回鼠标命中的节点 id，未命中返回 null。
     * 命中半径比绘制半径略大，便于点击小节点。
     */
    public static String hitTest(Map<String, int[]> positions, double mouseX, double mouseY) {
        String best = null;
        double bestD = Double.MAX_VALUE;
        for (var e : positions.entrySet()) {
            int[] p = e.getValue();
            double d = Math.hypot(mouseX - p[0], mouseY - p[1]);
            if (d <= NODE_RADIUS + 4 && d < bestD) {
                bestD = d;
                best = e.getKey();
            }
        }
        return best;
    }

    /**
     * 图例：一行五个色块 + 标签，从左到右依次说明三态/可达/当前。
     * 放在节点图下方（Screen 自己找位置调用），玩家不用记配色含义。
     */
    public static void renderLegend(GuiGraphics gfx, Font font, int x, int y) {
        int swatch = 8;
        int gap = 4;
        int itemGap = 14;
        int cy = y + swatch / 2;

        int cur = x;
        cur = legendItem(gfx, font, cur, cy, swatch, gap, itemGap,
                g -> fillCircle(g, 0, 0, swatch / 2, COLOR_CLEARED), "已通关");
        cur = legendItem(gfx, font, cur, cy, swatch, gap, itemGap,
                g -> { fillCircle(g, 0, 0, swatch / 2, COLOR_UNVISITED);
                       outlineCircle(g, 0, 0, swatch / 2, COLOR_ENTERED); }, "进行中");
        cur = legendItem(gfx, font, cur, cy, swatch, gap, itemGap,
                g -> fillCircle(g, 0, 0, swatch / 2, COLOR_UNVISITED), "未访问");
        cur = legendItem(gfx, font, cur, cy, swatch, gap, itemGap,
                g -> { fillCircle(g, 0, 0, swatch / 2, COLOR_UNVISITED);
                       outlineCircle(g, 0, 0, swatch / 2 + 1, COLOR_REACHABLE); }, "可前往");
        legendItem(gfx, font, cur, cy, swatch, gap, itemGap,
                g -> { fillCircle(g, 0, 0, swatch / 2, COLOR_UNVISITED);
                       outlineCircle(g, 0, 0, swatch / 2 + 2, COLOR_CURRENT); }, "当前");
    }

    /** 画一个图例项：色块（以 (0,0) 为圆心的回调）+ 标签；返回下一项起点 x。 */
    private static int legendItem(GuiGraphics gfx, Font font, int x, int cy, int swatch,
                                  int gap, int itemGap,
                                  java.util.function.Consumer<GuiGraphics> draw,
                                  String label) {
        // PoseStack 平移让色块回调围绕自身圆心作画，比让回调里写偏移干净
        gfx.pose().pushPose();
        gfx.pose().translate(x + swatch / 2.0, cy, 0);
        draw.accept(gfx);
        gfx.pose().popPose();
        gfx.drawString(font, label, x + swatch + gap, cy - font.lineHeight / 2,
                COLOR_TEXT, false);
        return x + swatch + gap + font.width(label) + itemGap;
    }

    /** Bresenham 画线（与旧实现一致，1px 像素点）。 */
    private static void drawLine(GuiGraphics gfx, int x1, int y1, int x2, int y2, int color) {
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        int maxIterations = 10000;
        int iterations = 0;
        while (true) {
            gfx.fill(x1, y1, x1 + 1, y1 + 1, color);
            if (x1 == x2 && y1 == y2) break;
            if (++iterations > maxIterations) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx)  { err += dx; y1 += sy; }
        }
    }

    private static void fillCircle(GuiGraphics gfx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt(r * r - dy * dy);
            gfx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static void outlineCircle(GuiGraphics gfx, int cx, int cy, int r, int color) {
        // 中点圆算法，1px 描边
        int x = r, y = 0, err = 1 - r;
        while (x >= y) {
            plot8(gfx, cx, cy, x, y, color);
            y++;
            if (err < 0) {
                err += 2 * y + 1;
            } else {
                x--;
                err += 2 * (y - x) + 1;
            }
        }
    }

    private static void plot8(GuiGraphics gfx, int cx, int cy, int x, int y, int color) {
        gfx.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, color);
        gfx.fill(cx - x, cy + y, cx - x + 1, cy + y + 1, color);
        gfx.fill(cx + x, cy - y, cx + x + 1, cy - y + 1, color);
        gfx.fill(cx - x, cy - y, cx - x + 1, cy - y + 1, color);
        gfx.fill(cx + y, cy + x, cx + y + 1, cy + x + 1, color);
        gfx.fill(cx - y, cy + x, cx - y + 1, cy + x + 1, color);
        gfx.fill(cx + y, cy - x, cx + y + 1, cy - x + 1, color);
        gfx.fill(cx - y, cy - x, cx - y + 1, cy - x + 1, color);
    }

    /** 便捷工具：取玩家背包里某槽位的钥匙进度（找不到钥匙时返回 EMPTY）。 */
    public static DungeonProgress progressFromSlot(int keySlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return DungeonProgress.EMPTY;
        var stack = mc.player.getInventory().getItem(keySlot);
        if (stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
            return stack.getOrDefault(
                    com.piranport.registry.ModDataComponents.DUNGEON_PROGRESS.get(),
                    DungeonProgress.EMPTY);
        }
        return DungeonProgress.EMPTY;
    }

    /** 便捷工具：取讲台上插着的钥匙进度（客户端讲台 BE 会同步 KeyStack）。 */
    public static DungeonProgress progressFromLectern(net.minecraft.core.BlockPos lecternPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || lecternPos == null) return DungeonProgress.EMPTY;
        if (mc.level.getBlockEntity(lecternPos)
                instanceof com.piranport.dungeon.block.DungeonLecternBlockEntity be) {
            var stack = be.getKeyStack();
            if (stack.getItem() instanceof com.piranport.dungeon.key.DungeonKeyItem) {
                return com.piranport.dungeon.key.DungeonKeyItem.getProgress(stack);
            }
        }
        return DungeonProgress.EMPTY;
    }
}
