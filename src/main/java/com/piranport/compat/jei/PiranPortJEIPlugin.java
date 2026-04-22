package com.piranport.compat.jei;

import com.piranport.PiranPort;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class PiranPortJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return UID; }
}
