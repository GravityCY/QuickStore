package me.gravityio.itemio.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class RenderHelper {

    public static Quaternionf getBillboard(Camera camera, Vec2 rotator, Billboard type) {
        return switch (type) {
            case FIXED -> new Quaternionf();
            case HORIZONTAL ->
                    new Quaternionf().rotationYXZ((float) (-Math.PI / 180.0) * rotator.y, (float) (-Math.PI / 180.0) * camera.getXRot(), 0.0F);
            case VERTICAL -> new Quaternionf()
                    .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * camera.getYRot(), (float) (Math.PI / 180.0) * rotator.x, 0.0F);
            case CENTER -> new Quaternionf()
                    .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * camera.getYRot(), (float) (-Math.PI / 180.0) * camera.getXRot(), 0.0F);
        };
    }

    public static void renderText(PoseStack matrices, Font textRenderer, MultiBufferSource.BufferSource vc, Component text, float height, int argb) {
        matrices.pushPose();
        matrices.translate(0.0F, height, 0.0F);
        matrices.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrices.last().pose();
        float offset = (float) (-textRenderer.width(text) / 2);
        textRenderer.drawInBatch(text, offset, 0, argb, false, matrix4f, vc, Font.DisplayMode.NORMAL, 0, 0xF000F0);
        matrices.popPose();
    }

    public static void renderItem(Minecraft client, MultiBufferSource.BufferSource vc, PoseStack matrices, Level world, ItemStack stack, float x, float y, float z) {
        if (!stack.isEmpty()) {
            BakedModel bakedModel = client.getItemRenderer().getModel(stack, world, null, 0);
            matrices.pushPose();
            matrices.translate(x, y, z);
            client.getItemRenderer()
                    .render(stack, ItemDisplayContext.GUI, false, matrices, vc, 15728880, OverlayTexture.NO_OVERLAY, bakedModel);
            matrices.popPose();
        }
    }

    public static void renderCube(VertexConsumer v, Matrix4f matrix, float width, float height, float depth, int r, int g, int b, int a, int light) {
        v.addVertex(matrix, -width, -height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, -height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, height, depth).setColor(r, g, b, a).setLight(light);

        v.addVertex(matrix, width, -height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, -height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, -depth).setColor(r, g, b, a).setLight(light);

        v.addVertex(matrix, -width, -height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, -height, -depth).setColor(r, g, b, a).setLight(light);

        v.addVertex(matrix, width, -height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, -height, depth).setColor(r, g, b, a).setLight(light);

        v.addVertex(matrix, -width, height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, height, -depth).setColor(r, g, b, a).setLight(light);

        v.addVertex(matrix, width, -height, -depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, width, -height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, -height, depth).setColor(r, g, b, a).setLight(light);
        v.addVertex(matrix, -width, -height, -depth).setColor(r, g, b, a).setLight(light);
    }

    public enum Billboard {
        FIXED,
        HORIZONTAL,
        VERTICAL,
        CENTER
    }

}
