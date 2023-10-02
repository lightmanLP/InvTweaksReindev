package com.lightmanlp.invtweaks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lightmanlp.invtweaks.InvTweaksMod;

import net.minecraft.client.Minecraft;
import net.minecraft.src.client.renderer.EntityRenderer;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Inject(method = "updateCameraAndRender", at = @At(value = "RETURN"))
    public void updateCameraAndRenderMixin(float deltaTicks, CallbackInfo ci) {
        InvTweaksMod.INSTANCE.onTickInstantiated(Minecraft.getInstance());
    }
}
