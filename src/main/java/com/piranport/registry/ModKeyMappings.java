package com.piranport.registry;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    private static final String CATEGORY = "key.categories.piranport";

    public static final KeyMapping CYCLE_WEAPON =
            new KeyMapping("key.piranport.cycle_weapon", GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping FIRE_CONTROL_LOCK =
            new KeyMapping("key.piranport.fire_control_lock", GLFW.GLFW_KEY_P, CATEGORY);

    public static final KeyMapping FIRE_CONTROL_ADD =
            new KeyMapping("key.piranport.fire_control_add", GLFW.GLFW_KEY_O, CATEGORY);

    public static final KeyMapping FIRE_CONTROL_CANCEL =
            new KeyMapping("key.piranport.fire_control_cancel", GLFW.GLFW_KEY_I, CATEGORY);

    public static final KeyMapping OPEN_FLIGHT_GROUP =
            new KeyMapping("key.piranport.open_flight_group", GLFW.GLFW_KEY_U, CATEGORY);

    public static final KeyMapping HIGHLIGHT_ENTITIES =
            new KeyMapping("key.piranport.highlight_entities", GLFW.GLFW_KEY_Y, CATEGORY);

    public static final KeyMapping TOGGLE_AUTO_LAUNCH =
            new KeyMapping("key.piranport.toggle_auto_launch", GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping DEBUG_TOGGLE =
            new KeyMapping("key.piranport.debug_toggle", GLFW.GLFW_KEY_F8, CATEGORY);

    public static final KeyMapping MANUAL_RELOAD =
            new KeyMapping("key.piranport.manual_reload", GLFW.GLFW_KEY_R, CATEGORY);
}
