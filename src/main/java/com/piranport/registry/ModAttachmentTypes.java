package com.piranport.registry;

import com.mojang.serialization.Codec;
import com.piranport.PiranPort;
import com.piranport.handler.WaterWalkingState;
import com.piranport.rigging.RiggingLoadout;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachmentTypes {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PiranPort.MOD_ID);

    public static final Supplier<AttachmentType<Integer>> ACTIVE_SKIN =
            ATTACHMENT_TYPES.register("active_skin",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    public static final Supplier<AttachmentType<Integer>> ACTIVE_ENTITY_CORE =
            ATTACHMENT_TYPES.register("active_entity_core",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

    /**
     * 「已进入过副本」持久标记（策划决策/副本/17 §二）。第一次成功进入任意副本时写入，
     * 死亡、换钥匙、换书台都不清除（copyOnDeath）。书台遗迹开箱据此发 1-1 或金猫猫钥匙。
     */
    public static final Supplier<AttachmentType<Boolean>> ENTERED_DUNGEON =
            ATTACHMENT_TYPES.register("entered_dungeon",
                    () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build());

    /**
     * 《副本/18》ulan 彩蛋的当日计数：按玩家、按 UTC 地球日。
     * 只统计「新钥匙第一次成功进本」，死亡重进不重复计数；跨日由 day 字段判零。
     */
    public static final Supplier<AttachmentType<Long>> ULAN_DAILY_DAY =
            ATTACHMENT_TYPES.register("ulan_daily_day",
                    () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());
    public static final Supplier<AttachmentType<Integer>> ULAN_DAILY_COUNT =
            ATTACHMENT_TYPES.register("ulan_daily_count",
                    () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build());

    // 移动预测缓存属于各端的玩家实例，不持久化、不同步，也不复制到重生玩家。
    public static final Supplier<AttachmentType<WaterWalkingState>> WATER_WALKING =
            ATTACHMENT_TYPES.register("water_walking",
                    () -> AttachmentType.builder(WaterWalkingState::new).build());
    // 服务端权威、按需同步到本地玩家；死亡与退出时随玩家实例释放。
    public static final Supplier<AttachmentType<com.piranport.combat.AASilenceState>> AA_SILENCE =
            ATTACHMENT_TYPES.register("aa_silence",
                    () -> AttachmentType.builder(com.piranport.combat.AASilenceState::new).build());

    /**
     * 玩家舰装挂点快照：服务端 {@link com.piranport.handler.RiggingLoadoutHandler} 定时重建，
     * 内容变化才 setData（附件同步自动广播给看得见该玩家的客户端）。
     *
     * <p><b>只 sync 不 serialize</b>：值里是 count=1 的 ItemStack 展示副本，只服务渲染，
     * 不需要进存档；NeoForge 的 {@code AttachmentType.Builder#sync(StreamCodec)} 支持独立使用。
     * 非持久化附件在玩家重生时也不会被复制，服务端下一个周期会重新填上。
     */
    public static final Supplier<AttachmentType<RiggingLoadout>> RIGGING_LOADOUT =
            ATTACHMENT_TYPES.register("rigging_loadout",
                    () -> AttachmentType.builder(() -> RiggingLoadout.EMPTY)
                            .sync(RiggingLoadout.STREAM_CODEC)
                            .build());
}
