package com.piranport.deepocean;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 离线渲染入口 —— 把 DeepOceanDestroyerModel 渲染成多角度 PNG, 便于快速迭代造型.
 *
 * 运行:
 *   javac -d out/classes $(find src/main/java -name '*.java')
 *   java -cp out/classes com.piranport.deepocean.Main <贴图路径> <输出目录>
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path root = findRepoRoot();
        String texPath = args.length > 0 ? args[0]
                : root.resolve("src/main/resources/assets/piranport/textures/entity/deep_ocean/destroyer_model.png").toString();
        String outDir = args.length > 1 ? args[1] : root.resolve("build/offline-renders").toString();

        var texture = ImageIO.read(new File(texPath));
        if (texture == null) throw new IllegalStateException("贴图读取失败: " + texPath);

        DeepOceanDestroyerModel model = new DeepOceanDestroyerModel();
        File dir = new File(outDir);
        dir.mkdirs();

        // 四个角度 + 一个特写
        render(model, texture, dir, "destroyer_front.png", 0f, 0f, 1.0f, 0.95f);
        render(model, texture, dir, "destroyer_3q.png", -26f, 8f, 1.0f, 0.72f);
        render(model, texture, dir, "destroyer_side.png", -90f, 6f, 1.0f, 0.85f);
        render(model, texture, dir, "destroyer_back.png", 180f, 8f, 1.0f, 0.90f);
        render(model, texture, dir, "destroyer_3q2.png", 28f, 8f, 1.0f, 0.72f);
        render(model, texture, dir, "destroyer_head.png", -20f, 14f, 3.4f, 0.66f);

        System.out.println("渲染完成 -> " + dir.getAbsolutePath());
    }

    private static void render(DeepOceanDestroyerModel model, java.awt.image.BufferedImage texture,
                               File dir, String name, float yawDeg, float pitchDeg, float zoom,
                               float frame) throws Exception {
        DeepOceanRenderer r = new DeepOceanRenderer(900, 900, texture);
        r.camRotY = (float) Math.toRadians(yawDeg);
        r.camRotX = (float) Math.toRadians(pitchDeg);
        r.zoom = zoom;
        r.render(model.root);
        ImageIO.write(r.getImage(), "PNG", new File(dir, name));
    }

    private static Path findRepoRoot() {
        Path p = Paths.get("").toAbsolutePath();
        while (p != null && !p.resolve("src/main/java/com/piranport").toFile().exists()) {
            p = p.getParent();
        }
        return p != null ? p : Paths.get("").toAbsolutePath();
    }
}
