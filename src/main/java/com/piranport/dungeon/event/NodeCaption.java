package com.piranport.dungeon.event;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.SceneData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.VictoryCondition;

/** 《副本/12》进节点字幕的文案键。服务端只发键，客户端按自己的语言翻译。 */
final class NodeCaption {

    private NodeCaption() {}

    static String objectiveKey(StageData stage, NodeData node) {
        if (node.escort() != null) return "hud.piranport.dungeon.objective.escort";
        if (node.completesByArrival()) return "hud.piranport.dungeon.objective.reach";
        if (stage.victoryConditions().contains(VictoryCondition.CAPTURE_FLAG)) {
            return "hud.piranport.dungeon.objective.flag";
        }
        return "hud.piranport.dungeon.objective.battle";
    }

    /** 白天与晴天不加字幕行，《副本/12》只列夜战、雷雨、大雾、烈日。 */
    static String sceneKey(SceneData scene) {
        return switch (scene) {
            case NIGHT -> "hud.piranport.dungeon.scene.night";
            case STORM -> "hud.piranport.dungeon.scene.storm";
            case FOG -> "hud.piranport.dungeon.scene.fog";
            case HOT_SUN -> "hud.piranport.dungeon.scene.hot_sun";
            case DAY, CLEAR -> "";
        };
    }
}
