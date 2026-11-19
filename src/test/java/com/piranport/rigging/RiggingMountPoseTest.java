package com.piranport.rigging;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.client.RiggingDeckPose;
import com.piranport.client.RiggingSocketTransforms;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RiggingMountPoseTest {
    @Test
    void aircraftReplacesRightSpareGunSockets() {
        assertEquals(List.of(RiggingSocket.SIDE_LEFT_2, RiggingSocket.SIDE_LEFT_1),
                RiggingLoadoutResolver.sideSockets(true));
        assertEquals(4, RiggingLoadoutResolver.sideSockets(false).size());
        assertEquals(RiggingSocket.SIDE_LEFT_2, RiggingLoadoutResolver.sideSockets(false).getFirst());
    }

    @Test
    void leftFrontMountOpposesDeckWithEitherForwardAxis() {
        for (boolean firstPerson : new boolean[]{false, true}) {
            var mainHand = RiggingSocketTransforms.mountPose(RiggingSocket.SIDE_LEFT_2, firstPerson);
            var aircraft = RiggingSocketTransforms.mountPose(RiggingSocket.DECK_1, firstPerson);
            var rightFront = RiggingSocketTransforms.mountPose(RiggingSocket.SIDE_RIGHT_2, firstPerson);
            int forward = firstPerson ? 1 : -1;
            assertTrue(mainHand.x() > 0 && mainHand.z() * forward > 0);
            assertTrue(aircraft.x() < 0);
            assertEquals(-mainHand.x(), rightFront.x());
            assertEquals(mainHand.z(), rightFront.z());
        }
    }

    @Test
    void torpedoBasesFaceLegsWhileTopsFaceOutwardAndTubesFaceForward() {
        for (RiggingSocket socket : List.of(RiggingSocket.WAIST_LEFT, RiggingSocket.WAIST_RIGHT)) {
            float side = socket == RiggingSocket.WAIST_LEFT ? 1 : -1;
            var mount = RiggingSocketTransforms.mountPose(socket, false);
            PoseStack stack = new PoseStack();
            RiggingSocketTransforms.applyMountFrame(stack, mount);
            stack.mulPose(Axis.XP.rotationDegrees(180));
            stack.scale(mount.scale(), mount.scale(), mount.scale());
            stack.mulPose(Axis.YP.rotationDegrees(180));
            stack.translate(-0.5, -0.5, -0.5);
            Vector3f up = stack.last().pose().transformDirection(new Vector3f(0, 1, 0)).normalize();
            Vector3f front = stack.last().pose().transformDirection(new Vector3f(0, 0, 1)).normalize();
            Vector3f base = stack.last().pose().transformPosition(new Vector3f(0.5f, 0, 0.5f));
            assertEquals(side, up.x, 1e-5);
            assertEquals(0, up.y, 1e-5);
            assertEquals(-1, front.z, 1e-5);
            assertEquals(side * 8 / 16, base.x, 1e-5);
            assertEquals(9 / 16.0f, base.y, 1e-5);
        }
    }

    @Test
    void deckPitchMatchesBothViewsAndKeepsHingeFixed() {
        assertEquals(-45, RiggingDeckPose.pitchDegrees(0, 0, -45, 15));
        assertEquals(15, RiggingDeckPose.pitchDegrees(1, 0, -45, 15));
        assertEquals(0, RiggingDeckPose.pitchDegrees(0, 1, -45, 15));
        assertEquals(0, RiggingDeckPose.pitchDegrees(1, 1, -45, 15));
        for (boolean firstPerson : new boolean[]{false, true}) {
            for (float pitch : new float[]{-45, 0, 15}) {
                PoseStack stack = new PoseStack();
                RiggingSocketTransforms.applyDeckPose(stack, pitch, firstPerson);
                Vector3f hinge = new Vector3f(-RiggingDeckPose.HINGE_X / 16, RiggingDeckPose.HINGE_Y / 16, 0);
                Vector3f transformed = stack.last().pose().transformPosition(new Vector3f(hinge));
                assertEquals(0, transformed.distance(hinge), 1e-5);
                Vector3f up = stack.last().pose().transformDirection(new Vector3f(0, 1, 0));
                Vector3f front = stack.last().pose().transformDirection(new Vector3f(0, 0, firstPerson ? 1 : -1));
                assertEquals(Math.sin(Math.toRadians(pitch)), front.y, 1e-5);
                assertEquals(Math.cos(Math.toRadians(pitch)), up.y, 1e-5);
                assertEquals(0, up.x, 1e-5, "不再向舷外竖翻");
            }
        }
    }

    @Test
    void forwardAircraftSocketStaysAheadOfRearSocketInBothViews() {
        for (boolean firstPerson : new boolean[]{false, true}) {
            float direction = firstPerson ? 1 : -1;
            float front = direction * RiggingDeckPose.aircraftZ(RiggingSocket.DECK_1, firstPerson);
            float rear = direction * RiggingDeckPose.aircraftZ(RiggingSocket.DECK_2, firstPerson);
            assertTrue(rear < 0 && front > 0 && front < RiggingDeckPose.EDGE_Z);
        }
    }

    @Test
    void breathingIsContinuousAndPeriodic() {
        assertEquals(0, RiggingSocketTransforms.breathingOffset(0), 1e-6);
        assertEquals(0.35f / 16, RiggingSocketTransforms.breathingOffset(20), 1e-6);
        assertEquals(-0.22f / 16, RiggingSocketTransforms.breathingOffset(60), 1e-6);
        for (double tick : new double[]{-1, 0, 19.5, 20, 40, 60, 79.9}) {
            assertEquals(RiggingSocketTransforms.breathingOffset(tick),
                    RiggingSocketTransforms.breathingOffset(tick + 80), 1e-6);
        }
    }
}
