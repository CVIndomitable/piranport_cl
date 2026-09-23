package com.piranport.item;

import com.piranport.platform.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TorpedoItem extends Item {
    private final int caliber;
    private final float damage;
    private final int range;        // 航程（原始单位，×20=生存ticks）
    private final float speed;      // 鱼雷速度（blocks/tick）
    private final boolean magnetic;
    private final boolean wireGuided;
    private final boolean acoustic;
    private final boolean oxygen;   // Phase 27：策划 §3.3 氧气鱼雷
    /**
     * 型号 ID = 本物品的注册名（不带命名空间，如 {@code torpedo_533mm_mk14}）。
     *
     * <p>WHY 由注册点传入而不是 {@code builtInRegistryHolder().getKey()} 反查：
     * 构造发生在 {@code DeferredRegister} 的 supplier 里，此时物品尚未注册进
     * {@code BuiltInRegistries}，反查拿到的是 null，且反查会拖慢每个鱼雷物品的构造。
     * 注册点本来就知道自己的注册名，写死一个字符串零成本、零歧义。
     *
     * <p>旧构造器（只传口径的那几个）不传它，此时为 null——{@code torpedoSpeedDelta}
     * 对 null 返回 0，即「不可覆盖」，不会误命中别的型号。
     */
    private final String modelKey;

    public TorpedoItem(Properties properties, int caliber) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.0f, false, false, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.0f, magnetic, false, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.0f, magnetic, wireGuided, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided, boolean acoustic) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, acoustic ? 0.7f : (caliber >= 610 ? 1.0f : 1.0f),
                magnetic, wireGuided, acoustic, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided,
                       boolean acoustic, boolean oxygen) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, acoustic ? 0.7f : (caliber >= 610 ? 1.0f : 1.0f),
                magnetic, wireGuided, acoustic, oxygen);
    }

    /*
     * ===== 带 modelKey 的简写构造器 =====
     *
     * WHY 需要这一组：上面的简写构造器只传口径/制导标志，跑完参数后 modelKey 落到 null，
     * 于是这些型号在调试终端里根本不可覆盖。注册点本来就知道自己的注册名，
     * 让它把 ID 写在末尾，比让每个注册点展开成 10 参的完整构造器短得多也好读。
     * 基准航速必须与上面同签名构造器保持一致（口径 610 及以上 28 伤害，声自导 0.7 速），
     * 否则同一个型号「有没有传 ID」会算出两种航速。
     */

    public TorpedoItem(Properties properties, int caliber, @Nullable String modelKey) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, 1.0f, false, false, false, false, modelKey);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, @Nullable String modelKey) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, 1.0f, magnetic, false, false, false, modelKey);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided,
                       @Nullable String modelKey) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, 1.0f, magnetic, wireGuided, false, false, modelKey);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided,
                       boolean acoustic, @Nullable String modelKey) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, acoustic ? 0.7f : 1.0f, magnetic, wireGuided, acoustic, false, modelKey);
    }

    public TorpedoItem(Properties properties, int caliber, float damage, int range, float speed,
                        boolean magnetic, boolean wireGuided, boolean acoustic, boolean oxygen) {
        this(properties, caliber, damage, range, speed, magnetic, wireGuided, acoustic, oxygen, null);
    }

    /**
     * 完整构造器。
     *
     * @param modelKey 型号 ID（本物品的注册名，不带命名空间），供调试终端按型号覆盖航速；
     *                 传 null 表示该型号不做速度覆盖。
     */
    public TorpedoItem(Properties properties, int caliber, float damage, int range, float speed,
                        boolean magnetic, boolean wireGuided, boolean acoustic, boolean oxygen,
                        @Nullable String modelKey) {
        super(properties);
        this.caliber = caliber;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
        this.magnetic = magnetic;
        this.wireGuided = wireGuided;
        this.acoustic = acoustic;
        this.oxygen = oxygen;
        this.modelKey = modelKey;
    }

    public int getCaliber() {
        return caliber;
    }

    public float getDamage() {
        return damage;
    }

    public int getRange() {
        return range;
    }

    /** 航程转换为 lifetime ticks。 */
    public int getLifetimeTicks() {
        return range * 20;
    }

    /** 基准航速，不含调试终端覆盖。 */
    public float getBaseSpeed() {
        return speed;
    }

    /**
     * 实机航速 = 基准值 + 调试终端覆盖。
     *
     * <p>WHY 覆盖加在这里而不是去改 {@code speed} 字段：{@code TorpedoItem} 是注册表里的
     * 单例，字段是全局的。直接改字段会污染同型号的所有鱼雷、且存档间互相串味。
     * 覆盖值存在 {@link com.piranport.terminal.TerminalOverrides} 的静态快照里，
     * 由服务端在改动时同步给客户端，这里按需查表。
     */
    public float getSpeed() {
        return speed + com.piranport.terminal.TerminalOverrides.torpedoSpeedDelta(modelKey);
    }

    /** 鱼雷型号 ID（= 注册名，如 {@code torpedo_533mm_mk14}），供调试终端做型号级覆盖。 */
    public String getModelKey() {
        return modelKey;
    }

    public boolean isMagnetic() {
        return magnetic;
    }

    public boolean isWireGuided() {
        return wireGuided;
    }

    public boolean isAcoustic() {
        return acoustic;
    }

    public boolean isOxygen() {
        return oxygen;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.piranport.ammo_type.torpedo")
                .withStyle(ChatFormatting.DARK_GREEN));
        if (ClientHooks.isClient()) {
            if (ClientHooks.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.damage",
                        String.format("%.1f", damage)).withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.range",
                        range).withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.speed",
                        String.format("%.2f", getSpeed())).withStyle(ChatFormatting.GREEN));
                if (magnetic) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.magnetic")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                if (wireGuided) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.wire_guided")
                            .withStyle(ChatFormatting.YELLOW));
                }
                if (acoustic) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.acoustic")
                            .withStyle(ChatFormatting.GOLD));
                }
                if (oxygen) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.oxygen")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
