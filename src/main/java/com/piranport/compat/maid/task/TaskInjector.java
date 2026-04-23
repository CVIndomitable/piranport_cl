package com.piranport.compat.maid.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IMaidTask;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.google.common.collect.ImmutableMap;
import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TaskInjector {
    private static volatile boolean injected = false;
    private static volatile boolean failedPermanently = false;
    private static IMaidTask ourTask;

    private TaskInjector() {}

    @SubscribeEvent
    public static void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TaskInjector::tryInject);
    }

    public static synchronized boolean tryInject() {
        if (injected || failedPermanently) return injected;
        if (ourTask == null) ourTask = new TaskMaidRigging();
        try {
            Field mapField = TaskManager.class.getDeclaredField("TASK_MAP");
            Field idxField = TaskManager.class.getDeclaredField("TASK_INDEX");
            mapField.setAccessible(true);
            idxField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, IMaidTask> map = (Map<ResourceLocation, IMaidTask>) mapField.get(null);
            @SuppressWarnings("unchecked")
            List<IMaidTask> list = (List<IMaidTask>) idxField.get(null);
            if (map == null || list == null) {
                PiranPort.LOGGER.info("[Compat/Maid] TLM TaskManager not ready, deferring task injection");
                return false;
            }
            ResourceLocation uid = ourTask.getUid();
            if (!map.containsKey(uid)) {
                Map<ResourceLocation, IMaidTask> newMap = new HashMap<>(map);
                newMap.put(uid, ourTask);
                mapField.set(null, ImmutableMap.copyOf(newMap));

                List<IMaidTask> newList = new ArrayList<>(list);
                newList.add(ourTask);
                idxField.set(null, newList);

                PiranPort.LOGGER.info("[Compat/Maid] Registered maid task: {}", uid);
            }
            injected = true;
            return true;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            PiranPort.LOGGER.error("[Compat/Maid] Failed to inject maid task via reflection (TLM internal change?)", e);
            failedPermanently = true;
            return false;
        }
    }

    public static boolean isInjected() {
        return injected;
    }
}
