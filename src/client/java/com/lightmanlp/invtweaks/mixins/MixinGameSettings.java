package com.lightmanlp.invtweaks.mixins;

import java.io.File;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.lightmanlp.invtweaks.InvTweaksMod;

import net.minecraft.src.client.GameSettings;
import net.minecraft.src.client.KeyBinding;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {
    @Redirect(
        method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/src/client/GameSettings;keyBindings:[Lnet/minecraft/src/client/KeyBinding",
            opcode = Opcodes.PUTFIELD
        )
    )
    public void initRedirect(GameSettings self, KeyBinding[] keys) {
        InvTweaksMod.registerCustomKeys(self, keys);
    }

    @Redirect(
        method = "<init>()V",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/src/client/GameSettings;keyBindings:[Lnet/minecraft/src/client/KeyBinding",
            opcode = Opcodes.PUTFIELD
        )
    )
    public void initEmptyRedirect(GameSettings self, KeyBinding[] keys) {
        InvTweaksMod.registerCustomKeys(self, keys);
    }
}
