package com.piranport.unicorn;

import javax.imageio.ImageIO;
import java.io.File;

/** main: 渲染 UnicornModel 到指定 PNG 文件。 */
public class Main {
    public static void main(String[] args) throws Exception {
        UnicornModel model = new UnicornModel();
        UnicornRenderer renderer = new UnicornRenderer(1000, 1000);
        renderer.render(model);
        File out = new File(args.length > 0 ? args[0] : "output/unicorn_redesign.png");
        out.getParentFile().mkdirs();
        ImageIO.write(renderer.getImage(), "PNG", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}
