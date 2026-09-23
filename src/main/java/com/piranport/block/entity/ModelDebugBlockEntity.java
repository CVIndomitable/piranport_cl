package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Marker BlockEntity that stores which entity model to render (b25 / f4f / ...).
 *
 * <p>{@code modelType} 决定"用哪套几何"，{@code variant} 决定"用哪张贴图 / 哪个载体物品"。
 * 两者分开是因为炮弹与导弹共用同一份 Java 模型（{@code TorpedoModel}），
 * 小/中/大口径的差异只在贴图 UV 与渲染缩放上，仅凭 modelType 无法区分。
 */
public class ModelDebugBlockEntity extends BlockEntity {

    private String modelType = "b25";
    /** 弹体细分类：口径档位 / 弹种，渲染时用来选贴图与缩放。非弹体模型忽略此字段。 */
    private String variant = "";

    public ModelDebugBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.MODEL_DEBUG.get(), pos, state);
    }

    public String getModelType() {
        return modelType;
    }

    public String getVariant() {
        return variant;
    }

    public void setModelType(String type, String variant) {
        this.modelType = type;
        this.variant = variant == null ? "" : variant;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("model", modelType);
        tag.putString("variant", variant);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("model")) {
            modelType = tag.getString("model");
        }
        if (tag.contains("variant")) {
            variant = tag.getString("variant");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(3.0);
    }
}
