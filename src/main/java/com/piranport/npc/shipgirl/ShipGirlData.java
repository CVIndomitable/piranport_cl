package com.piranport.npc.shipgirl;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 舰娘数据记录：等级/好感/技能/装备槽，用于数据驱动。
 * 框架级实现，先搭数据结构，具体数值和逻辑后续填充。
 */
public class ShipGirlData {
    private int level;
    private int affection;
    private int maxHp;
    private int currentHp;
    private float attackPower;
    private float defense;
    private int[] equipmentSlots;
    private String[] skills;
    private int remodelLevel;

    public ShipGirlData() {
        this.level = 1;
        this.affection = 0;
        this.maxHp = 60;
        this.currentHp = 60;
        this.attackPower = 10.0f;
        this.defense = 5.0f;
        this.equipmentSlots = new int[3];
        this.skills = new String[3];
        this.remodelLevel = 0;
    }

    public ShipGirlData(int level, int affection, int maxHp, float attackPower, float defense) {
        this.level = level;
        this.affection = affection;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.attackPower = attackPower;
        this.defense = defense;
        this.equipmentSlots = new int[3];
        this.skills = new String[3];
        this.remodelLevel = 0;
    }

    // Getters
    public int getLevel() { return level; }
    public int getAffection() { return affection; }
    public int getMaxHp() { return maxHp; }
    public int getCurrentHp() { return currentHp; }
    public float getAttackPower() { return attackPower; }
    public float getDefense() { return defense; }
    public int[] getEquipmentSlots() { return equipmentSlots; }
    public String[] getSkills() { return skills; }
    public int getRemodelLevel() { return remodelLevel; }

    // Setters
    public void setLevel(int level) { this.level = level; }
    public void setAffection(int affection) { this.affection = affection; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = Math.min(currentHp, maxHp); }
    public void setAttackPower(float attackPower) { this.attackPower = attackPower; }
    public void setDefense(float defense) { this.defense = defense; }
    public void setRemodelLevel(int remodelLevel) { this.remodelLevel = remodelLevel; }

    public void setEquipmentSlot(int index, int id) {
        if (index >= 0 && index < equipmentSlots.length) {
            equipmentSlots[index] = id;
        }
    }

    public void setSkill(int index, String skillId) {
        if (index >= 0 && index < skills.length) {
            skills[index] = skillId;
        }
    }

    /**
     * 按 DungeonInstance 难度信息缩放属性，场内属性归一化。
     * 返回归一化后的属性副本，不修改原始数据。
     */
    public NormalizedAttributes normalizeForDungeon(float difficultyScale) {
        int normalizedMaxHp = Math.round(maxHp * difficultyScale);
        float normalizedAttack = attackPower * difficultyScale;
        float normalizedDefense = defense * difficultyScale;

        // 职能补位：随从 DPS 上限刻意低于玩家
        float followerDpsCap = 0.7f;
        normalizedAttack *= followerDpsCap;

        return new NormalizedAttributes(normalizedMaxHp, normalizedAttack, normalizedDefense, level, remodelLevel);
    }

    public static class NormalizedAttributes {
        private final int maxHp;
        private final float attackPower;
        private final float defense;
        private final int level;
        private final int remodelLevel;

        public NormalizedAttributes(int maxHp, float attackPower, float defense, int level, int remodelLevel) {
            this.maxHp = maxHp;
            this.attackPower = attackPower;
            this.defense = defense;
            this.level = level;
            this.remodelLevel = remodelLevel;
        }

        public int getMaxHp() { return maxHp; }
        public float getAttackPower() { return attackPower; }
        public float getDefense() { return defense; }
        public int getLevel() { return level; }
        public int getRemodelLevel() { return remodelLevel; }
    }

    // NBT 保存
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("level", level);
        tag.putInt("affection", affection);
        tag.putInt("maxHp", maxHp);
        tag.putInt("currentHp", currentHp);
        tag.putFloat("attackPower", attackPower);
        tag.putFloat("defense", defense);
        tag.putInt("remodelLevel", remodelLevel);

        ListTag eqList = new ListTag();
        for (int id : equipmentSlots) {
            eqList.add(IntTag.valueOf(id));
        }
        tag.put("equipmentSlots", eqList);

        ListTag skillList = new ListTag();
        for (String skill : skills) {
            skillList.add(net.minecraft.nbt.StringTag.valueOf(skill != null ? skill : ""));
        }
        tag.put("skills", skillList);

        return tag;
    }

    public static ShipGirlData load(CompoundTag tag) {
        ShipGirlData data = new ShipGirlData();
        data.level = tag.getInt("level");
        data.affection = tag.getInt("affection");
        data.maxHp = tag.getInt("maxHp");
        data.currentHp = tag.getInt("currentHp");
        data.attackPower = tag.getFloat("attackPower");
        data.defense = tag.getFloat("defense");
        data.remodelLevel = tag.getInt("remodelLevel");

        if (tag.contains("equipmentSlots", Tag.TAG_LIST)) {
            ListTag eqList = tag.getList("equipmentSlots", Tag.TAG_INT);
            for (int i = 0; i < Math.min(eqList.size(), data.equipmentSlots.length); i++) {
                data.equipmentSlots[i] = eqList.getInt(i);
            }
        }

        if (tag.contains("skills", Tag.TAG_LIST)) {
            ListTag skillList = tag.getList("skills", Tag.TAG_STRING);
            for (int i = 0; i < Math.min(skillList.size(), data.skills.length); i++) {
                data.skills[i] = skillList.getString(i);
            }
        }

        return data;
    }
}
