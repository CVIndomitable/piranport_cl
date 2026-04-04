package com.piranport.item;

import net.minecraft.world.item.Item;

/**
 * 鱼雷再装填强化部件 — 安装在舰装核心的强化槽中，
 * 使鱼雷发射器可以自动从弹药槽/背包中装填鱼雷。
 * 不安装此部件时，必须使用装填设施方块预装填鱼雷发射器。
 */
public class TorpedoReloadItem extends Item {
    private final int weight;

    public TorpedoReloadItem(Properties properties, int weight) {
        super(properties);
        this.weight = weight;
    }

    public int getWeight() { return weight; }
}
