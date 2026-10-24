package com.piranport.client.model.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bedrock 1.12.0 geometry 的运行时数据结构。
 *
 * <p>YSM 模型本身就是 Bedrock 几何：骨骼树 + 方块 + 逐面 UV。
 * 这里按原样保存 Bedrock 坐标（Y 向上、面朝 -Z、1 单位 = 1/16 格），
 * 实际转换到 Java 模型空间在 {@link BedrockMesh#render} 里做。</p>
 */
public final class BedrockGeometry {
    public final float textureWidth;
    public final float textureHeight;
    public final List<Bone> bones;
    public final List<Integer> roots;
    public final Map<String, Integer> indexByName;

    private BedrockGeometry(float textureWidth, float textureHeight, List<Bone> bones,
                            List<Integer> roots, Map<String, Integer> indexByName) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.bones = bones;
        this.roots = roots;
        this.indexByName = indexByName;
    }

    public static BedrockGeometry empty() {
        return new BedrockGeometry(64.0F, 64.0F, List.of(), List.of(), Map.of());
    }

    /** 一个 Bedrock 面在纹理上的矩形。u1/v1/u2/v2 的语义与 ModelPart.Polygon 一致。 */
    public record FaceUv(float u1, float v1, float u2, float v2) {
        public float width() {
            return Math.abs(u2 - u1);
        }

        public float height() {
            return Math.abs(v2 - v1);
        }

        public boolean isVisible() {
            return width() > 1.0E-4F && height() > 1.0E-4F;
        }
    }

    public static final class Cube {
        public final float[] origin;
        public final float[] size;
        public final float[] pivot;
        public final float[] rotation;
        public final Map<Direction, FaceUv> faces = new EnumMap<>(Direction.class);

        Cube(float[] origin, float[] size, float[] pivot, float[] rotation) {
            this.origin = origin;
            this.size = size;
            this.pivot = pivot;
            this.rotation = rotation;
        }

        public boolean isRotated() {
            return rotation[0] != 0.0F || rotation[1] != 0.0F || rotation[2] != 0.0F;
        }
    }

    public static final class Bone {
        public final String name;
        public final int index;
        public final float[] pivot;
        public final float[] rotation;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Integer> children = new ArrayList<>();
        public int parent = -1;

        Bone(String name, int index, float[] pivot, float[] rotation) {
            this.name = name;
            this.index = index;
            this.pivot = pivot;
            this.rotation = rotation;
        }
    }

    // ===== 解析 =====

    public static BedrockGeometry parse(JsonObject root) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries == null || geometries.size() == 0) {
            throw new IllegalArgumentException("缺少 minecraft:geometry");
        }
        JsonObject geometry = geometries.get(0).getAsJsonObject();
        JsonObject description = geometry.getAsJsonObject("description");
        float textureWidth = description != null && description.has("texture_width")
                ? description.get("texture_width").getAsFloat() : 64.0F;
        float textureHeight = description != null && description.has("texture_height")
                ? description.get("texture_height").getAsFloat() : 64.0F;

        JsonArray boneArray = geometry.getAsJsonArray("bones");
        if (boneArray == null) {
            throw new IllegalArgumentException("缺少 bones");
        }

        List<Bone> bones = new ArrayList<>(boneArray.size());
        Map<String, Integer> indexByName = new HashMap<>();
        for (int i = 0; i < boneArray.size(); i++) {
            JsonObject boneJson = boneArray.get(i).getAsJsonObject();
            String name = boneJson.get("name").getAsString();
            float[] pivot = readVec3(boneJson, "pivot", new float[]{0.0F, 0.0F, 0.0F});
            float[] rotation = readVec3(boneJson, "rotation", new float[]{0.0F, 0.0F, 0.0F});
            bones.add(new Bone(name, i, pivot, rotation));
            indexByName.putIfAbsent(name, i);
        }

        List<Integer> roots = new ArrayList<>();
        for (int i = 0; i < boneArray.size(); i++) {
            JsonObject boneJson = boneArray.get(i).getAsJsonObject();
            Bone bone = bones.get(i);
            if (boneJson.has("parent")) {
                Integer parent = indexByName.get(boneJson.get("parent").getAsString());
                if (parent != null) {
                    bone.parent = parent;
                    bones.get(parent).children.add(i);
                } else {
                    roots.add(i);
                }
            } else {
                roots.add(i);
            }

            JsonArray cubes = boneJson.getAsJsonArray("cubes");
            if (cubes != null) {
                for (JsonElement cubeElement : cubes) {
                    bone.cubes.add(parseCube(cubeElement.getAsJsonObject(), textureWidth, textureHeight));
                }
            }
        }
        return new BedrockGeometry(textureWidth, textureHeight, bones, roots, indexByName);
    }

    private static Cube parseCube(JsonObject json, float textureWidth, float textureHeight) {
        float[] origin = readVec3(json, "origin", new float[]{0.0F, 0.0F, 0.0F});
        float[] size = readVec3(json, "size", new float[]{0.0F, 0.0F, 0.0F});

        float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0F;
        if (inflate != 0.0F) {
            origin = new float[]{origin[0] - inflate, origin[1] - inflate, origin[2] - inflate};
            size = new float[]{size[0] + inflate * 2.0F, size[1] + inflate * 2.0F, size[2] + inflate * 2.0F};
        }

        // Bedrock 的 cube pivot 缺省为立方体中心
        float[] pivot = json.has("pivot")
                ? readVec3(json, "pivot", new float[]{0.0F, 0.0F, 0.0F})
                : new float[]{origin[0] + size[0] * 0.5F, origin[1] + size[1] * 0.5F, origin[2] + size[2] * 0.5F};
        float[] rotation = readVec3(json, "rotation", new float[]{0.0F, 0.0F, 0.0F});
        boolean mirror = json.has("mirror") && json.get("mirror").getAsBoolean();

        Cube cube = new Cube(origin, size, pivot, rotation);
        JsonElement uv = json.get("uv");
        if (uv == null) return cube;

        if (uv.isJsonArray()) {
            // box UV：按原版箱式展开布局换算成逐面矩形
            float u = uv.getAsJsonArray().get(0).getAsFloat();
            float v = uv.getAsJsonArray().get(1).getAsFloat();
            float dx = size[0];
            float dy = size[1];
            float dz = size[2];
            putFace(cube, Direction.NORTH, u + dz, v + dz, u + dz + dx, v + dz + dy, mirror);
            putFace(cube, Direction.SOUTH, u + 2.0F * dz + dx, v + dz, u + 2.0F * dz + 2.0F * dx, v + dz + dy, mirror);
            // 箱式展开里 east 在 u+dz+dx、west 在 u；Bedrock 的 up/down 对应 Java 模型空间的 down/up
            putFace(cube, Direction.EAST, u + dz + dx, v + dz, u + 2.0F * dz + dx, v + dz + dy, mirror);
            putFace(cube, Direction.WEST, u, v + dz, u + dz, v + dz + dy, mirror);
            putFace(cube, Direction.UP, u + dz, v, u + dz + dx, v + dz, mirror);
            putFace(cube, Direction.DOWN, u + dz + dx, v + dz, u + dz + 2.0F * dx, v, mirror);
            return cube;
        }

        JsonObject faceJson = uv.getAsJsonObject();
        for (Direction direction : Direction.values()) {
            String key = faceKey(direction);
            if (key == null || !faceJson.has(key)) continue;
            JsonObject face = faceJson.getAsJsonObject(key);
            float u = face.getAsJsonArray("uv").get(0).getAsFloat();
            float v = face.getAsJsonArray("uv").get(1).getAsFloat();
            JsonArray sizeJson = face.getAsJsonArray("uv_size");
            float w = sizeJson != null ? sizeJson.get(0).getAsFloat() : 0.0F;
            float h = sizeJson != null ? sizeJson.get(1).getAsFloat() : 0.0F;
            if (w == 0.0F || h == 0.0F) continue;
            if (direction == Direction.UP || direction == Direction.DOWN) {
                // Blockbench 导出时 up/down 的 uv 记在右下角、尺寸为负；解析回内部矩形
                putFace(cube, direction, u + w, v + h, u, v, mirror);
            } else {
                putFace(cube, direction, u, v, u + w, v + h, mirror);
            }
        }
        return cube;
    }

    private static void putFace(Cube cube, Direction direction, float u1, float v1, float u2, float v2, boolean mirror) {
        if (mirror) {
            float swap = u1;
            u1 = u2;
            u2 = swap;
        }
        cube.faces.put(direction, new FaceUv(u1, v1, u2, v2));
    }

    private static String faceKey(Direction direction) {
        return switch (direction) {
            case NORTH -> "north";
            case SOUTH -> "south";
            case EAST -> "east";
            case WEST -> "west";
            case UP -> "up";
            case DOWN -> "down";
        };
    }

    static float[] readVec3(JsonObject json, String key, float[] fallback) {
        if (!json.has(key)) return fallback.clone();
        JsonArray array = json.getAsJsonArray(key);
        if (array == null || array.size() < 3) return fallback.clone();
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }
}
