package com.lightmanlp.invtweaks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.lightmanlp.invtweaks.InvTweaks;
import com.lightmanlp.invtweaks.InvTweaksMod;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements Runnable {
    @Inject(
        method = "startGame",
        at = @At(value = "RETURN")
    )
    public void startGameMixin(CallbackInfo ci) {
        InvTweaksMod.invTweaks = new InvTweaks(Minecraft.theMinecraft);
    }
}
