package com.piranport.item;

import net.minecraft.world.item.Item;

/**
 * 阶段 3 占位：纯食物 Item 子类。
 *
 * 1.0 的 ModFoodItem 还会在 shift+右键朝上方放置时把食物变成桌上摆件
 * （依赖 PLACEABLE_INFO component + PlaceableFoodBlockEntity）。
 * 1.20.1 走 NBT 路线，这部分等阶段 7 NBT 重写、阶段 4 BlockEntity 上线后再回填。
 */
public class ModFoodItem extends Item {
    public ModFoodItem(Properties props) {
        super(props);
    }
}
