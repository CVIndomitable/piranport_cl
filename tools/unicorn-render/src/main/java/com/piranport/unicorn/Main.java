package com.piranport.unicorn;

import javax.imageio.ImageIO;
import java.io.File;

/** main: 渲染 UnicornModel 到 output/unicorn.png */
public class Main {
    public static void main(String[] args) throws Exception {
        UnicornModel model = new UnicornModel();
        UnicornRenderer renderer = new UnicornRenderer(600, 600);
        renderer.render(model);
        File out = new File("output/unicorn.png");
        out.getParentFile().mkdirs();
        ImageIO.write(renderer.getImage(), "PNG", out);
        System.out.println("Wrote " + out.getAbsolutePath());
    }
}