package com.piranport.combat;

import com.piranport.component.WeaponCooldown;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WeaponReloadDisplayTest {
    @Test
    void concurrentCooldownsProgressIndependently() {
        WeaponCooldown first = new WeaponCooldown(120, 20);
        WeaponCooldown second = new WeaponCooldown(140, 40);

        assertEquals(0.5f, first.getProgress(110));
        assertEquals(0.25f, second.getProgress(110));
        assertEquals(1.0f, first.getProgress(120));
        assertEquals(0.5f, second.getProgress(120));
        assertFalse(first.isOnCooldown(120));
        assertTrue(second.isOnCooldown(120));
    }

    @Test
    void itemBarsUseDepthIndependentOverlay() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/piranport/client/WeaponReloadDecorator.java"));
        assertEquals(3, source.split("gui\\.fill\\(RenderType\\.guiOverlay\\(\\)", -1).length - 1);
        assertFalse(source.contains("gui.fill(barX"));
    }
}
