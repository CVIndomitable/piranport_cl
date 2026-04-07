package com.piranport.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;

/**
 * 足球巨星套装 — 护甲物品。
 * 穿戴任意一件即可获得"经验提升 I"Buff，多件不叠加等级。
 */
public class FootballArmorItem extends ArmorItem {

    public FootballArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }
}
