package me.gravityio.itemio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class RenderHelper {

    public static Quaternionf getBillboard(Camera camera, Vec2f rotator, Billboard type) {
        return switch(type) {
            case FIXED -> new Quaternionf();
            case HORIZONTAL -> new Quaternionf().rotationYXZ((float) (-Math.PI / 180.0) * rotator.y, (float) (-Math.PI / 180.0) * camera.getPitch(), 0.0F);
            case VERTICAL -> new Quaternionf()
                    .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * camera.getYaw(), (float) (Math.PI / 180.0) * rotator.x, 0.0F);
            case CENTER -> new Quaternionf()
                    .rotationYXZ((float) Math.PI - (float) (Math.PI / 180.0) * camera.getYaw(), (float) (-Math.PI / 180.0) * camera.getPitch(), 0.0F);
        };
    }

    public static void renderText(MatrixStack matrices, TextRenderer textRenderer, VertexConsumerProvider.Immediate vc, Text text, float height, int argb) {
        matrices.push();
        matrices.translate(0.0F, height, 0.0F);
        matrices.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        float offset = (float)(-textRenderer.getWidth(text) / 2);
        textRenderer.draw(text, offset, 0, argb, false, matrix4f, vc, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        matrices.pop();
    }

    public static void renderItem(MinecraftClient client, VertexConsumerProvider.Immediate vc, MatrixStack matrices, World world, ItemStack stack, float x, float y, float z) {
        if (!stack.isEmpty() ) {
            BakedModel bakedModel = client.getItemRenderer().getModel(stack, world, null, 0);
            matrices.push();
            matrices.translate(x, y, z);
            client.getItemRenderer()
                    .renderItem(stack, ModelTransformationMode.GUI, false, matrices, vc, 15728880, OverlayTexture.DEFAULT_UV, bakedModel);
            matrices.pop();
        }
    }

    public static void renderCube(VertexConsumer v, Matrix4f matrix, float width, float height, float depth, int r, int g, int b, int a, int light) {
        v.vertex(matrix, -width, -height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, -height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, height, depth).color(r, g, b, a).light(light).next();

        v.vertex(matrix, width, -height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, -height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, -depth).color(r, g, b, a).light(light).next();

        v.vertex(matrix, -width, -height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, -height, -depth).color(r, g, b, a).light(light).next();

        v.vertex(matrix, width, -height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, -height, depth).color(r, g, b, a).light(light).next();

        v.vertex(matrix, -width, height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, height, -depth).color(r, g, b, a).light(light).next();

        v.vertex(matrix, width, -height, -depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, width, -height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, -height, depth).color(r, g, b, a).light(light).next();
        v.vertex(matrix, -width, -height, -depth).color(r, g, b, a).light(light).next();
    }

    public enum Billboard {
        FIXED,
        HORIZONTAL,
        VERTICAL,
        CENTER
    }

}
