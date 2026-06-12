package com.example.alieninvasion.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/**
 * The rotating head of the plasma turret, drawn by {@link com.example.alieninvasion.client.PlasmaTurretRenderer}.
 * A yoke that yaws toward the target carrying twin elevating barrels with glowing
 * muzzles and a sensor antenna. UV 64x64.
 */
public class PlasmaTurretModel {
    public static final String YOKE = "yoke";
    public static final String CRADLE = "cradle";

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Yoke: yaws on the base. Pivots at the mount point (y=5 above the block).
        PartDefinition yoke = root.addOrReplaceChild(YOKE, CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 4.0F, 8.0F)   // turret housing
                        .texOffs(0, 13).addBox(-3.0F, -5.0F, -1.0F, 6.0F, 2.0F, 2.0F), // sensor brow
                PartPose.offset(0.0F, 11.0F, 0.0F));
        yoke.addOrReplaceChild("antenna", CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offset(3.0F, -3.0F, 3.0F));

        // Cradle: pitches up/down, carries the twin barrels (point toward -Z).
        PartDefinition cradle = yoke.addOrReplaceChild(CRADLE, CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-3.0F, -2.0F, -2.0F, 6.0F, 4.0F, 4.0F), // gun block
                PartPose.offset(0.0F, -1.0F, 0.0F));
        cradle.addOrReplaceChild("barrel_l", CubeListBuilder.create()
                        .texOffs(24, 9).addBox(-1.0F, -1.0F, -10.0F, 2.0F, 2.0F, 9.0F)
                        .texOffs(34, 9).addBox(-1.5F, -1.5F, -11.0F, 3.0F, 3.0F, 2.0F), // muzzle
                PartPose.offset(-2.0F, 0.0F, 0.0F));
        cradle.addOrReplaceChild("barrel_r", CubeListBuilder.create()
                        .texOffs(24, 9).addBox(-1.0F, -1.0F, -10.0F, 2.0F, 2.0F, 9.0F)
                        .texOffs(34, 9).addBox(-1.5F, -1.5F, -11.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offset(2.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
