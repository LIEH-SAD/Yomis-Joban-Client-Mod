package com.jsblock.scripting.api;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * A transformation stack exposed to PIDS scripts, mirroring JCM 2.3's {@code Matrices}.
 * A draw call can carry one of these through {@code matrices(Matrices)}, in which case it is
 * applied on top of the PIDS canvas before the draw call renders.
 * @author LX86 (JCM)
 */
public class Matrices {
    private final PoseStack poseStack = new PoseStack();
    private int depth = 0;

    public void translate(double x, double y, double z) {
        poseStack.translate(x, y, z);
    }

    public void scale(double x, double y, double z) {
        poseStack.scale((float) x, (float) y, (float) z);
    }

    public void rotateX(double radian) {
        poseStack.mulPose(Axis.XP.rotation((float) radian));
    }

    public void rotateY(double radian) {
        poseStack.mulPose(Axis.YP.rotation((float) radian));
    }

    public void rotateZ(double radian) {
        poseStack.mulPose(Axis.ZP.rotation((float) radian));
    }

    public void rotateXDegrees(double degree) {
        poseStack.mulPose(Axis.XP.rotationDegrees((float) degree));
    }

    public void rotateYDegrees(double degree) {
        poseStack.mulPose(Axis.YP.rotationDegrees((float) degree));
    }

    public void rotateZDegrees(double degree) {
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) degree));
    }

    public void pushPose() {
        poseStack.pushPose();
        depth++;
    }

    public void popPose() {
        if (depth > 0) {
            poseStack.popPose();
            depth--;
        }
    }

    public void popPushPose() {
        if (depth > 0) {
            poseStack.popPose();
            poseStack.pushPose();
        }
    }

    /** @return the combined transformation, so a script can hand it to a draw call expecting a matrix */
    public org.joml.Matrix4f last() {
        return poseStack.last().pose();
    }

    /** Applies this transformation on top of an existing render matrix stack. */
    public void apply(PoseStack matrices) {
        matrices.last().pose().mul(poseStack.last().pose());
    }

    @Override
    public String toString() {
        return "Matrices[depth=" + depth + "]";
    }
}
