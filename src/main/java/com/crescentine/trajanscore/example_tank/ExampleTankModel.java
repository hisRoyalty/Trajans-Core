package com.crescentine.trajanscore.example_tank;

import com.crescentine.trajanscore.TrajansCoreMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.geo.render.built.GeoBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class ExampleTankModel extends AnimatedGeoModel<ExampleTankEntity> {
    public ResourceLocation getModelResource(ExampleTankEntity object) {
        return new ResourceLocation(TrajansCoreMod.MOD_ID, "geo/tank.geo.json");
    }

    public ResourceLocation getTextureResource(ExampleTankEntity object) {
        return new ResourceLocation(TrajansCoreMod.MOD_ID, "textures/item/tank.png");
    }

    public ResourceLocation getAnimationResource(ExampleTankEntity animatable) {
        return new ResourceLocation(TrajansCoreMod.MOD_ID, "animations/tank.json");
    }
    @Override
    public void setCustomAnimations(ExampleTankEntity animatable, int instanceId, AnimationEvent animationEvent) {
        super.setCustomAnimations(animatable, instanceId, animationEvent);
        IBone turret = this.getAnimationProcessor().getBone("TopPart");
        Entity rider = animatable.getControllingPassenger();
        if (animatable.isVehicle() && rider instanceof Player) {
            turret.setRotationY((float) -Math.toRadians(rider.getYHeadRot() - animatable.getYRot()));
        }
    }

}