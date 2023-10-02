package com.lightmanlp.invtweaks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.fox2code.foxloader.network.NetworkPlayer;
import com.lightmanlp.invtweaks.InvTweaksMod;

import net.minecraft.src.client.player.EntityPlayerSP;
import net.minecraft.src.game.entity.Entity;
import net.minecraft.src.game.entity.player.EntityPlayer;
import net.minecraft.src.game.level.World;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends EntityPlayer implements NetworkPlayer {
    public MixinEntityPlayerSP(World world) {
        super(world);
    }

    @Inject(method = "onItemPickup", at = @At(value = "HEAD"))
    public void onItemPickupMixin(Entity entity, int arg2, CallbackInfo ci) {
        InvTweaksMod.invTweaks.onItemPickup();
    }
}
