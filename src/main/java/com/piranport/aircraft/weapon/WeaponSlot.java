package com.piranport.aircraft.weapon;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 飞机武器槽 - 存储单个武器槽的状态（对空或对海）
 *
 * 职责：
 * - 存储武器类型（弹药物品ID）
 * - 追踪剩余弹药量
 * - 追踪弹药容量
 * - NBT 序列化/反序列化
 */
public class WeaponSlot {

    /**
     * 武器槽类型枚举
     */
    public enum SlotType {
        AIR,    // 对空武器槽（机枪弹匣、空空导弹）
        SEA     // 对海武器槽（炸弹、鱼雷、火箭、深弹）
    }

    private final SlotType slotType;

    /**
     * 当前装载的弹药类型（物品注册ID），如 "piranport:aerial_bomb"
     * null 或空字符串表示未装载
     */
    @Nullable
    private String ammoType;

    /**
     * 剩余弹药数量
     */
    private int remainingAmmo;

    /**
     * 弹药容量上限（由飞机机型和弹药类型决定）
     */
    private int ammoCapacity;

    /**
     * 是否已经开火（用于某些攻击模式的一次性检查）
     */
    private boolean hasFired;

    public WeaponSlot(SlotType slotType) {
        this.slotType = slotType;
        this.ammoType = null;
        this.remainingAmmo = 0;
        this.ammoCapacity = 0;
        this.hasFired = false;
    }

    /**
     * 装载弹药到武器槽
     * @param ammoType 弹药物品ID（如 "piranport:aerial_bomb"）
     * @param capacity 弹药容量
     */
    public void loadAmmo(String ammoType, int capacity) {
        this.ammoType = ammoType;
        this.ammoCapacity = capacity;
        this.remainingAmmo = capacity;
        this.hasFired = false;
    }

    /**
     * 卸载武器槽中的弹药，返回剩余数量
     */
    public int unloadAmmo() {
        int remaining = this.remainingAmmo;
        this.ammoType = null;
        this.remainingAmmo = 0;
        this.ammoCapacity = 0;
        this.hasFired = false;
        return remaining;
    }

    /**
     * 消耗弹药
     * @param count 消耗数量
     * @return 实际消耗的数量
     */
    public int consumeAmmo(int count) {
        int consumed = Math.min(count, remainingAmmo);
        remainingAmmo -= consumed;
        return consumed;
    }

    /**
     * 补给弹药（不超过容量上限）
     * @param count 补给数量
     * @return 实际补给的数量
     */
    public int replenishAmmo(int count) {
        int toAdd = Math.min(count, ammoCapacity - remainingAmmo);
        remainingAmmo += toAdd;
        if (toAdd > 0) {
            hasFired = false;
        }
        return toAdd;
    }

    /**
     * 是否已装载弹药
     */
    public boolean isLoaded() {
        return ammoType != null && !ammoType.isEmpty();
    }

    /**
     * 是否有剩余弹药
     */
    public boolean hasAmmo() {
        return isLoaded() && remainingAmmo > 0;
    }

    /**
     * 是否为空槽（未装载任何武器）
     */
    public boolean isEmpty() {
        return !isLoaded();
    }

    // Getters

    public SlotType getSlotType() {
        return slotType;
    }

    @Nullable
    public String getAmmoType() {
        return ammoType;
    }

    public int getRemainingAmmo() {
        return remainingAmmo;
    }

    public int getAmmoCapacity() {
        return ammoCapacity;
    }

    public boolean hasFired() {
        return hasFired;
    }

    public void setHasFired(boolean fired) {
        this.hasFired = fired;
    }

    public void setRemainingAmmo(int amount) {
        this.remainingAmmo = Math.max(0, Math.min(amount, ammoCapacity));
    }

    // NBT 序列化

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("SlotType", slotType.name());
        if (ammoType != null) {
            tag.putString("AmmoType", ammoType);
        }
        tag.putInt("RemainingAmmo", remainingAmmo);
        tag.putInt("AmmoCapacity", ammoCapacity);
        tag.putBoolean("HasFired", hasFired);
        return tag;
    }

    public static WeaponSlot load(CompoundTag tag) {
        SlotType type;
        try {
            type = SlotType.valueOf(tag.getString("SlotType"));
        } catch (IllegalArgumentException e) {
            type = SlotType.AIR;
        }

        WeaponSlot slot = new WeaponSlot(type);
        if (tag.contains("AmmoType")) {
            slot.ammoType = tag.getString("AmmoType");
        }
        slot.remainingAmmo = tag.getInt("RemainingAmmo");
        slot.ammoCapacity = tag.getInt("AmmoCapacity");
        slot.hasFired = tag.getBoolean("HasFired");
        return slot;
    }

    /**
     * 创建副本
     */
    public WeaponSlot copy() {
        WeaponSlot copy = new WeaponSlot(this.slotType);
        copy.ammoType = this.ammoType;
        copy.remainingAmmo = this.remainingAmmo;
        copy.ammoCapacity = this.ammoCapacity;
        copy.hasFired = this.hasFired;
        return copy;
    }

    @Override
    public String toString() {
        return "WeaponSlot{" +
                "type=" + slotType +
                ", ammo=" + (ammoType != null ? ammoType : "empty") +
                ", remaining=" + remainingAmmo + "/" + ammoCapacity +
                ", fired=" + hasFired +
                '}';
    }
}
