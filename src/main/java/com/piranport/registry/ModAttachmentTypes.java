package com.piranport.registry;

import com.mojang.serialization.Codec;
import com.piranport.PiranPort;
import com.piranport.handler.WaterWalkingState;
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

    // 移动预测缓存属于各端的玩家实例，不持久化、不同步，也不复制到重生玩家。
    public static final Supplier<AttachmentType<WaterWalkingState>> WATER_WALKING =
            ATTACHMENT_TYPES.register("water_walking",
                    () -> AttachmentType.builder(WaterWalkingState::new).build());
    // 服务端权威、按需同步到本地玩家；死亡与退出时随玩家实例释放。
    public static final Supplier<AttachmentType<com.piranport.combat.AASilenceState>> AA_SILENCE =
            ATTACHMENT_TYPES.register("aa_silence",
                    () -> AttachmentType.builder(com.piranport.combat.AASilenceState::new).build());
}
