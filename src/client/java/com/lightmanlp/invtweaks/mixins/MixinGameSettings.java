package com.lightmanlp.invtweaks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lightmanlp.invtweaks.InvTweaksMod;

import net.minecraft.src.client.GameSettings;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {
    @Inject(
        method = "<init>()V",
        at = @At(value = "RETURN")
    )
    public void initMixin(CallbackInfo ci) {
        GameSettings self = (GameSettings)(Object) this;
        InvTweaksMod.registerCustomKeys(self);
    }

    @Inject(
        method = "loadOptions()V",
        at = @At(value = "HEAD")
    )
    public void loadOptionsMixin(CallbackInfo ci) {
        GameSettings self = (GameSettings)(Object) this;
        InvTweaksMod.registerCustomKeys(self);
    }
}
