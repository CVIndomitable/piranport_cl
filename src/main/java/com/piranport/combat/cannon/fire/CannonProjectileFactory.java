package com.piranport.combat.cannon.fire;

import com.piranport.entity.CannonProjectileEntity;

/** 炮弹实体工厂，集中保留请求到实体的字段映射。 */
public final class CannonProjectileFactory {
    private CannonProjectileFactory() {}

    public static CannonProjectileEntity create(CannonFireRequest request) {
        return CannonFireService.createProjectile(request);
    }
}
