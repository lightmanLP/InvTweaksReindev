package com.lightmanlp.invtweaks.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.src.client.gui.GuiButton;
import net.minecraft.src.client.gui.GuiScreen;

@Mixin(GuiScreen.class)
public interface MixinGuiScreen {
    @Accessor("controlList")
    List<GuiButton> getControlList();
}
