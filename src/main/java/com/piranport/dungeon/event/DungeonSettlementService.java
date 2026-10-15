package com.piranport.dungeon.event;

import com.piranport.advancement.ModAdvancements;
import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.DungeonResultPayload;
import com.piranport.dungeon.saved.DungeonSavedData;
import com.piranport.dungeon.saved.DungeonSettlementData;
import com.piranport.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 副本/19：每节点即时发奖，末节点把首通奖励合并进同一张展示页。 */
public final class DungeonSettlementService {
    private DungeonSettlementService() {}

    /** 副本/20：用时相对 par_time_seconds 评价，S ≤0.6×、A ≤0.85×、B ≤1.15×、C 其余。 */
    public static String ratingFor(long elapsedMillis, int parTimeSeconds) {
        double par = (parTimeSeconds > 0 ? parTimeSeconds : 1200) * 1000.0;
        double ratio = Math.max(0L, elapsedMillis) / par;
        if (ratio <= 0.6) return "S";
        if (ratio <= 0.85) return "A";
        if (ratio <= 1.15) return "B";
        return "C";
    }
    /**
     * 宝箱出口：节点奖励不再直接塞进背包，而是由出口结构收集进战利品箱。
     * 该队列在 {@link #settleNode} 清点奖励时逐条入队，随后由
     * {@link com.piranport.dungeon.block.PortalStructureHelper#buildPortalStructure}
     * 生成宝箱时取走。结算界面照常展示 namesOut 里的条目。
     */
    public static final java.util.Map<String,List<ItemStack>> PENDING_CHEST_LOOT = new java.util.concurrent.ConcurrentHashMap<>();
    static String lootKey(UUID instanceId,String nodeId){return instanceId+"/"+nodeId;}
    /** 由 PortalStructureHelper 在摆箱子前调用，取走并清空该节点的待放战利品。 */
    public static List<ItemStack> drainPendingLoot(UUID instanceId,String nodeId){
        List<ItemStack> l=PENDING_CHEST_LOOT.remove(lootKey(instanceId,nodeId));
        return l==null?List.of():l;
    }
    public static boolean isCounting(DungeonInstance i,String n){return i!=null&&n!=null&&i.getState()==DungeonInstance.State.ACTIVE&&i.hasEnteredNode(n)&&!i.getClearedNodes().contains(n);}
    public static void tickActiveNode(ServerLevel l,DungeonInstance i){if(isCounting(i,i.getCurrentNode()))DungeonSettlementData.get(l).tick(i.getInstanceId(),i.getCurrentNode());}
    public static void recordKill(ServerLevel l,DungeonInstance i,String n,UUID e){if(e!=null&&isCounting(i,n))DungeonSettlementData.get(l).recordKill(i.getInstanceId(),n,e);}
    public static boolean settleNode(ServerLevel l,DungeonInstance i,StageData s,NodeData n,boolean completes){
        DungeonSettlementData d=DungeonSettlementData.get(l); if(!i.getClearedNodes().contains(n.nodeId())||!d.claimSettlement(i.getInstanceId(),n.nodeId()))return false;
        DungeonInstanceManager m=DungeonInstanceManager.get(l); DungeonSavedData first=DungeonSavedData.get(l);
        // 宝箱出口：节点奖励按节点roll一次（原为每人一份），放入出口战利品箱；
        // 首通奖励仍按参与判定发个人背包——这是身份奖励，进公共箱子会被误拿。
        // 概率roll需要一个随机源：优先用第一个有效玩家，没人则用世界随机数。
        List<ItemStack> chestLoot=new ArrayList<>();
        List<String> sharedRewardNames=new ArrayList<>();
        net.minecraft.util.RandomSource rng=l.getRandom();
        ServerPlayer roller=null;
        for(UUID id:i.getPlayerUuids()){ServerPlayer p=l.getServer().getPlayerList().getPlayer(id);if(p!=null&&p.isAlive()&&!p.isSpectator()&&m.getInstanceForPlayer(p)==i){roller=p;break;}}
        if(roller!=null){for(NodeData.RewardEntry r:n.rewards())RewardDispatcher.rollToStack(roller,r,sharedRewardNames,chestLoot);}
        else{for(NodeData.RewardEntry r:n.rewards()){if(r.chance()<1.0f&&rng.nextFloat()>r.chance())continue;net.minecraft.world.item.Item item=r.resolvedItem();if(item!=null){chestLoot.add(new ItemStack(item,r.count()));sharedRewardNames.add(item.getDescription().getString()+" x"+r.count());}}}
        for(UUID id:i.getPlayerUuids()){ServerPlayer p=l.getServer().getPlayerList().getPlayer(id);if(p==null||!p.isAlive()||p.isSpectator()||m.getInstanceForPlayer(p)!=i)continue;List<String> rs=new ArrayList<>(sharedRewardNames);if(completes){ModAdvancements.award(p,"dungeon/first_clear");// 首通奖励/标记只给 Boss 击杀瞬间在场（快照）的玩家；eligible 无快照时全员兜底
if(FirstClearRules.eligible(i,id)&&!first.hasFirstCleared(s.stageId(),id)){first.markFirstCleared(s.stageId(),id);for(NodeData.RewardEntry r:s.firstClearRewards())RewardDispatcher.give(p,r,rs);int c=completedChapterNumber(s,DungeonRegistry.INSTANCE.getChapter(s.chapter()));if(c>0)giveDeployMedal(p,c,rs);}}PacketDistributor.sendToPlayer(p,new DungeonResultPayload(s.displayName()+" · "+n.nodeId(),d.elapsedMillis(i.getInstanceId(),n.nodeId()),false,rs,d.kills(i.getInstanceId(),n.nodeId())));}
        if(!chestLoot.isEmpty())PENDING_CHEST_LOOT.put(lootKey(i.getInstanceId(),n.nodeId()),chestLoot);
        return true;
    }
    static int completedChapterNumber(StageData s,ChapterData c){if(c==null||c.stages().isEmpty()||!c.stages().get(c.stages().size()-1).equals(s.stageId())||!s.chapter().matches("chapter_[1-7]"))return -1;return Integer.parseInt(s.chapter().substring(8));}
    private static void giveDeployMedal(ServerPlayer p,int n,List<String> rs){ItemStack m=switch(n){case 1->new ItemStack(ModItems.DEPLOY_MEDAL_CH1.get());case 2->new ItemStack(ModItems.DEPLOY_MEDAL_CH2.get());case 3->new ItemStack(ModItems.DEPLOY_MEDAL_CH3.get());case 4->new ItemStack(ModItems.DEPLOY_MEDAL_CH4.get());case 5->new ItemStack(ModItems.DEPLOY_MEDAL_CH5.get());case 6->new ItemStack(ModItems.DEPLOY_MEDAL_CH6.get());case 7->new ItemStack(ModItems.DEPLOY_MEDAL_CH7.get());default->ItemStack.EMPTY;};if(m.isEmpty())return;rs.add(m.getHoverName().getString()+" x1");if(!p.getInventory().add(m))p.drop(m,false);}
}
