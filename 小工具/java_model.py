# -*- coding: utf-8 -*-
"""从 BOXES 列表生成 Java 实体模型类。

模型类是纯机械代码（LayerDefinition + addBox 列表 + 两个空实现），
批量机型手抄容易把 texOffs 抄错，这里和贴图脚本共用同一份 BOXES。
"""
from pathlib import Path


def _f(v):
    return f"{float(v):.1f}F"


def write_model(class_name, layer_name, boxes, doc, package="com.piranport.client.model",
                out_dir="src/main/java/com/piranport/client/model"):
    path = Path(out_dir) / f"{class_name}.java"
    lines = []
    lines.append("package " + package + ";")
    lines.append("")
    lines.append("import com.mojang.blaze3d.vertex.PoseStack;")
    lines.append("import com.mojang.blaze3d.vertex.VertexConsumer;")
    lines.append("import com.piranport.PiranPort;")
    lines.append("import net.minecraft.client.model.EntityModel;")
    lines.append("import net.minecraft.client.model.geom.ModelLayerLocation;")
    lines.append("import net.minecraft.client.model.geom.ModelPart;")
    lines.append("import net.minecraft.client.model.geom.PartPose;")
    lines.append("import net.minecraft.client.model.geom.builders.CubeDeformation;")
    lines.append("import net.minecraft.client.model.geom.builders.CubeListBuilder;")
    lines.append("import net.minecraft.client.model.geom.builders.LayerDefinition;")
    lines.append("import net.minecraft.client.model.geom.builders.MeshDefinition;")
    lines.append("import net.minecraft.client.model.geom.builders.PartDefinition;")
    lines.append("import net.minecraft.resources.ResourceLocation;")
    lines.append("import net.minecraft.world.entity.Entity;")
    lines.append("")
    lines.append("/**")
    for line in doc.strip().splitlines():
        lines.append(" * " + line if line else " *")
    lines.append(" *")
    lines.append(" * <p>BOXES 由 小工具/dev_draw_<机型>_entity.py 与贴图同源生成，"
                 "改模型必须同时重新生成贴图。")
    lines.append(" */")
    lines.append(f"public class {class_name}<T extends Entity> extends EntityModel<T> {{")
    lines.append("")
    lines.append("    public static final ModelLayerLocation LAYER_LOCATION =")
    lines.append(f"            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "
                 f"\"{layer_name}\"), \"main\");")
    lines.append("")
    lines.append("    private final ModelPart bb_main;")
    lines.append("")
    lines.append(f"    public {class_name}(ModelPart root) {{")
    lines.append("        this.bb_main = root.getChild(\"bb_main\");")
    lines.append("    }")
    lines.append("")
    lines.append("    public static LayerDefinition createBodyLayer() {")
    lines.append("        MeshDefinition meshdefinition = new MeshDefinition();")
    lines.append("        PartDefinition partdefinition = meshdefinition.getRoot();")
    lines.append("        partdefinition.addOrReplaceChild(\"bb_main\",")
    lines.append("                CubeListBuilder.create()")
    for i, b in enumerate(boxes):
        x, y, z, w, h, d, u, v = b
        suffix = "," if i == len(boxes) - 1 else ""
        lines.append(f"                        .texOffs({int(u)}, {int(v)})"
                     f".addBox({_f(x)}, {_f(y)}, {_f(z)}, {_f(w)}, {_f(h)}, {_f(d)}, "
                     f"new CubeDeformation(0.0F)){suffix}")
    lines.append("                PartPose.offset(0.0F, 24.0F, 0.0F));")
    lines.append("        return LayerDefinition.create(meshdefinition, 64, 64);")
    lines.append("    }")
    lines.append("")
    lines.append("    @Override")
    lines.append("    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,")
    lines.append("                          float ageInTicks, float netHeadYaw, float headPitch) {")
    lines.append("    }")
    lines.append("")
    lines.append("    @Override")
    lines.append("    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer,")
    lines.append("                               int packedLight, int packedOverlay, int color) {")
    lines.append("        bb_main.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);")
    lines.append("    }")
    lines.append("}")
    path.write_text("\n".join(lines) + "\n")
    print("wrote", path)
