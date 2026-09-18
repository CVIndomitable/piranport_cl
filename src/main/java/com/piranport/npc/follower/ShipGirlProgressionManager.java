package com.piranport.npc.follower;

import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 随从舰娘局外养成数据（NPC/01 决策 A 方案）。
 *
 * <p>每位玩家拥有独立的舰娘列表，每艘舰娘包含等级、经验、好感度、技能点等。
 * 数据持久化到玩家 NBT（随玩家数据自动保存）。</p>
 */
public class ShipGirlProgressionManager {

    /** 每名玩家携带位上限（与 DungeonFollowerManager 保持一致） */
    public static final int MAX_FOLLOWERS = 2;
    /** 等级上限 */
    public static final int MAX_LEVEL = 50;
    /** 每级所需经验基数 */
    private static final int BASE_XP_PER_LEVEL = 100;
    /** 经验缩放因子 */
    private static final float XP_SCALE = 1.15f;

    private ShipGirlProgressionManager() {}

    // ===== 舰娘个体数据 =====

    /**
     * 单艘随从舰娘的养成数据。
     */
    public static class ShipGirlData {
        public UUID entityUuid;       // 舰娘实体 UUID（主世界实体）
        public String shipType;       // 舰娘类型（如 "destroyer", "light_cruiser"）
        public String displayName;    // 自定义名称
        public int level;             // 当前等级 (1~MAX_LEVEL)
        public int experience;        // 当前经验值
        public int rapport;           // 好感度 (0~100)
        public int skillPoints;       // 可用技能点
        public Set<Integer> unlockedSkills = new HashSet<>(); // 已解锁技能 ID
        public int[] equipmentSlots = new int[3]; // 装备槽（与 ShipGirlEntity 同步）
        public long lastPlayTime;     // 上次互动时间（好感衰减计算用）

        public ShipGirlData() {
            this.level = 1;
            this.experience = 0;
            this.rapport = 0;
            this.skillPoints = 0;
            this.lastPlayTime = System.currentTimeMillis();
        }

        /** 当前等级升级所需经验 */
        public int xpToNextLevel() {
            return (int) (BASE_XP_PER_LEVEL * Math.pow(XP_SCALE, level - 1));
        }

        /** 添加经验，返回是否升级 */
        public boolean addExperience(int xp) {
            this.experience += xp;
            boolean leveledUp = false;
            while (this.experience >= xpToNextLevel() && this.level < MAX_LEVEL) {
                this.experience -= xpToNextLevel();
                this.level++;
                this.skillPoints++;
                leveledUp = true;
            }
            if (this.level >= MAX_LEVEL) {
                this.experience = 0;
            }
            return leveledUp;
        }

        /** 增加好感度 */
        public void addRapport(int amount) {
            this.rapport = Math.min(100, Math.max(0, this.rapport + amount));
        }

        /** 序列化到 NBT */
        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("EntityUuid", entityUuid);
            tag.putString("ShipType", shipType != null ? shipType : "");
            tag.putString("DisplayName", displayName != null ? displayName : "");
            tag.putInt("Level", level);
            tag.putInt("Experience", experience);
            tag.putInt("Rapport", rapport);
            tag.putInt("SkillPoints", skillPoints);

            ListTag skillList = new ListTag();
            for (Integer skillId : unlockedSkills) {
                skillList.add(IntTag.valueOf(skillId));
            }
            tag.put("UnlockedSkills", skillList);

            ListTag equipList = new ListTag();
            for (int slot : equipmentSlots) {
                equipList.add(IntTag.valueOf(slot));
            }
            tag.put("EquipmentSlots", equipList);

            tag.putLong("LastPlayTime", lastPlayTime);
            return tag;
        }

        /** 从 NBT 反序列化 */
        public static ShipGirlData fromNbt(CompoundTag tag) {
            ShipGirlData data = new ShipGirlData();
            if (tag.hasUUID("EntityUuid")) {
                data.entityUuid = tag.getUUID("EntityUuid");
            }
            data.shipType = tag.getString("ShipType");
            data.displayName = tag.getString("DisplayName");
            data.level = tag.getInt("Level");
            data.experience = tag.getInt("Experience");
            data.rapport = tag.getInt("Rapport");
            data.skillPoints = tag.getInt("SkillPoints");

            if (tag.contains("UnlockedSkills", Tag.TAG_LIST)) {
                ListTag skillList = tag.getList("UnlockedSkills", Tag.TAG_INT);
                for (int i = 0; i < skillList.size(); i++) {
                    data.unlockedSkills.add(skillList.getInt(i));
                }
            }

            if (tag.contains("EquipmentSlots", Tag.TAG_LIST)) {
                ListTag equipList = tag.getList("EquipmentSlots", Tag.TAG_INT);
                int len = Math.min(equipList.size(), data.equipmentSlots.length);
                for (int i = 0; i < len; i++) {
                    data.equipmentSlots[i] = equipList.getInt(i);
                }
            }

            data.lastPlayTime = tag.getLong("LastPlayTime");
            return data;
        }
    }

    // ===== 玩家数据管理 =====

    /** playerUuid -> 舰娘列表 */
    private static final Map<UUID, List<ShipGirlData>> playerShipGirls = new HashMap<>();

    /**
     * 获取玩家拥有的所有舰娘数据。
     */
    public static List<ShipGirlData> getPlayerShipGirls(UUID playerUuid) {
        return playerShipGirls.computeIfAbsent(playerUuid, k -> new ArrayList<>());
    }

    /**
     * 获取玩家指定的舰娘数据。
     */
    public static Optional<ShipGirlData> getShipGirl(UUID playerUuid, UUID entityUuid) {
        return getPlayerShipGirls(playerUuid).stream()
                .filter(d -> d.entityUuid.equals(entityUuid))
                .findFirst();
    }

    /**
     * 添加新舰娘到玩家列表（招募成功时调用）。
     */
    public static ShipGirlData recruitShipGirl(Player player, String shipType, String displayName) {
        ShipGirlData data = new ShipGirlData();
        data.shipType = shipType;
        data.displayName = displayName != null ? displayName : shipType;
        data.entityUuid = UUID.randomUUID();
        getPlayerShipGirls(player.getUUID()).add(data);
        return data;
    }

    /**
     * 舰娘获得经验（副本战斗结束后调用）。
     */
    public static void gainExperience(UUID playerUuid, UUID entityUuid, int xp) {
        getShipGirl(playerUuid, entityUuid).ifPresent(data -> data.addExperience(xp));
    }

    /**
     * 增加舰娘好感度。
     */
    public static void addRapport(UUID playerUuid, UUID entityUuid, int amount) {
        getShipGirl(playerUuid, entityUuid).ifPresent(data -> data.addRapport(amount));
    }

    /**
     * 解锁舰娘技能。
     */
    public static boolean unlockSkill(UUID playerUuid, UUID entityUuid, int skillId) {
        Optional<ShipGirlData> opt = getShipGirl(playerUuid, entityUuid);
        if (opt.isEmpty()) return false;
        ShipGirlData data = opt.get();
        if (data.skillPoints <= 0) return false;
        if (data.unlockedSkills.contains(skillId)) return false;
        data.skillPoints--;
        data.unlockedSkills.add(skillId);
        return true;
    }

    // ===== 持久化 =====

    /**
     * 从玩家 NBT 加载舰娘数据。
     */
    public static void loadFromNbt(CompoundTag tag, UUID playerUuid) {
        if (!tag.contains("ShipGirls", Tag.TAG_LIST)) return;
        ListTag list = tag.getList("ShipGirls", Tag.TAG_COMPOUND);
        List<ShipGirlData> shipGirls = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            shipGirls.add(ShipGirlData.fromNbt(list.getCompound(i)));
        }
        playerShipGirls.put(playerUuid, shipGirls);
    }

    /**
     * 将舰娘数据保存到玩家 NBT。
     */
    public static void saveToNbt(CompoundTag tag, UUID playerUuid) {
        List<ShipGirlData> shipGirls = getPlayerShipGirls(playerUuid);
        ListTag list = new ListTag();
        for (ShipGirlData data : shipGirls) {
            list.add(data.toNbt());
        }
        tag.put("ShipGirls", list);
    }
}
