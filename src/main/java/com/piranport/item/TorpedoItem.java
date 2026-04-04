package com.piranport.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TorpedoItem extends Item {
    private final int caliber;
    private final float damage;
    private final int range;        // 航程（原始单位，×20=生存ticks）
    private final float speed;      // 鱼雷速度（blocks/tick）
    private final boolean magnetic;
    private final boolean wireGuided;
    private final boolean acoustic;

    public TorpedoItem(Properties properties, int caliber) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.2f, false, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.2f, magnetic, false, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, caliber >= 610 ? 1.0f : 1.2f, magnetic, wireGuided, false);
    }

    public TorpedoItem(Properties properties, int caliber, boolean magnetic, boolean wireGuided, boolean acoustic) {
        this(properties, caliber, caliber >= 610 ? 28f : 18f,
                60, acoustic ? 1.0f : (caliber >= 610 ? 1.0f : 1.2f), magnetic, wireGuided, acoustic);
    }

    public TorpedoItem(Properties properties, int caliber, float damage, int range, float speed,
                        boolean magnetic, boolean wireGuided, boolean acoustic) {
        super(properties);
        this.caliber = caliber;
        this.damage = damage;
        this.range = range;
        this.speed = speed;
        this.magnetic = magnetic;
        this.wireGuided = wireGuided;
        this.acoustic = acoustic;
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

    public float getSpeed() {
        return speed;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.piranport.ammo_type.torpedo")
                .withStyle(ChatFormatting.DARK_GREEN));
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.damage",
                        String.format("%.1f", damage)).withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.range",
                        range).withStyle(ChatFormatting.AQUA));
                tooltipComponents.add(Component.translatable("tooltip.piranport.torpedo.speed",
                        String.format("%.2f", speed)).withStyle(ChatFormatting.GREEN));
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
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
