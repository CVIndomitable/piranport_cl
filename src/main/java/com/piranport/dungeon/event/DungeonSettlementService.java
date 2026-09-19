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
    public static boolean isCounting(DungeonInstance i,String n){return i!=null&&n!=null&&i.getState()==DungeonInstance.State.ACTIVE&&i.hasEnteredNode(n)&&!i.getClearedNodes().contains(n);}
    public static void tickActiveNode(ServerLevel l,DungeonInstance i){if(isCounting(i,i.getCurrentNode()))DungeonSettlementData.get(l).tick(i.getInstanceId(),i.getCurrentNode());}
    public static void recordKill(ServerLevel l,DungeonInstance i,String n,UUID e){if(e!=null&&isCounting(i,n))DungeonSettlementData.get(l).recordKill(i.getInstanceId(),n,e);}
    public static boolean settleNode(ServerLevel l,DungeonInstance i,StageData s,NodeData n,boolean completes){
        DungeonSettlementData d=DungeonSettlementData.get(l); if(!i.getClearedNodes().contains(n.nodeId())||!d.claimSettlement(i.getInstanceId(),n.nodeId()))return false;
        DungeonInstanceManager m=DungeonInstanceManager.get(l); DungeonSavedData first=DungeonSavedData.get(l);
        for(UUID id:i.getPlayerUuids()){ServerPlayer p=l.getServer().getPlayerList().getPlayer(id);if(p==null||!p.isAlive()||p.isSpectator()||m.getInstanceForPlayer(p)!=i)continue;List<String> rs=new ArrayList<>();for(NodeData.RewardEntry r:n.rewards())RewardDispatcher.give(p,r,rs);if(completes){ModAdvancements.award(p,"dungeon/first_clear");if(!first.hasFirstCleared(s.stageId(),id)){first.markFirstCleared(s.stageId(),id);for(NodeData.RewardEntry r:s.firstClearRewards())RewardDispatcher.give(p,r,rs);int c=completedChapterNumber(s,DungeonRegistry.INSTANCE.getChapter(s.chapter()));if(c>0)giveDeployMedal(p,c,rs);}}PacketDistributor.sendToPlayer(p,new DungeonResultPayload(s.displayName()+" · "+n.nodeId(),d.elapsedMillis(i.getInstanceId(),n.nodeId()),false,rs,d.kills(i.getInstanceId(),n.nodeId())));}
        return true;
    }
    static int completedChapterNumber(StageData s,ChapterData c){if(c==null||c.stages().isEmpty()||!c.stages().get(c.stages().size()-1).equals(s.stageId())||!s.chapter().matches("chapter_[1-7]"))return -1;return Integer.parseInt(s.chapter().substring(8));}
    private static void giveDeployMedal(ServerPlayer p,int n,List<String> rs){ItemStack m=switch(n){case 1->new ItemStack(ModItems.DEPLOY_MEDAL_CH1.get());case 2->new ItemStack(ModItems.DEPLOY_MEDAL_CH2.get());case 3->new ItemStack(ModItems.DEPLOY_MEDAL_CH3.get());case 4->new ItemStack(ModItems.DEPLOY_MEDAL_CH4.get());case 5->new ItemStack(ModItems.DEPLOY_MEDAL_CH5.get());case 6->new ItemStack(ModItems.DEPLOY_MEDAL_CH6.get());case 7->new ItemStack(ModItems.DEPLOY_MEDAL_CH7.get());default->ItemStack.EMPTY;};if(m.isEmpty())return;rs.add(m.getHoverName().getString()+" x1");if(!p.getInventory().add(m))p.drop(m,false);}
}
