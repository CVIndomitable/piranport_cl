package com.piranport.unicorn;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 独角兽离线预览使用的像素材质。
 *
 * <p>材质刻意保持清晰的像素边缘，同时给不同朝向提供足够的明暗分区，
 * 用来检查模型的体块、薄片和前后遮挡关系。</p>
 */
public class PixelTextureGenerator {
    public static final int PIXELS_PER_UNIT = 16;

    private static final int HAIR_BASE = 0xD8EAF8;
    private static final int HAIR_SHADE = 0x94B9DC;
    private static final int HAIR_DEEP = 0x6385B8;
    private static final int HAIR_LIGHT = 0xF8FCFF;
    private static final int SKIN_BASE = 0xF9D8C9;
    private static final int SKIN_SHADE = 0xE6B6B0;
    private static final int SKIN_LIGHT = 0xFFEAE0;
    private static final int EYE_DARK_COLOR = 0x263455;
    private static final int EYE_BLUE_COLOR = 0x399BE5;
    private static final int EYE_LIGHT = 0xA9E8FF;
    private static final int BLUSH_COLOR = 0xF08EA9;
    private static final int MOUTH_COLOR = 0xD66C82;
    private static final int DRESS_WHITE = 0xF9FBFF;
    private static final int DRESS_SHADE = 0xC5D9EA;
    private static final int RIBBON_BLUE = 0x6EAFE2;
    private static final int RIBBON_DARK = 0x47749F;
    private static final int STOCKING_WHITE = 0xF4F8FB;
    private static final int SHOE_WHITE = 0xF7FBFF;
    private static final int SHOE_SOLE = 0x607C99;
    private static final int METAL_LIGHT = 0xE9F1F5;
    private static final int METAL_BASE = 0x9AAAB8;
    private static final int METAL_DARK = 0x586A7A;
    private static final int GOLD_LIGHT = 0xFFE18A;
    private static final int GOLD_BASE = 0xDFAF35;
    private static final int GOLD_DARK = 0x9D6D1F;
    private static final int HARP_RED_BASE = 0xB92F43;
    private static final int HARP_RED_DARK = 0x722638;
    private static final int HARP_BLUE_BASE = 0x354E82;
    private static final int HARP_BLUE_LIGHT = 0x718DBA;
    private static final int BIRD_BASE = 0x4FBDAE;
    private static final int BIRD_DARK = 0x237D83;
    private static final int OUTLINE = 0x394A62;

    public enum Material {
        HAIR,
        HAIR_FRONT,
        HAIR_HIGHLIGHT,
        SKIN,
        FACE,
        EYE_DARK,
        EYE_BLUE,
        EYE_WHITE,
        BLUSH,
        MOUTH,
        FLOWER,
        FLOWER_PINK,
        FLOWER_YELLOW,
        FLOWER_MINT,
        FLOWER_CORAL,
        DRESS,
        RIBBON,
        STOCKING,
        SHOE,
        METAL,
        GOLD,
        HARP_RED,
        HARP_BLUE,
        BIRD,
        GENERAL
    }

    public static Map<Material, BufferedImage[]> createAll() {
        Map<Material, BufferedImage[]> textures = new HashMap<>();
        textures.put(Material.HAIR, sixFaces(hairTexture(HAIR_BASE, HAIR_SHADE, HAIR_LIGHT)));
        textures.put(Material.HAIR_FRONT, sixFaces(hairTexture(HAIR_LIGHT, HAIR_BASE, 0xFFFFFFFE)));
        textures.put(Material.HAIR_HIGHLIGHT, sixFaces(hairTexture(HAIR_LIGHT, HAIR_SHADE, 0xFFFFFFFF)));
        textures.put(Material.SKIN, sixFaces(skinTexture(false)));
        textures.put(Material.FACE, sixFaces(skinTexture(true)));
        textures.put(Material.EYE_DARK, sixFaces(solidBox(EYE_DARK_COLOR, 0x18233C, 0x4C638A, true)));
        textures.put(Material.EYE_BLUE, sixFaces(eyeTexture()));
        textures.put(Material.EYE_WHITE, sixFaces(solidBox(0xFFFFFF, 0xD9F3FF, 0xFFFFFF, false)));
        textures.put(Material.BLUSH, sixFaces(solidBox(BLUSH_COLOR, 0xC96886, 0xFFB1C3, false)));
        textures.put(Material.MOUTH, sixFaces(solidBox(MOUTH_COLOR, 0x98465E, 0xF5A0AE, false)));
        textures.put(Material.FLOWER, sixFaces(flowerTexture(0xF2F5FA, 0xB7CBE3, GOLD_BASE)));
        textures.put(Material.FLOWER_PINK, sixFaces(flowerTexture(0xF18AAE, 0xC95B87, GOLD_BASE)));
        textures.put(Material.FLOWER_YELLOW, sixFaces(flowerTexture(0xFFE06A, 0xD5A62C, 0xFFF1A7)));
        textures.put(Material.FLOWER_MINT, sixFaces(flowerTexture(0x73D4BF, 0x3F9E99, 0xE6FFCE)));
        textures.put(Material.FLOWER_CORAL, sixFaces(flowerTexture(0xFF9A86, 0xC86768, 0xFFD48E)));
        textures.put(Material.DRESS, sixFaces(dressTexture()));
        textures.put(Material.RIBBON, sixFaces(ribbonTexture()));
        textures.put(Material.STOCKING, sixFaces(stockingTexture()));
        textures.put(Material.SHOE, sixFaces(shoeTexture()));
        textures.put(Material.METAL, sixFaces(metalTexture()));
        textures.put(Material.GOLD, sixFaces(goldTexture()));
        textures.put(Material.HARP_RED, sixFaces(harpRedTexture()));
        textures.put(Material.HARP_BLUE, sixFaces(harpBlueTexture()));
        textures.put(Material.BIRD, sixFaces(birdTexture()));
        textures.put(Material.GENERAL, sixFaces(metalTexture()));
        return textures;
    }

    public static BufferedImage solidBox(int base, int shade, int highlight, boolean outlined) {
        BufferedImage image = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int color = base;
                if (outlined && (x == 0 || y == 0 || x == 15 || y == 15)) {
                    color = OUTLINE;
                } else if (y <= 1) {
                    color = highlight;
                } else if (y >= 14) {
                    color = shade;
                } else if (x <= 1) {
                    color = blend(base, shade, 0.26F);
                } else if (x >= 14) {
                    color = blend(base, highlight, 0.22F);
                }
                image.setRGB(x, y, opaque(color));
            }
        }
        return image;
    }

    private static BufferedImage hairTexture(int base, int shade, int highlight) {
        BufferedImage image = new BufferedImage(PIXELS_PER_UNIT, PIXELS_PER_UNIT,
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < PIXELS_PER_UNIT; y++) {
            for (int x = 0; x < PIXELS_PER_UNIT; x++) {
                int color;
                if (x == 0 || y == 0 || x == 15 || y == 15) {
                    color = OUTLINE;
                } else if ((x + y * 2) % 9 <= 1) {
                    color = highlight;
                } else if ((x * 2 + y) % 11 >= 9) {
                    color = shade;
                } else {
                    color = base;
                }
                image.setRGB(x, y, opaque(color));
            }
        }
        return image;
    }

    private static BufferedImage skinTexture(boolean face) {
        BufferedImage image = solidBox(SKIN_BASE, SKIN_SHADE, SKIN_LIGHT, false);
        if (face) {
            for (int y = 3; y < 13; y++) {
                for (int x = 2; x < 14; x++) {
                    if (y == 3 || y == 12 || x == 2 || x == 13) {
                        image.setRGB(x, y, opaque(blend(SKIN_BASE, SKIN_SHADE, 0.18F)));
                    }
                }
            }
        }
        return image;
    }

    private static BufferedImage eyeTexture() {
        BufferedImage image = solidBox(EYE_BLUE_COLOR, 0x20659E, EYE_LIGHT, true);
        for (int y = 4; y < 10; y++) {
            for (int x = 3; x < 13; x++) {
                if ((x + y) % 4 == 0) {
                    image.setRGB(x, y, opaque(EYE_LIGHT));
                }
            }
        }
        return image;
    }

    private static BufferedImage flowerTexture(int base, int shade, int center) {
        BufferedImage image = solidBox(base, shade, 0xFFFFFF, true);
        for (int y = 5; y < 11; y++) {
            for (int x = 5; x < 11; x++) {
                if (Math.abs(x - 8) <= 1 && Math.abs(y - 8) <= 1) {
                    image.setRGB(x, y, opaque(center));
                }
            }
        }
        return image;
    }

    private static BufferedImage dressTexture() {
        BufferedImage image = solidBox(DRESS_WHITE, DRESS_SHADE, 0xFFFFFF, true);
        for (int y = 3; y < 14; y++) {
            if (y % 4 == 1) {
                for (int x = 3; x < 14; x++) {
                    image.setRGB(x, y, opaque(blend(DRESS_WHITE, DRESS_SHADE, 0.28F)));
                }
            }
        }
        return image;
    }

    private static BufferedImage ribbonTexture() {
        BufferedImage image = solidBox(RIBBON_BLUE, RIBBON_DARK, 0xB9E9FF, true);
        for (int y = 2; y < 14; y++) {
            if (y % 3 == 0) {
                for (int x = 3; x < 14; x++) {
                    image.setRGB(x, y, opaque(blend(RIBBON_BLUE, 0xD8F6FF, 0.28F)));
                }
            }
        }
        return image;
    }

    private static BufferedImage stockingTexture() {
        return solidBox(STOCKING_WHITE, DRESS_SHADE, 0xFFFFFF, true);
    }

    private static BufferedImage shoeTexture() {
        BufferedImage image = solidBox(SHOE_WHITE, SHOE_SOLE, 0xFFFFFF, true);
        for (int x = 3; x < 13; x++) {
            image.setRGB(x, 5, opaque(RIBBON_BLUE));
        }
        return image;
    }

    private static BufferedImage metalTexture() {
        return solidBox(METAL_BASE, METAL_DARK, METAL_LIGHT, true);
    }

    private static BufferedImage goldTexture() {
        return solidBox(GOLD_BASE, GOLD_DARK, GOLD_LIGHT, true);
    }

    private static BufferedImage harpRedTexture() {
        return solidBox(HARP_RED_BASE, HARP_RED_DARK, 0xF05E66, true);
    }

    private static BufferedImage harpBlueTexture() {
        return solidBox(HARP_BLUE_BASE, 0x202D55, HARP_BLUE_LIGHT, true);
    }

    private static BufferedImage birdTexture() {
        return solidBox(BIRD_BASE, BIRD_DARK, 0xB9FFF0, true);
    }

    private static int opaque(int rgb) {
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static int blend(int first, int second, float amount) {
        int fr = (first >> 16) & 0xFF;
        int fg = (first >> 8) & 0xFF;
        int fb = first & 0xFF;
        int sr = (second >> 16) & 0xFF;
        int sg = (second >> 8) & 0xFF;
        int sb = second & 0xFF;
        int r = Math.round(fr + (sr - fr) * amount);
        int g = Math.round(fg + (sg - fg) * amount);
        int b = Math.round(fb + (sb - fb) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static BufferedImage[] sixFaces(BufferedImage image) {
        return new BufferedImage[] {image, image, image, image, image, image};
    }
}
