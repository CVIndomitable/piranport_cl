package com.piranport.dungeon.block;

import com.piranport.config.ModEquipmentConfig;
import net.minecraft.util.StringRepresentable;

/**
 * 书台纹路三态（策划决策/副本/17 §3.1）。
 *
 * <ul>
 *   <li>WHITE — 空书台，未插钥匙</li>
 *   <li>RED — 已插钥匙，副本建造中；或该实例在线人数已达进本门槛；或钥匙无法建出实例</li>
 *   <li>GREEN — 建造完成且未满员，唯一允许进本的状态</li>
 * </ul>
 *
 * <p>只做外观与进本门槛判定，纯函数便于单测。
 */
public enum LecternPattern implements StringRepresentable {
    WHITE("white", 0xFFFFFF),
    RED("red", 0xE04040),
    GREEN("green", 0x50D060);

    /**
     * 进本门槛 = 单实例同时在线人数上限（副本/22 §四）。
     *
     * <p>每次实时读取终端参数 {@code global.dungeon_capacity.max_players}，默认 4。刻意不缓存成
     * 编译期常量：策划在终端改门槛后应立即生效，不能等重启。注意它与缩放钳制
     * {@code global.dungeon_scaling.scaling_player_cap} 是两个概念——这里只管「能不能进」。</p>
     */
    public static int instanceCapacity() {
        return ModEquipmentConfig.DUNGEON_CAPACITY_MAX_PLAYERS.get();
    }

    private final String name;
    private final int tint;

    LecternPattern(String name, int tint) {
        this.name = name;
        this.tint = tint;
    }

    @Override
    public String getSerializedName() { return name; }

    /** 纹路面的乘色（BlockColor）。白色 = 不改原贴图。 */
    public int tint() { return tint; }

    /** 书台此刻的进本阻塞原因。 */
    public enum Status { EMPTY, BUILDING, FULL, INVALID, READY }

    /**
     * @param hasKey     书台上是否插着钥匙
     * @param hasInstance 钥匙是否已绑定到一个存在的实例
     * @param built      实例起点地形是否已建造完成
     * @param present    实例当前在线人数
     */
    public static Status status(boolean hasKey, boolean hasInstance, boolean built, int present) {
        if (!hasKey) return Status.EMPTY;
        if (!hasInstance) return Status.INVALID;
        if (!built) return Status.BUILDING;
        if (present >= instanceCapacity()) return Status.FULL;
        return Status.READY;
    }

    public static LecternPattern of(Status status) {
        return switch (status) {
            case EMPTY -> WHITE;
            case READY -> GREEN;
            case BUILDING, FULL, INVALID -> RED;
        };
    }

    /** 建造中钥匙锁定：只有建造完成（含满员）或钥匙无效时才允许空手潜行撤下。 */
    public static boolean canWithdraw(Status status) {
        return status == Status.FULL || status == Status.READY || status == Status.INVALID;
    }
}
