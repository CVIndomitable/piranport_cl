package com.piranport.client.model.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bedrock 1.8.0 骨骼动画的运行时采样器。
 *
 * <p>只实现离线验证过的语义：动画旋转/位移/缩放是<b>叠加</b>在骨骼绑定姿态上的
 * （Blockbench 的 displayRotation 就是 {@code bone.rotation += ...}），
 * 欧拉序为 ZYX；关键帧支持 pre/post、linear 和 catmullrom。</p>
 */
public final class BedrockAnimation {
    public final String name;
    public final float length;
    public final boolean loop;
    private final Map<String, BoneChannel> bones;

    private BedrockAnimation(String name, float length, boolean loop, Map<String, BoneChannel> bones) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.bones = bones;
    }

    public static BedrockAnimation parse(String name, JsonObject json) {
        boolean loop = json.has("loop") && json.get("loop").isJsonPrimitive()
                && json.get("loop").getAsJsonPrimitive().isBoolean()
                && json.get("loop").getAsBoolean();

        Map<String, BoneChannel> bones = new HashMap<>();
        float maxTime = 0.0F;
        JsonObject bonesJson = json.getAsJsonObject("bones");
        if (bonesJson != null) {
            for (Map.Entry<String, JsonElement> entry : bonesJson.entrySet()) {
                JsonObject boneJson = entry.getValue().getAsJsonObject();
                BoneChannel channel = new BoneChannel();
                if (boneJson.has("rotation")) channel.rotation = Channel.parse(boneJson.get("rotation"));
                if (boneJson.has("position")) channel.position = Channel.parse(boneJson.get("position"));
                if (boneJson.has("scale")) channel.scale = Channel.parse(boneJson.get("scale"));
                bones.put(entry.getKey(), channel);
                maxTime = Math.max(maxTime, channel.maxTime());
            }
        }

        float length = json.has("animation_length")
                ? json.get("animation_length").getAsFloat()
                : maxTime;
        if (length <= 0.0F) length = 1.0F;
        return new BedrockAnimation(name, length, loop, bones);
    }

    /**
     * 把动画在 {@code time} 秒处的姿态写入 9 分量骨骼数组。
     * 数组布局：rotation(度) xyz、position(格) xyz、scale xyz。
     */
    public void sample(float time, BedrockMolang.Context ctx, float[] out, Map<String, Integer> boneIndex) {
        for (Map.Entry<String, BoneChannel> entry : bones.entrySet()) {
            Integer index = boneIndex.get(entry.getKey());
            if (index == null) continue;
            entry.getValue().sample(time, ctx, out, index * 9);
        }
    }

    private static final class BoneChannel {
        Channel rotation;
        Channel position;
        Channel scale;

        void sample(float time, BedrockMolang.Context ctx, float[] out, int base) {
            if (rotation != null) rotation.sample(time, ctx, out, base);
            if (position != null) position.sample(time, ctx, out, base + 3);
            if (scale != null) scale.sample(time, ctx, out, base + 6);
        }

        float maxTime() {
            float max = 0.0F;
            if (rotation != null) max = Math.max(max, rotation.maxTime());
            if (position != null) max = Math.max(max, position.maxTime());
            if (scale != null) max = Math.max(max, scale.maxTime());
            return max;
        }
    }

    private interface Vec3Value {
        void eval(BedrockMolang.Context ctx, float[] out, int offset);
    }

    private static final class Channel {
        private static final Vec3Value ZERO = (ctx, out, offset) -> {
            out[offset] = 0.0F;
            out[offset + 1] = 0.0F;
            out[offset + 2] = 0.0F;
        };

        private Vec3Value constant;
        private float[] times;
        private Vec3Value[] pre;
        private Vec3Value[] post;
        private boolean[] catmull;

        static Channel parse(JsonElement element) {
            Channel channel = new Channel();
            if (element.isJsonObject() && !isKeyframeValue(element)) {
                JsonObject keyframes = element.getAsJsonObject();
                List<Map.Entry<Float, JsonElement>> entries = new ArrayList<>();
                for (Map.Entry<String, JsonElement> entry : keyframes.entrySet()) {
                    try {
                        entries.add(Map.entry(Float.parseFloat(entry.getKey()), entry.getValue()));
                    } catch (NumberFormatException ignored) {
                        // 非法时间键直接跳过
                    }
                }
                entries.sort(Comparator.comparingDouble(Map.Entry::getKey));
                int n = entries.size();
                channel.times = new float[n];
                channel.pre = new Vec3Value[n];
                channel.post = new Vec3Value[n];
                channel.catmull = new boolean[n];
                for (int i = 0; i < n; i++) {
                    float time = entries.get(i).getKey();
                    JsonElement value = entries.get(i).getValue();
                    channel.times[i] = time;
                    if (isKeyframeValue(value)) {
                        JsonObject keyframe = value.getAsJsonObject();
                        JsonElement preJson = keyframe.has("pre") ? keyframe.get("pre")
                                : keyframe.get("post");
                        JsonElement postJson = keyframe.has("post") ? keyframe.get("post")
                                : keyframe.get("pre");
                        channel.pre[i] = parseValue(preJson);
                        channel.post[i] = parseValue(postJson);
                        channel.catmull[i] = keyframe.has("lerp_mode")
                                && "catmullrom".equals(keyframe.get("lerp_mode").getAsString());
                    } else {
                        Vec3Value value3 = parseValue(value);
                        channel.pre[i] = value3;
                        channel.post[i] = value3;
                    }
                }
            } else {
                channel.constant = parseValue(element);
            }
            return channel;
        }

        private static boolean isKeyframeValue(JsonElement element) {
            if (element == null || !element.isJsonObject()) return false;
            JsonObject object = element.getAsJsonObject();
            return object.has("pre") || object.has("post");
        }

        void sample(float time, BedrockMolang.Context ctx, float[] out, int offset) {
            if (constant != null) {
                constant.eval(ctx, out, offset);
                return;
            }
            int n = times.length;
            if (n == 0) return;
            if (time <= times[0]) {
                post[0].eval(ctx, out, offset);
                return;
            }
            if (time >= times[n - 1]) {
                pre[n - 1].eval(ctx, out, offset);
                return;
            }
            int i = 0;
            while (i + 1 < n && times[i + 1] <= time) i++;
            float span = times[i + 1] - times[i];
            float u = span <= 1.0E-6F ? 0.0F : (time - times[i]) / span;

            float[] a = new float[3];
            float[] b = new float[3];
            post[i].eval(ctx, a, 0);
            pre[i + 1].eval(ctx, b, 0);
            if (catmull[i] && i > 0 && i + 2 < n) {
                float[] p0 = new float[3];
                float[] p3 = new float[3];
                post[i - 1].eval(ctx, p0, 0);
                pre[i + 2].eval(ctx, p3, 0);
                float u2 = u * u;
                float u3 = u2 * u;
                for (int c = 0; c < 3; c++) {
                    out[offset + c] = 0.5F * (2.0F * a[c]
                            + (-p0[c] + b[c]) * u
                            + (2.0F * p0[c] - 5.0F * a[c] + 4.0F * b[c] - p3[c]) * u2
                            + (-p0[c] + 3.0F * a[c] - 3.0F * b[c] + p3[c]) * u3);
                }
            } else {
                for (int c = 0; c < 3; c++) {
                    out[offset + c] = a[c] + (b[c] - a[c]) * u;
                }
            }
        }

        float maxTime() {
            if (times == null || times.length == 0) return 0.0F;
            return times[times.length - 1];
        }

        private static Vec3Value parseValue(JsonElement element) {
            if (element == null || element.isJsonNull()) return ZERO;
            if (element.isJsonPrimitive()) {
                if (element.getAsJsonPrimitive().isNumber()) {
                    float v = element.getAsFloat();
                    return constant3(v, v, v);
                }
                BedrockMolang.Node node = BedrockMolang.compile(element.getAsString());
                return (ctx, out, offset) -> {
                    float v = node.eval(ctx);
                    out[offset] = v;
                    out[offset + 1] = v;
                    out[offset + 2] = v;
                };
            }
            if (element.isJsonArray()) {
                JsonArray array = element.getAsJsonArray();
                if (array.size() >= 4) {
                    // 四元数关键帧：按 ZYX 欧拉角换算（YSM 主模型目前没有用到）
                    float x = array.get(0).getAsFloat();
                    float y = array.get(1).getAsFloat();
                    float z = array.get(2).getAsFloat();
                    float w = array.get(3).getAsFloat();
                    float[] euler = quaternionToEulerZyx(x, y, z, w);
                    return constant3(euler[0], euler[1], euler[2]);
                }
                BedrockMolang.Node[] nodes = new BedrockMolang.Node[3];
                for (int i = 0; i < 3; i++) {
                    if (i < array.size()) {
                        JsonElement component = array.get(i);
                        if (component.getAsJsonPrimitive().isNumber()) {
                            nodes[i] = BedrockMolang.constant(component.getAsFloat());
                        } else {
                            nodes[i] = BedrockMolang.compile(component.getAsString());
                        }
                    } else {
                        nodes[i] = BedrockMolang.constant(0.0F);
                    }
                }
                return (ctx, out, offset) -> {
                    out[offset] = nodes[0].eval(ctx);
                    out[offset + 1] = nodes[1].eval(ctx);
                    out[offset + 2] = nodes[2].eval(ctx);
                };
            }
            return ZERO;
        }

        private static Vec3Value constant3(float x, float y, float z) {
            return (ctx, out, offset) -> {
                out[offset] = x;
                out[offset + 1] = y;
                out[offset + 2] = z;
            };
        }

        private static float[] quaternionToEulerZyx(float x, float y, float z, float w) {
            double sinX = 2.0 * (w * x + y * z);
            double cosX = 1.0 - 2.0 * (x * x + y * y);
            double rotX = Math.atan2(sinX, cosX);
            double sinY = 2.0 * (w * y - z * x);
            sinY = Math.max(-1.0, Math.min(1.0, sinY));
            double rotY = Math.asin(sinY);
            double sinZ = 2.0 * (w * z + x * y);
            double cosZ = 1.0 - 2.0 * (y * y + z * z);
            double rotZ = Math.atan2(sinZ, cosZ);
            return new float[]{(float) Math.toDegrees(rotX), (float) Math.toDegrees(rotY), (float) Math.toDegrees(rotZ)};
        }
    }
}
