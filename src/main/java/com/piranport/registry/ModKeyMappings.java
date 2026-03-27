package com.piranport.registry;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    private static final String CATEGORY = "key.categories.piranport";

    public static final KeyMapping FIRE_CONTROL_LOCK =
            new KeyMapping("key.piranport.fire_control_lock", GLFW.GLFW_KEY_P, CATEGORY);

    public static final KeyMapping FIRE_CONTROL_ADD =
            new KeyMapping("key.piranport.fire_control_add", GLFW.GLFW_KEY_O, CATEGORY);

    public static final KeyMapping FIRE_CONTROL_CANCEL =
            new KeyMapping("key.piranport.fire_control_cancel", GLFW.GLFW_KEY_I, CATEGORY);
}
