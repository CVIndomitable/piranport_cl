package com.piranport.unicorn;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 像素纹理生成器 —— 格子画风格 (16x16 per 1/16 格).
 *
 * 风格: 模仿原版 Minecraft 玩家模型的"低像素"质感,
 * 每个 1x1 单位 = 16x16 像素, 整体方块感强.
 *
 * 部件 (material) → 调色板:
 *   HAIR_BASE       银白蓝    (头发基础)
 *   HAIR_SHADE      浅灰蓝    (头发阴影)
 *   HAIR_HIGHLIGHT  银白高光  (头发亮面)
 *   SKIN_BASE       肤色      (脸/颈)
 *   SKIN_SHADE      肤色阴影
 *   HORN_GOLD       金黄      (独角)
 *   HORN_DARK       深金
 *   FLOWER_YELLOW   花黄
 *   FLOWER_GREEN    花绿
 *   FLOWER_PINK     花粉
 *   FLOWER_WHITE    花白
 *   DRESS_WHITE     裙白
 *   DRESS_SHADE     裙灰
 *   RIBBON_BLUE     蓝条纹
 *   RIBBON_DARK     蓝条纹阴影
 *   STOCKING_WHITE  丝袜白
 *   SHOE_WHITE      鞋白
 *   SHOE_DARK       鞋底
 *   EYE_RED         红瞳
 *   OUTLINE         描边黑
 */
public class PixelTextureGenerator {

    // 调色板
    public static final int HAIR_BASE     = 0xD8E4F0;
    public static final int HAIR_SHADE    = 0xA8B8CC;
    public static final int HAIR_HIGHLIGHT = 0xF0F4FF;
    public static final int SKIN_BASE     = 0xFBE0CB;
    public static final int SKIN_SHADE    = 0xE0BFA5;
    public static final int HORN_GOLD     = 0xFFD24A;
    public static final int HORN_DARK     = 0xC18A20;
    public static final int FLOWER_YELLOW = 0xFFE060;
    public static final int FLOWER_GREEN  = 0x80C060;
    public static final int FLOWER_PINK   = 0xF898B8;
    public static final int FLOWER_WHITE  = 0xFFFFFF;
    public static final int DRESS_WHITE   = 0xF8F8FF;
    public static final int DRESS_SHADE   = 0xC8C8D8;
    public static final int RIBBON_BLUE   = 0x6699CC;
    public static final int RIBBON_DARK   = 0x406A99;
    public static final int STOCKING_WHITE = 0xFAFAFA;
    public static final int SHOE_WHITE    = 0xFFFAF0;
    public static final int SHOE_DARK     = 0x4A4030;
    public static final int EYE_RED       = 0xD83030;
    public static final int OUTLINE       = 0x1A1A2A;

    /** 分辨率: 1 MC 单位 = 16 像素 */
    public static final int PIXELS_PER_UNIT = 16;

    /**
     * 生成一个 16x16 像素的纯色 box 纹理.
     * @param base 主色
     * @param shade 阴影色 (用于底部和左侧)
     * @param highlight 高光色 (用于顶部和右侧)
     * @param outlined 是否画 1px 黑色描边
     */
    public static BufferedImage solidBox(int base, int shade, int highlight, boolean outlined) {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c = base;
                if (outlined && (x == 0 || y == 0 || x == 15 || y == 15)) {
                    c = OUTLINE;
                } else if (y < 1) {
                    c = highlight;
                } else if (y > 14) {
                    c = shade;
                } else if (x < 2) {
                    c = blend(base, shade, 0.4f);
                } else if (x > 13) {
                    c = blend(base, highlight, 0.3f);
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 头发专用: 流苏感, 高光朝右上 */
    public static BufferedImage hairFlame(boolean rightSide) {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y < 3) {
                    c = HAIR_HIGHLIGHT;
                } else if (y > 13) {
                    c = HAIR_SHADE;
                } else {
                    // 流动条纹
                    int stripe = (x + y * 2) % 6;
                    if (stripe < 2) c = HAIR_HIGHLIGHT;
                    else if (stripe > 4) c = HAIR_SHADE;
                    else c = HAIR_BASE;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 花环: 随机色花 */
    public static BufferedImage flowerTexture() {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        int[] flowers = {FLOWER_YELLOW, FLOWER_PINK, FLOWER_WHITE, FLOWER_GREEN};
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else {
                    // 4x4 块状分布
                    int fb = ((x / 4) + (y / 4) * 4) % 4;
                    c = flowers[fb];
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 脸: 肤色 + 眼睛 */
    public static BufferedImage faceTexture(boolean leftSide) {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y < 2) {
                    c = HAIR_SHADE; // 顶部头发
                } else if (y > 14) {
                    c = SKIN_SHADE;
                } else {
                    c = SKIN_BASE;
                }
                // 眼睛 (按侧面只画一侧)
                // 眼睛位置约 y=10, x=5~7 (左) 或 x=8~10 (右)
                if (y >= 9 && y <= 11) {
                    if (leftSide && x >= 4 && x <= 6) {
                        c = EYE_RED;
                    } else if (!leftSide && x >= 9 && x <= 11) {
                        c = EYE_RED;
                    }
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 裙摆: 白色 + 蓝色点 (立绘里裙摆有花) */
    public static BufferedImage dressTexture() {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y < 2) {
                    c = DRESS_SHADE;
                } else if (y > 14) {
                    c = DRESS_SHADE;
                } else {
                    c = DRESS_WHITE;
                    // 偶发小花点点
                    if ((x % 5 == 2) && (y % 4 == 1)) c = FLOWER_PINK;
                    if ((x % 7 == 3) && (y % 5 == 2)) c = FLOWER_YELLOW;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 蓝色条纹 (蓝白格, 立体感) */
    public static BufferedImage ribbonTexture() {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y < 2) {
                    c = RIBBON_DARK;
                } else if (y > 14) {
                    c = RIBBON_DARK;
                } else {
                    c = RIBBON_BLUE;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 丝袜: 纯白 + 1px 阴影 */
    public static BufferedImage stockingTexture() {
        return solidBox(STOCKING_WHITE, DRESS_SHADE, HAIR_HIGHLIGHT, true);
    }

    /** 鞋: 白帮 + 棕底 */
    public static BufferedImage shoeTexture() {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y > 12) {
                    c = SHOE_DARK; // 鞋底
                } else if (y < 2) {
                    c = SHOE_DARK;
                } else {
                    c = SHOE_WHITE;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 角: 金色, 顶端高光 */
    public static BufferedImage hornTexture() {
        BufferedImage img = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                                              BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int c;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    c = OUTLINE;
                } else if (y < 3) {
                    c = HORN_DARK;
                } else if (y < 7) {
                    c = HORN_GOLD;
                } else {
                    c = HORN_DARK;
                }
                img.setRGB(x, y, 0xFF000000 | c);
            }
        }
        return img;
    }

    /** 肤色 */
    public static BufferedImage skinTexture() {
        return solidBox(SKIN_BASE, SKIN_SHADE, HAIR_HIGHLIGHT, true);
    }

    // ===== 工具: 颜色混合 =====
    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int rr = (int) (ar * (1 - t) + br * t);
        int gg = (int) (ag * (1 - t) + bg * t);
        int bl = (int) (ab * (1 - t) + bb * t);
        return (rr << 16) | (gg << 8) | bl;
    }

    // ====== material map ======
    public enum Material {
        HAIR, HAIR_FRONT, FL,
        SKIN, FACE_L, FACE_R,
        HORN, FLOWER,
        DRESS, RIBBON,
        STOCKING, SHOE,
        GENERAL
    }

    public static Map<Material, BufferedImage[]> createAll() {
        Map<Material, BufferedImage[]> map = new HashMap<>();
        map.put(Material.HAIR, sixFaces(hairFlame(false)));
        map.put(Material.HAIR_FRONT, sixFaces(hairFlame(true)));
        map.put(Material.FL, sixFaces(hairFlame(false)));
        map.put(Material.SKIN, sixFaces(skinTexture()));
        map.put(Material.FACE_L, sixFaces(faceTexture(true)));
        map.put(Material.FACE_R, sixFaces(faceTexture(false)));
        map.put(Material.HORN, sixFaces(hornTexture()));
        map.put(Material.FLOWER, sixFaces(flowerTexture()));
        map.put(Material.DRESS, sixFaces(dressTexture()));
        map.put(Material.RIBBON, sixFaces(ribbonTexture()));
        map.put(Material.STOCKING, sixFaces(stockingTexture()));
        map.put(Material.SHOE, sixFaces(shoeTexture()));
        map.put(Material.GENERAL, sixFaces(solidBox(0xCCCCDD, 0x888899, 0xEEEEFF, true)));
        return map;
    }

    /** 将 1 张 16x16 图扩展成 6 面: [+X, -X, +Y, -Y, +Z, -Z] */
    private static BufferedImage[] sixFaces(BufferedImage img) {
        return new BufferedImage[]{img, img, img, img, img, img};
    }
}