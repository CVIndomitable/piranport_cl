package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, PiranPort.MOD_ID);

    /** 足球巨星套装 — 低防御值（装饰性），使用透明模型贴图（穿上后不显示盔甲模型） */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FOOTBALL =
            ARMOR_MATERIALS.register("football", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 1);
                        map.put(ArmorItem.Type.LEGGINGS, 2);
                        map.put(ArmorItem.Type.CHESTPLATE, 3);
                        map.put(ArmorItem.Type.HELMET, 1);
                    }),
                    12,
                    SoundEvents.ARMOR_EQUIP_CHAIN,
                    () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "football"))),
                    0.0F,
                    0.0F
            ));

    /** 小型舰装核心材质 — 护甲值8，韧性4 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SMALL_SHIP_CORE =
            ARMOR_MATERIALS.register("small_ship_core", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 8);  // ShipType.SMALL.baseArmor
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "small_ship_core"))),
                    4.0F,  // ShipType.SMALL.armorToughness
                    0.0F
            ));

    /** 中型舰装核心材质 — 护甲值12，韧性8 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MEDIUM_SHIP_CORE =
            ARMOR_MATERIALS.register("medium_ship_core", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 12);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "medium_ship_core"))),
                    8.0F,
                    0.0F
            ));

    /** 大型舰装核心材质 — 护甲值16，韧性12 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LARGE_SHIP_CORE =
            ARMOR_MATERIALS.register("large_ship_core", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 16);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "large_ship_core"))),
                    12.0F,
                    0.0F
            ));

    /** 潜艇核心材质 — 护甲值4，韧性0 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SUBMARINE_CORE =
            ARMOR_MATERIALS.register("submarine_core", () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.HELMET, 4);
                    }),
                    15,
                    SoundEvents.ARMOR_EQUIP_IRON,
                    () -> Ingredient.of(ModItems.ALUMINUM_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(
                            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "submarine_core"))),
                    0.0F,
                    0.0F
            ));
}
