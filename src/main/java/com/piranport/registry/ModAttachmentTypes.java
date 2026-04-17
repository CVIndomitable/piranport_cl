package com.piranport.registry;

import com.mojang.serialization.Codec;
import com.piranport.PiranPort;
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
}
