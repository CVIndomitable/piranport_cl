package com.piranport.client;

import net.minecraft.client.CameraType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 开镜视角恢复的纯逻辑测试：只验证「该不该恢复、恢复成什么」，
 * 真正的视角切换动作依赖客户端 {@code Minecraft.options}，无法在无客户端环境执行。
 */
class ClientScopeHandlerCameraTest {

    @Test
    void restoresRecordedThirdPersonViewWhenStillInForcedFirstPerson() {
        assertEquals(CameraType.THIRD_PERSON_BACK,
                ClientScopeHandler.cameraTypeToRestore(
                        CameraType.THIRD_PERSON_BACK, CameraType.FIRST_PERSON, false));
        assertEquals(CameraType.THIRD_PERSON_FRONT,
                ClientScopeHandler.cameraTypeToRestore(
                        CameraType.THIRD_PERSON_FRONT, CameraType.FIRST_PERSON, false));
    }

    @Test
    void doesNotRestoreWhenPlayerAlreadyLeftFirstPerson() {
        assertNull(ClientScopeHandler.cameraTypeToRestore(
                CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_BACK, false));
        assertNull(ClientScopeHandler.cameraTypeToRestore(
                CameraType.THIRD_PERSON_BACK, CameraType.THIRD_PERSON_FRONT, false));
    }

    @Test
    void doesNotRestoreWithoutRecordedView() {
        // 进入开镜时本来就在第一人称：没有记录，退出时也不该动视角。
        assertNull(ClientScopeHandler.cameraTypeToRestore(null, CameraType.FIRST_PERSON, false));
        assertNull(ClientScopeHandler.cameraTypeToRestore(null, CameraType.THIRD_PERSON_BACK, false));
    }

    @Test
    void respectsPlayerToggleEvenWhenTheyCycleBackToFirstPerson() {
        // 开镜期间玩家自己按过 F5：即使当前又回到第一人称，也不覆盖玩家的选择。
        assertNull(ClientScopeHandler.cameraTypeToRestore(
                CameraType.THIRD_PERSON_BACK, CameraType.FIRST_PERSON, true));
    }
}
