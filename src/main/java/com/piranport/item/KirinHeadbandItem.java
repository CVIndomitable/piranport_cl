package com.piranport.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;

/**
 * 基林级发带 — 戴在头上时给予恒定隐身效果。
 * 实现 Equipable 使其可右键装备到头盔槽；不使用 ArmorItem 所以无护甲模型渲染。
 * 隐身效果逻辑在 GameEvents.onPlayerTick 中处理。
 */
public class KirinHeadbandItem extends Item implements Equipable {

    public KirinHeadbandItem(Properties properties) {
        super(properties);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}
